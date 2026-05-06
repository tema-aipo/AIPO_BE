package com.aipo.backend.domain.external.opendart.service;

import com.aipo.backend.domain.external.common.dto.ExternalIpoSourceDataCommand;
import com.aipo.backend.domain.external.common.service.IpoExternalSourceDataService;
import com.aipo.backend.domain.external.opendart.client.OpenDartClient;
import com.aipo.backend.domain.external.opendart.dto.OpenDartIpoSyncResponse;
import com.aipo.backend.domain.external.opendart.entity.ExternalApiResponse;
import com.aipo.backend.domain.external.opendart.entity.IpoExternalDisclosure;
import com.aipo.backend.domain.external.opendart.repository.ExternalApiResponseRepository;
import com.aipo.backend.domain.external.opendart.repository.IpoExternalDisclosureRepository;
import com.aipo.backend.domain.external.kind.dto.KindIpoSupplementResult;
import com.aipo.backend.domain.external.kind.service.KindIpoSupplementService;
import com.aipo.backend.domain.ipo.entity.IpoLeadManager;
import com.aipo.backend.domain.ipo.entity.IpoSchedule;
import com.aipo.backend.domain.ipo.entity.IpoStock;
import com.aipo.backend.domain.ipo.entity.ScheduleType;
import com.aipo.backend.domain.ipo.repository.IpoLeadManagerRepository;
import com.aipo.backend.domain.ipo.repository.IpoScheduleRepository;
import com.aipo.backend.domain.ipo.repository.IpoStockRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class OpenDartIpoSyncService {

    private static final String PROVIDER = "OPENDART";
    private static final String API_DISCLOSURE_SEARCH = "disclosure_search";
    private static final String API_EQUITY_SECURITIES = "equity_securities";
    private static final String API_COMPANY_OVERVIEW = "company_overview";
    private static final int PAGE_COUNT = 100;
    private static final DateTimeFormatter DART_DATE = DateTimeFormatter.BASIC_ISO_DATE;
    private static final Pattern COMPACT_DATE = Pattern.compile("(?<!\\d)(\\d{8})(?!\\d)");
    private static final Pattern SEPARATED_DATE = Pattern.compile("(\\d{4})\\s*[.\\-/년]\\s*(\\d{1,2})\\s*[.\\-/월]\\s*(\\d{1,2})");

    private final OpenDartClient openDartClient;
    private final ObjectMapper objectMapper;
    private final ExternalApiResponseRepository externalApiResponseRepository;
    private final IpoExternalDisclosureRepository ipoExternalDisclosureRepository;
    private final IpoStockRepository ipoStockRepository;
    private final IpoLeadManagerRepository ipoLeadManagerRepository;
    private final IpoScheduleRepository ipoScheduleRepository;
    private final KindIpoSupplementService kindIpoSupplementService;
    private final IpoExternalSourceDataService ipoExternalSourceDataService;

    @Transactional
    public OpenDartIpoSyncResponse syncRecentMonth() {
        return syncRecentMonths(1);
    }

    @Transactional
    public OpenDartIpoSyncResponse syncRecentMonths(int months) {
        LocalDate endDate = LocalDate.now(ZoneId.of("Asia/Seoul"));
        LocalDate startDate = endDate.minusMonths(Math.max(1, months));
        SyncCounter counter = new SyncCounter();

        List<IpoExternalDisclosure> disclosures = collectDisclosures(startDate, endDate, counter);
        processDetails(startDate, endDate, disclosures, counter);
        KindIpoSupplementResult kindResult = supplementFromKind(startDate, endDate);

        return new OpenDartIpoSyncResponse(
                startDate,
                endDate,
                counter.searchedDisclosureCount,
                counter.newDisclosureCount,
                counter.cachedSearchResponseCount,
                counter.fetchedSearchResponseCount,
                counter.cachedDetailResponseCount,
                counter.fetchedDetailResponseCount,
                counter.cachedCompanyOverviewResponseCount,
                counter.fetchedCompanyOverviewResponseCount,
                counter.processedDisclosureCount,
                counter.fallbackStockCount,
                counter.upsertedStockCount,
                counter.failedDisclosureCount,
                kindResult.cachedResponseCount(),
                kindResult.fetchedResponseCount(),
                kindResult.matchedStockCount(),
                kindResult.supplementedStockCount(),
                kindResult.supplementedScheduleCount()
        );
    }

    private KindIpoSupplementResult supplementFromKind(LocalDate startDate, LocalDate endDate) {
        try {
            return kindIpoSupplementService.supplement(startDate, endDate);
        } catch (Exception e) {
            return KindIpoSupplementResult.empty();
        }
    }

    private List<IpoExternalDisclosure> collectDisclosures(
            LocalDate startDate,
            LocalDate endDate,
            SyncCounter counter
    ) {
        List<IpoExternalDisclosure> disclosures = new ArrayList<>();
        int pageNo = 1;
        int totalPage = 1;

        while (pageNo <= totalPage) {
            int currentPageNo = pageNo;
            String requestKey = "bgn_de=%s&end_de=%s&pblntf_detail_ty=C001&page_no=%d&page_count=%d"
                    .formatted(startDate.format(DART_DATE), endDate.format(DART_DATE), currentPageNo, PAGE_COUNT);
            CachedBody cachedBody = getCachedOrFetch(
                    API_DISCLOSURE_SEARCH,
                    requestKey,
                    () -> openDartClient.searchEquitySecuritiesReports(startDate, endDate, currentPageNo, PAGE_COUNT)
            );
            if (cachedBody.cached()) {
                counter.cachedSearchResponseCount++;
            } else {
                counter.fetchedSearchResponseCount++;
            }

            JsonNode root = readTree(cachedBody.body());
            if (!isNormalOrNoData(root)) {
                break;
            }
            totalPage = Math.max(1, root.path("total_page").asInt(1));

            for (JsonNode item : iterable(root.path("list"))) {
                counter.searchedDisclosureCount++;
                String reportName = text(item, "report_nm");
                String stockCode = text(item, "stock_code");
                if (!isIpoCandidateReport(reportName, stockCode)) {
                    continue;
                }

                String rceptNo = text(item, "rcept_no");
                if (rceptNo == null) {
                    continue;
                }

                Optional<IpoExternalDisclosure> existingDisclosure =
                        ipoExternalDisclosureRepository.findByRceptNo(rceptNo);
                if (existingDisclosure.isPresent()) {
                    if (shouldReprocess(existingDisclosure.get())) {
                        disclosures.add(existingDisclosure.get());
                    }
                    continue;
                }

                IpoExternalDisclosure disclosure = IpoExternalDisclosure.create(
                        rceptNo,
                        text(item, "corp_code"),
                        text(item, "corp_name"),
                        text(item, "corp_cls"),
                        stockCode,
                        reportName,
                        parseDartDate(text(item, "rcept_dt"))
                );
                disclosures.add(ipoExternalDisclosureRepository.save(disclosure));
                counter.newDisclosureCount++;
            }

            pageNo++;
        }

        return disclosures;
    }

    private boolean shouldReprocess(IpoExternalDisclosure disclosure) {
        if (!disclosure.isProcessed()) {
            return true;
        }
        boolean missingDisclosureSource =
                !ipoExternalSourceDataService.exists(PROVIDER, API_DISCLOSURE_SEARCH, disclosure.getRceptNo());
        boolean missingEquitySource =
                !ipoExternalSourceDataService.exists(PROVIDER, API_EQUITY_SECURITIES, disclosure.getRceptNo());
        boolean missingCompanyOverviewSource = disclosure.getCorpCode() != null
                && !disclosure.getCorpCode().isBlank()
                && !ipoExternalSourceDataService.exists(PROVIDER, API_COMPANY_OVERVIEW, disclosure.getCorpCode());
        return missingDisclosureSource || missingEquitySource || missingCompanyOverviewSource;
    }

    private void processDetails(
            LocalDate startDate,
            LocalDate endDate,
            List<IpoExternalDisclosure> disclosures,
            SyncCounter counter
    ) {
        Set<String> corpCodes = new LinkedHashSet<>();
        for (IpoExternalDisclosure disclosure : disclosures) {
            if (disclosure.getCorpCode() != null && !disclosure.getCorpCode().isBlank()) {
                corpCodes.add(disclosure.getCorpCode());
            }
        }

        for (String corpCode : corpCodes) {
            supplementCompanyOverview(corpCode, counter);

            String requestKey = "corp_code=%s&bgn_de=%s&end_de=%s"
                    .formatted(corpCode, startDate.format(DART_DATE), endDate.format(DART_DATE));
            CachedBody cachedBody = getCachedOrFetch(
                    API_EQUITY_SECURITIES,
                    requestKey,
                    () -> openDartClient.fetchEquitySecurities(corpCode, startDate, endDate)
            );
            if (cachedBody.cached()) {
                counter.cachedDetailResponseCount++;
            } else {
                counter.fetchedDetailResponseCount++;
            }

            Map<String, EquitySecurityData> byReceiptNo = parseEquitySecurityData(cachedBody.body());
            List<IpoExternalDisclosure> targetDisclosures = disclosures.stream()
                    .filter(disclosure -> corpCode.equals(disclosure.getCorpCode()))
                    .toList();

            for (IpoExternalDisclosure disclosure : targetDisclosures) {
                EquitySecurityData data = byReceiptNo.get(disclosure.getRceptNo());
                if (data == null) {
                    upsertFallbackIpoData(disclosure, counter);
                    disclosure.markProcessed();
                    counter.processedDisclosureCount++;
                    counter.fallbackStockCount++;
                    continue;
                }
                upsertIpoData(disclosure, data, counter);
                disclosure.markProcessed();
                counter.processedDisclosureCount++;
            }
        }
    }

    private void supplementCompanyOverview(String corpCode, SyncCounter counter) {
        String requestKey = "corp_code=%s".formatted(corpCode);
        CachedBody cachedBody = getCachedOrFetch(
                API_COMPANY_OVERVIEW,
                requestKey,
                () -> openDartClient.fetchCompanyOverview(corpCode)
        );
        if (cachedBody.cached()) {
            counter.cachedCompanyOverviewResponseCount++;
        } else {
            counter.fetchedCompanyOverviewResponseCount++;
        }

        CompanyOverviewData company = parseCompanyOverview(cachedBody.body());
        if (!company.hasAnyValue()) {
            return;
        }

        ipoExternalSourceDataService.upsert(new ExternalIpoSourceDataCommand(
                PROVIDER,
                API_COMPANY_OVERVIEW,
                corpCode,
                firstNonBlank(company.stockName(), company.corpName()),
                corpCode,
                company.stockCode(),
                mapMarketType(company.corpCls()),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                60
        ));
        ipoExternalSourceDataService.mergeByDartCorpCode(corpCode);
    }

    private void upsertFallbackIpoData(IpoExternalDisclosure disclosure, SyncCounter counter) {
        String stockName = disclosure.getCorpName();
        String companyName = disclosure.getCorpName();
        String marketType = mapMarketType(disclosure.getCorpCls());

        ipoExternalSourceDataService.upsert(new ExternalIpoSourceDataCommand(
                PROVIDER,
                API_DISCLOSURE_SEARCH,
                disclosure.getRceptNo(),
                companyName,
                disclosure.getCorpCode(),
                disclosure.getStockCode(),
                marketType,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                40
        ));

        findExistingStock(disclosure.getCorpCode())
                .map(existing -> {
                    existing.updateFromExternal(
                            stockName,
                            companyName,
                            disclosure.getStockCode(),
                            marketType,
                            null,
                            null,
                            null
                    );
                    return existing;
                })
                .orElseGet(() -> ipoStockRepository.save(IpoStock.createFromExternal(
                        stockName,
                        companyName,
                        disclosure.getStockCode(),
                        disclosure.getCorpCode(),
                        marketType,
                        null,
                        null,
                        null
                )));
        ipoExternalSourceDataService.mergeByDartCorpCode(disclosure.getCorpCode());
        counter.upsertedStockCount++;
    }

    private void upsertIpoData(
            IpoExternalDisclosure disclosure,
            EquitySecurityData data,
            SyncCounter counter
    ) {
        String stockName = firstNonBlank(data.corpName(), disclosure.getCorpName());
        String companyName = firstNonBlank(data.corpName(), disclosure.getCorpName());
        String marketType = mapMarketType(firstNonBlank(data.corpCls(), disclosure.getCorpCls()));
        String leadManagers = data.leadManagers() == null || data.leadManagers().isEmpty()
                ? null
                : String.join(",", data.leadManagers());

        ipoExternalSourceDataService.upsert(new ExternalIpoSourceDataCommand(
                PROVIDER,
                API_EQUITY_SECURITIES,
                disclosure.getRceptNo(),
                companyName,
                disclosure.getCorpCode(),
                disclosure.getStockCode(),
                marketType,
                data.subscriptionStartDate(),
                data.subscriptionEndDate(),
                null,
                null,
                data.paymentDate(),
                null,
                data.offerPrice(),
                leadManagers,
                null,
                70
        ));

        IpoStock stock = findExistingStock(disclosure.getCorpCode())
                .map(existing -> {
                    existing.updateFromExternal(
                            stockName,
                            companyName,
                            null,
                            marketType,
                            data.offerPrice(),
                            data.subscriptionStartDate(),
                            data.subscriptionEndDate()
                    );
                    return existing;
                })
                .orElseGet(() -> ipoStockRepository.save(IpoStock.createFromExternal(
                        stockName,
                        companyName,
                        null,
                        disclosure.getCorpCode(),
                        marketType,
                        data.offerPrice(),
                        data.subscriptionStartDate(),
                        data.subscriptionEndDate()
                )));
        counter.upsertedStockCount++;

        replaceLeadManagers(stock, data.leadManagers());
        upsertSchedule(stock, ScheduleType.SUBSCRIPTION_START, data.subscriptionStartDate(), "OpenDART 청약 시작일");
        upsertSchedule(stock, ScheduleType.SUBSCRIPTION_END, data.subscriptionEndDate(), "OpenDART 청약 종료일");
        upsertSchedule(stock, ScheduleType.REFUND, data.paymentDate(), "OpenDART 납입기일");
        ipoExternalSourceDataService.mergeByDartCorpCode(disclosure.getCorpCode());
    }

    private Optional<IpoStock> findExistingStock(String dartCorpCode) {
        if (dartCorpCode == null || dartCorpCode.isBlank()) {
            return Optional.empty();
        }
        return ipoStockRepository.findAllByDartCorpCode(dartCorpCode)
                .stream()
                .findFirst();
    }

    private void replaceLeadManagers(IpoStock stock, Set<String> leadManagers) {
        if (leadManagers == null || leadManagers.isEmpty()) {
            return;
        }
        ipoLeadManagerRepository.deleteAllByStock_Id(stock.getId());
        ipoLeadManagerRepository.flush();

        int displayOrder = 1;
        for (String leadManager : leadManagers) {
            ipoLeadManagerRepository.save(IpoLeadManager.create(stock, leadManager, displayOrder++));
        }
    }

    private void upsertSchedule(IpoStock stock, ScheduleType scheduleType, LocalDate date, String note) {
        if (date == null) {
            return;
        }
        ipoScheduleRepository.findByStock_IdAndScheduleType(stock.getId(), scheduleType)
                .ifPresentOrElse(
                        schedule -> schedule.updateDate(date, note),
                        () -> ipoScheduleRepository.save(IpoSchedule.create(stock, scheduleType, date, note))
                );
    }

    private Map<String, EquitySecurityData> parseEquitySecurityData(String responseBody) {
        JsonNode root = readTree(responseBody);
        Map<String, EquitySecurityAccumulator> accumulators = new LinkedHashMap<>();

        if (!isNormalOrNoData(root)) {
            return Map.of();
        }

        for (JsonNode item : root.findParents("rcept_no")) {
            String rceptNo = text(item, "rcept_no");
            if (rceptNo == null) {
                continue;
            }

            EquitySecurityAccumulator accumulator =
                    accumulators.computeIfAbsent(rceptNo, ignored -> new EquitySecurityAccumulator());
            accumulator.corpCode = firstNonBlank(accumulator.corpCode, text(item, "corp_code"));
            accumulator.corpName = firstNonBlank(accumulator.corpName, text(item, "corp_name"));
            accumulator.corpCls = firstNonBlank(accumulator.corpCls, text(item, "corp_cls"));

            DateRange subscriptionPeriod = parseDateRange(text(item, "sbd"));
            if (subscriptionPeriod != null) {
                accumulator.subscriptionStartDate = subscriptionPeriod.startDate();
                accumulator.subscriptionEndDate = subscriptionPeriod.endDate();
            }

            accumulator.paymentDate = firstNonNull(accumulator.paymentDate, parseFirstDate(text(item, "pymd")));
            accumulator.offerPrice = firstNonNull(accumulator.offerPrice, parseMoney(text(item, "slprc")));

            String leadManager = text(item, "actnmn");
            if (leadManager != null && !leadManager.isBlank()) {
                accumulator.leadManagers.add(leadManager);
            }
        }

        Map<String, EquitySecurityData> result = new LinkedHashMap<>();
        accumulators.forEach((rceptNo, accumulator) -> result.put(rceptNo, accumulator.toData()));
        return result;
    }

    private CompanyOverviewData parseCompanyOverview(String responseBody) {
        JsonNode root = readTree(responseBody);
        if (!"000".equals(text(root, "status"))) {
            return CompanyOverviewData.empty();
        }
        return new CompanyOverviewData(
                text(root, "corp_name"),
                text(root, "stock_name"),
                text(root, "stock_code"),
                text(root, "corp_cls")
        );
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
                            body,
                            readStatus(body)
                    ));
                    return new CachedBody(body, false);
                });
    }

    private JsonNode readTree(String body) {
        try {
            return objectMapper.readTree(body);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse OpenDART response.", e);
        }
    }

    private String readStatus(String body) {
        return text(readTree(body), "status");
    }

    private boolean isNormalOrNoData(JsonNode root) {
        String status = text(root, "status");
        return "000".equals(status) || "013".equals(status);
    }

    private List<JsonNode> iterable(JsonNode node) {
        List<JsonNode> nodes = new ArrayList<>();
        if (node == null || node.isMissingNode() || node.isNull()) {
            return nodes;
        }
        if (node.isArray()) {
            node.forEach(nodes::add);
        } else if (node.isObject()) {
            nodes.add(node);
        }
        return nodes;
    }

    private String text(JsonNode node, String fieldName) {
        if (node == null || node.path(fieldName).isMissingNode() || node.path(fieldName).isNull()) {
            return null;
        }
        String value = node.path(fieldName).asText();
        return value == null || value.isBlank() || "-".equals(value.trim()) ? null : value.trim();
    }

    private LocalDate parseDartDate(String value) {
        if (value == null || value.length() != 8) {
            return null;
        }
        return LocalDate.parse(value, DART_DATE);
    }

    private DateRange parseDateRange(String value) {
        List<LocalDate> dates = extractDates(value);
        if (dates.isEmpty()) {
            return null;
        }
        LocalDate startDate = dates.get(0);
        LocalDate endDate = dates.size() > 1 ? dates.get(1) : startDate;
        return new DateRange(startDate, endDate);
    }

    private LocalDate parseFirstDate(String value) {
        List<LocalDate> dates = extractDates(value);
        return dates.isEmpty() ? null : dates.get(0);
    }

    private List<LocalDate> extractDates(String value) {
        List<LocalDate> dates = new ArrayList<>();
        if (value == null || value.isBlank()) {
            return dates;
        }

        Matcher separatedMatcher = SEPARATED_DATE.matcher(value);
        while (separatedMatcher.find()) {
            dates.add(LocalDate.of(
                    Integer.parseInt(separatedMatcher.group(1)),
                    Integer.parseInt(separatedMatcher.group(2)),
                    Integer.parseInt(separatedMatcher.group(3))
            ));
        }

        if (!dates.isEmpty()) {
            return dates;
        }

        Matcher compactMatcher = COMPACT_DATE.matcher(value);
        while (compactMatcher.find()) {
            dates.add(LocalDate.parse(compactMatcher.group(1), DART_DATE));
        }
        return dates;
    }

    private BigDecimal parseMoney(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.replaceAll("[^0-9.]", "");
        if (normalized.isBlank()) {
            return null;
        }
        return new BigDecimal(normalized);
    }

    private String mapMarketType(String corpCls) {
        return switch (corpCls == null ? "" : corpCls) {
            case "Y" -> "KOSPI";
            case "K" -> "KOSDAQ";
            case "N" -> "KONEX";
            default -> "OTHER";
        };
    }

    private boolean isIpoCandidateReport(String reportName, String stockCode) {
        if (reportName == null || !reportName.contains("증권신고서(지분증권)")) {
            return false;
        }

        // OpenDART's C001 search also includes listed-company offerings.
        // For the first OpenDART-only sync, treat non-listed issuers as IPO candidates.
        return stockCode == null || stockCode.isBlank();
    }

    private String firstNonBlank(String first, String second) {
        return first != null && !first.isBlank() ? first : second;
    }

    private <T> T firstNonNull(T first, T second) {
        return first != null ? first : second;
    }

    private record CachedBody(String body, boolean cached) {
    }

    private record DateRange(LocalDate startDate, LocalDate endDate) {
    }

    private record EquitySecurityData(
            String corpCode,
            String corpName,
            String corpCls,
            BigDecimal offerPrice,
            LocalDate subscriptionStartDate,
            LocalDate subscriptionEndDate,
            LocalDate paymentDate,
            Set<String> leadManagers
    ) {
    }

    private record CompanyOverviewData(
            String corpName,
            String stockName,
            String stockCode,
            String corpCls
    ) {
        static CompanyOverviewData empty() {
            return new CompanyOverviewData(null, null, null, null);
        }

        boolean hasAnyValue() {
            return corpName != null || stockName != null || stockCode != null || corpCls != null;
        }
    }

    private static class EquitySecurityAccumulator {
        private String corpCode;
        private String corpName;
        private String corpCls;
        private BigDecimal offerPrice;
        private LocalDate subscriptionStartDate;
        private LocalDate subscriptionEndDate;
        private LocalDate paymentDate;
        private final Set<String> leadManagers = new LinkedHashSet<>();

        private EquitySecurityData toData() {
            return new EquitySecurityData(
                    corpCode,
                    corpName,
                    corpCls,
                    offerPrice,
                    subscriptionStartDate,
                    subscriptionEndDate,
                    paymentDate,
                    leadManagers
            );
        }
    }

    private static class SyncCounter {
        private int searchedDisclosureCount;
        private int newDisclosureCount;
        private int cachedSearchResponseCount;
        private int fetchedSearchResponseCount;
        private int cachedDetailResponseCount;
        private int fetchedDetailResponseCount;
        private int cachedCompanyOverviewResponseCount;
        private int fetchedCompanyOverviewResponseCount;
        private int processedDisclosureCount;
        private int fallbackStockCount;
        private int upsertedStockCount;
        private int failedDisclosureCount;
    }
}
