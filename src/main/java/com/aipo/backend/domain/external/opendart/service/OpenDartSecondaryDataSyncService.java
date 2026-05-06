package com.aipo.backend.domain.external.opendart.service;

import com.aipo.backend.domain.external.common.dto.ExternalIpoSourceDataCommand;
import com.aipo.backend.domain.external.common.service.IpoExternalSourceDataService;
import com.aipo.backend.domain.external.opendart.client.OpenDartClient;
import com.aipo.backend.domain.external.opendart.dto.OpenDartSecondaryDataSyncResponse;
import com.aipo.backend.domain.external.opendart.entity.ExternalApiResponse;
import com.aipo.backend.domain.external.opendart.entity.IpoExternalCompanyProfile;
import com.aipo.backend.domain.external.opendart.repository.ExternalApiResponseRepository;
import com.aipo.backend.domain.external.opendart.repository.IpoExternalCompanyProfileRepository;
import com.aipo.backend.domain.ipo.entity.IpoStock;
import com.aipo.backend.domain.ipo.repository.IpoStockRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
public class OpenDartSecondaryDataSyncService {

    private static final String PROVIDER = "OPENDART";
    private static final String API_COMPANY_OVERVIEW = "company_overview";
    private static final DateTimeFormatter DART_DATE = DateTimeFormatter.BASIC_ISO_DATE;

    private final OpenDartClient openDartClient;
    private final ObjectMapper objectMapper;
    private final ExternalApiResponseRepository externalApiResponseRepository;
    private final IpoExternalCompanyProfileRepository companyProfileRepository;
    private final IpoExternalSourceDataService ipoExternalSourceDataService;
    private final IpoStockRepository ipoStockRepository;

    @Transactional
    public OpenDartSecondaryDataSyncResponse syncCompanyProfiles() {
        SecondaryCounter counter = new SecondaryCounter();
        List<IpoStock> targets = ipoStockRepository.findAll()
                .stream()
                .filter(stock -> stock.getDartCorpCode() != null && !stock.getDartCorpCode().isBlank())
                .toList();
        counter.targetStockCount = targets.size();

        for (IpoStock stock : targets) {
            try {
                syncCompanyProfile(stock, counter);
            } catch (Exception e) {
                counter.failedStockCount++;
            }
        }

        return new OpenDartSecondaryDataSyncResponse(
                counter.targetStockCount,
                counter.cachedCompanyOverviewResponseCount,
                counter.fetchedCompanyOverviewResponseCount,
                counter.upsertedCompanyProfileCount,
                counter.supplementedStockCount,
                counter.failedStockCount
        );
    }

    private void syncCompanyProfile(IpoStock stock, SecondaryCounter counter) {
        String requestKey = "corp_code=%s".formatted(stock.getDartCorpCode());
        CachedResponse cachedResponse = getCachedOrFetch(
                API_COMPANY_OVERVIEW,
                requestKey,
                () -> openDartClient.fetchCompanyOverview(stock.getDartCorpCode())
        );
        if (cachedResponse.cached()) {
            counter.cachedCompanyOverviewResponseCount++;
        } else {
            counter.fetchedCompanyOverviewResponseCount++;
        }

        CompanyOverviewData data = parseCompanyOverview(cachedResponse.response().getResponseBody());
        if (!data.hasAnyValue()) {
            return;
        }

        IpoExternalCompanyProfile profile = companyProfileRepository
                .findByProviderAndDartCorpCode(PROVIDER, stock.getDartCorpCode())
                .orElseGet(() -> companyProfileRepository.save(
                        IpoExternalCompanyProfile.create(PROVIDER, stock.getDartCorpCode())
                ));
        profile.updateData(
                stock,
                data.corpName(),
                data.corpNameEng(),
                data.stockName(),
                data.stockCode(),
                data.marketType(),
                data.ceoName(),
                data.corporationRegistrationNo(),
                data.businessRegistrationNo(),
                data.address(),
                data.homepageUrl(),
                data.irUrl(),
                data.phoneNumber(),
                data.faxNumber(),
                data.industryCode(),
                data.establishedDate(),
                data.fiscalMonth(),
                cachedResponse.response().getId()
        );
        counter.upsertedCompanyProfileCount++;

        ipoExternalSourceDataService.upsert(new ExternalIpoSourceDataCommand(
                PROVIDER,
                API_COMPANY_OVERVIEW,
                stock.getDartCorpCode(),
                firstNonBlank(data.stockName(), data.corpName()),
                stock.getDartCorpCode(),
                data.stockCode(),
                data.marketType(),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                cachedResponse.response().getId(),
                60
        ));
        IpoStock mergedStock = ipoExternalSourceDataService.mergeByDartCorpCode(stock.getDartCorpCode());
        if (mergedStock != null) {
            counter.supplementedStockCount++;
        }
    }

