package com.aipo.backend.domain.external.kind.service;

import com.aipo.backend.domain.external.common.dto.ExternalIpoSourceDataCommand;
import com.aipo.backend.domain.external.common.service.IpoExternalSourceDataService;
import com.aipo.backend.domain.external.kind.client.KindClient;
import com.aipo.backend.domain.external.kind.dto.KindIpoSupplementResult;
import com.aipo.backend.domain.external.opendart.entity.ExternalApiResponse;
import com.aipo.backend.domain.external.opendart.repository.ExternalApiResponseRepository;
import com.aipo.backend.domain.ipo.entity.IpoSchedule;
import com.aipo.backend.domain.ipo.entity.IpoStock;
import com.aipo.backend.domain.ipo.entity.ScheduleType;
import com.aipo.backend.domain.ipo.repository.IpoScheduleRepository;
import com.aipo.backend.domain.ipo.repository.IpoStockRepository;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class KindIpoSupplementService {

    private static final String PROVIDER = "KIND";
    private static final String API_PUBLIC_OFFERING_SCHEDULE = "public_offering_schedule";
    private static final String API_PUBLIC_OFFERING_COMPANIES = "public_offering_companies";
    private static final Pattern FULL_DATE = Pattern.compile("(20\\d{2})[.\\-/년]\\s*(\\d{1,2})[.\\-/월]\\s*(\\d{1,2})");
    private static final Pattern STOCK_CODE = Pattern.compile("(?<!\\d)(\\d{6})(?!\\d)");
    private static final DateTimeFormatter KIND_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final KindClient kindClient;
    private final ExternalApiResponseRepository externalApiResponseRepository;
    private final IpoStockRepository ipoStockRepository;
    private final IpoScheduleRepository ipoScheduleRepository;
    private final IpoExternalSourceDataService ipoExternalSourceDataService;

    @Transactional
    public KindIpoSupplementResult supplement(LocalDate startDate, LocalDate endDate) {
        KindCounter counter = new KindCounter();
        List<String> bodies = new ArrayList<>();

        CachedBody companyBody = getCachedOrFetch(
                API_PUBLIC_OFFERING_COMPANIES,
                "fromDate=%s&toDate=%s".formatted(startDate.format(KIND_DATE), endDate.format(KIND_DATE)),
                () -> kindClient.fetchPublicOfferingCompanies(startDate.format(KIND_DATE), endDate.format(KIND_DATE))
        );
        addCounter(companyBody, counter);
        bodies.add(companyBody.body());

        for (YearMonth month = YearMonth.from(startDate); !month.isAfter(YearMonth.from(endDate)); month = month.plusMonths(1)) {
            YearMonth targetMonth = month;
            CachedBody scheduleBody = getCachedOrFetch(
                    API_PUBLIC_OFFERING_SCHEDULE,
                    "year=%d&month=%02d".formatted(targetMonth.getYear(), targetMonth.getMonthValue()),
                    () -> kindClient.fetchPublicOfferingSchedule(targetMonth.getYear(), targetMonth.getMonthValue())
            );
            addCounter(scheduleBody, counter);
            bodies.add(scheduleBody.body());
        }

        List<IpoStock> stocks = ipoStockRepository.findAll();
        for (IpoStock stock : stocks) {
            SupplementData data = findSupplementData(stock, bodies);
            if (!data.hasAnyValue()) {
                continue;
            }

            counter.matchedStockCount++;
            String externalKey = stock.getDartCorpCode() != null && !stock.getDartCorpCode().isBlank()
                    ? stock.getDartCorpCode()
                    : stock.getStockName();
            ipoExternalSourceDataService.upsert(new ExternalIpoSourceDataCommand(
                    PROVIDER,
                    "kind_supplement",
                    externalKey,
                    stock.getCompanyName(),
                    stock.getDartCorpCode(),
                    data.stockCode(),
                    data.marketType(),
                    null,
                    null,
                    data.demandForecastStartDate(),
                    data.demandForecastEndDate(),
                    null,
                    data.listingDate(),
                    null,
                    null,
                    null,
                    90
            ));
            stock.supplementFromKind(data.stockCode(), data.marketType(), data.listingDate());
            counter.supplementedStockCount++;

            counter.supplementedScheduleCount += upsertSchedule(stock, ScheduleType.DEMAND_FORECAST_START, data.demandForecastStartDate(), "KIND 수요예측 시작일");
            counter.supplementedScheduleCount += upsertSchedule(stock, ScheduleType.DEMAND_FORECAST_END, data.demandForecastEndDate(), "KIND 수요예측 종료일");
            counter.supplementedScheduleCount += upsertSchedule(stock, ScheduleType.LISTING, data.listingDate(), "KIND 상장일");
            if (stock.getDartCorpCode() != null && !stock.getDartCorpCode().isBlank()) {
                ipoExternalSourceDataService.mergeByDartCorpCode(stock.getDartCorpCode());
            }
        }

        return new KindIpoSupplementResult(
                counter.cachedResponseCount,
                counter.fetchedResponseCount,
                counter.matchedStockCount,
                counter.supplementedStockCount,
                counter.supplementedScheduleCount
        );
    }

    private SupplementData findSupplementData(IpoStock stock, List<String> bodies) {
        SupplementAccumulator accumulator = new SupplementAccumulator();
        String normalizedName = normalize(stock.getStockName());

        for (String body : bodies) {
            if (isBlockedOrEmpty(body)) {
                continue;
            }

            Document document = Jsoup.parse(body);
            for (Element row : document.select("tr")) {
                String rowText = row.text();
                if (!normalize(rowText).contains(normalizedName)) {
                    continue;
                }
                accumulator.merge(parseRow(rowText));
            }

            String plainText = document.text();
            if (normalize(plainText).contains(normalizedName)) {
                accumulator.merge(parseNearbyText(plainText, stock.getStockName()));
            }
        }

        return accumulator.toData();
    }

    private SupplementData parseRow(String text) {
        String marketType = parseMarketType(text);
        String stockCode = parseStockCode(text);
        LocalDate listingDate = parseLabelDate(text, "상장");
        DateRange demandForecastPeriod = parseLabelDateRange(text, "수요예측");

        return new SupplementData(
                stockCode,
                marketType,
                listingDate,
                demandForecastPeriod == null ? null : demandForecastPeriod.startDate(),
                demandForecastPeriod == null ? null : demandForecastPeriod.endDate()
        );
    }

    private SupplementData parseNearbyText(String text, String stockName) {
        int index = normalize(text).indexOf(normalize(stockName));
        if (index < 0) {
            return SupplementData.empty();
        }

        int start = Math.max(0, index - 300);
        int end = Math.min(text.length(), index + 600);
        return parseRow(text.substring(start, end));
    }

    private String parseMarketType(String text) {
        if (text.contains("코스닥")) {
            return "KOSDAQ";
        }
        if (text.contains("유가증권") || text.contains("코스피")) {
            return "KOSPI";
        }
        if (text.contains("코넥스")) {
            return "KONEX";
        }
        return null;
    }

    private String parseStockCode(String text) {
        Matcher matcher = STOCK_CODE.matcher(text);
        return matcher.find() ? matcher.group(1) : null;
    }

    private LocalDate parseLabelDate(String text, String label) {
        int labelIndex = text.indexOf(label);
        if (labelIndex < 0) {
            return null;
        }

        String nearby = text.substring(Math.max(0, labelIndex - 80), Math.min(text.length(), labelIndex + 120));
        List<LocalDate> dates = extractDates(nearby);
        return dates.isEmpty() ? null : dates.get(0);
    }

    private DateRange parseLabelDateRange(String text, String label) {
        int labelIndex = text.indexOf(label);
        if (labelIndex < 0) {
            return null;
        }

        String nearby = text.substring(Math.max(0, labelIndex - 80), Math.min(text.length(), labelIndex + 160));
        List<LocalDate> dates = extractDates(nearby);
        if (dates.isEmpty()) {
            return null;
        }
        return new DateRange(dates.get(0), dates.size() > 1 ? dates.get(1) : dates.get(0));
    }

    private List<LocalDate> extractDates(String text) {
        Set<LocalDate> dates = new LinkedHashSet<>();
        Matcher matcher = FULL_DATE.matcher(text);
        while (matcher.find()) {
            dates.add(LocalDate.of(
                    Integer.parseInt(matcher.group(1)),
                    Integer.parseInt(matcher.group(2)),
                    Integer.parseInt(matcher.group(3))
            ));
        }
        return new ArrayList<>(dates);
    }

    private int upsertSchedule(IpoStock stock, ScheduleType scheduleType, LocalDate date, String note) {
        if (date == null) {
            return 0;
        }

        Optional<IpoSchedule> existing = ipoScheduleRepository.findByStock_IdAndScheduleType(stock.getId(), scheduleType);
        if (existing.isPresent()) {
            existing.get().updateDate(date, note);
        } else {
            ipoScheduleRepository.save(IpoSchedule.create(stock, scheduleType, date, note));
        }
        return 1;
    }

    private CachedBody getCachedOrFetch(String apiName, String requestKey, Supplier<String> fetcher) {
        return externalApiResponseRepository
                .findByProviderAndApiNameAndRequestKey(PROVIDER, apiName, requestKey)
                .map(response -> new CachedBody(response.getResponseBody(), true))
                .orElseGet(() -> {
                    String body = fetcher.get();
                    externalApiResponseRepository.save(ExternalApiResponse.create(
                            PROVIDER,
                            apiName,
                            requestKey,
                            body == null ? "" : body,
                            isBlockedOrEmpty(body) ? "BLOCKED_OR_EMPTY" : "OK"
                    ));
                    return new CachedBody(body == null ? "" : body, false);
                });
    }

    private void addCounter(CachedBody cachedBody, KindCounter counter) {
        if (cachedBody.cached()) {
            counter.cachedResponseCount++;
        } else {
            counter.fetchedResponseCount++;
        }
    }

    private boolean isBlockedOrEmpty(String body) {
        if (body == null || body.isBlank()) {
            return true;
        }
        return body.contains("img_notice.png") || body.contains("bg_visual.png");
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("주식회사", "")
                .replace("(주)", "")
                .replace("㈜", "")
                .replaceAll("\\s+", "")
                .toLowerCase();
    }

    private record CachedBody(String body, boolean cached) {
    }

    private record DateRange(LocalDate startDate, LocalDate endDate) {
    }

    private record SupplementData(
            String stockCode,
            String marketType,
            LocalDate listingDate,
            LocalDate demandForecastStartDate,
            LocalDate demandForecastEndDate
    ) {
        static SupplementData empty() {
            return new SupplementData(null, null, null, null, null);
        }

        boolean hasAnyValue() {
            return stockCode != null
                    || marketType != null
                    || listingDate != null
                    || demandForecastStartDate != null
                    || demandForecastEndDate != null;
        }
    }

    private static class SupplementAccumulator {
        private String stockCode;
        private String marketType;
        private LocalDate listingDate;
        private LocalDate demandForecastStartDate;
        private LocalDate demandForecastEndDate;

        private void merge(SupplementData data) {
            if (data.stockCode() != null) {
                stockCode = data.stockCode();
            }
            if (data.marketType() != null) {
                marketType = data.marketType();
            }
            if (data.listingDate() != null) {
                listingDate = data.listingDate();
            }
            if (data.demandForecastStartDate() != null) {
                demandForecastStartDate = data.demandForecastStartDate();
            }
            if (data.demandForecastEndDate() != null) {
                demandForecastEndDate = data.demandForecastEndDate();
            }
        }

        private SupplementData toData() {
            return new SupplementData(
                    stockCode,
                    marketType,
                    listingDate,
                    demandForecastStartDate,
                    demandForecastEndDate
            );
        }
    }

    private static class KindCounter {
        private int cachedResponseCount;
        private int fetchedResponseCount;
        private int matchedStockCount;
        private int supplementedStockCount;
        private int supplementedScheduleCount;
    }
}