    private CachedResponse getCachedOrFetch(String apiName, String requestKey, Supplier<String> fetcher) {
        return externalApiResponseRepository
                .findByProviderAndApiNameAndRequestKey(PROVIDER, apiName, requestKey)
                .map(response -> new CachedResponse(response, true))
                .orElseGet(() -> {
                    String body = fetcher.get();
                    ExternalApiResponse response = externalApiResponseRepository.save(ExternalApiResponse.create(
                            PROVIDER,
                            apiName,
                            requestKey,
                            body,
                            readStatus(body)
                    ));
                    return new CachedResponse(response, false);
                });
    }

    private CompanyOverviewData parseCompanyOverview(String responseBody) {
        JsonNode root = readTree(responseBody);
        if (!"000".equals(text(root, "status"))) {
            return CompanyOverviewData.empty();
        }
        return new CompanyOverviewData(
                text(root, "corp_name"),
                text(root, "corp_name_eng"),
                text(root, "stock_name"),
                text(root, "stock_code"),
                mapMarketType(text(root, "corp_cls")),
                text(root, "ceo_nm"),
                text(root, "jurir_no"),
                text(root, "bizr_no"),
                text(root, "adres"),
                text(root, "hm_url"),
                text(root, "ir_url"),
                text(root, "phn_no"),
                text(root, "fax_no"),
                text(root, "induty_code"),
                parseDartDate(text(root, "est_dt")),
                text(root, "acc_mt")
        );
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

    private String mapMarketType(String corpCls) {
        return switch (corpCls == null ? "" : corpCls) {
            case "Y" -> "KOSPI";
            case "K" -> "KOSDAQ";
            case "N" -> "KONEX";
            default -> "OTHER";
        };
    }

    private String firstNonBlank(String first, String second) {
        return first != null && !first.isBlank() ? first : second;
    }

    private record CachedResponse(ExternalApiResponse response, boolean cached) {
    }

    private record CompanyOverviewData(
            String corpName,
            String corpNameEng,
            String stockName,
            String stockCode,
            String marketType,
            String ceoName,
            String corporationRegistrationNo,
            String businessRegistrationNo,
            String address,
            String homepageUrl,
            String irUrl,
            String phoneNumber,
            String faxNumber,
            String industryCode,
            LocalDate establishedDate,
            String fiscalMonth
    ) {
        static CompanyOverviewData empty() {
            return new CompanyOverviewData(
                    null, null, null, null, null, null, null, null,
                    null, null, null, null, null, null, null, null
            );
        }

        boolean hasAnyValue() {
            return corpName != null
                    || stockName != null
                    || stockCode != null
                    || marketType != null
                    || ceoName != null
                    || address != null
                    || industryCode != null;
        }
    }

    private static class SecondaryCounter {
        private int targetStockCount;
        private int cachedCompanyOverviewResponseCount;
        private int fetchedCompanyOverviewResponseCount;
        private int upsertedCompanyProfileCount;
        private int supplementedStockCount;
        private int failedStockCount;
    }
}
