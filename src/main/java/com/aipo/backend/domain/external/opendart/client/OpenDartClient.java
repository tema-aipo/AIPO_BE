package com.aipo.backend.domain.external.opendart.client;

import com.aipo.backend.domain.external.opendart.config.OpenDartProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriBuilder;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Component
@RequiredArgsConstructor
public class OpenDartClient {

    private static final DateTimeFormatter DART_DATE = DateTimeFormatter.BASIC_ISO_DATE;

    private final OpenDartProperties properties;

    public String searchEquitySecuritiesReports(LocalDate startDate, LocalDate endDate, int pageNo, int pageCount) {
        validateApiKey();
        RestClient restClient = restClient();

        return restClient.get()
                .uri(uriBuilder -> baseQuery(uriBuilder, "/list.json")
                        .queryParam("bgn_de", startDate.format(DART_DATE))
                        .queryParam("end_de", endDate.format(DART_DATE))
                        .queryParam("last_reprt_at", "Y")
                        .queryParam("pblntf_ty", "C")
                        .queryParam("pblntf_detail_ty", "C001")
                        .queryParam("sort", "date")
                        .queryParam("sort_mth", "desc")
                        .queryParam("page_no", pageNo)
                        .queryParam("page_count", pageCount)
                        .build())
                .retrieve()
                .body(String.class);
    }

    public String fetchEquitySecurities(String corpCode, LocalDate startDate, LocalDate endDate) {
        validateApiKey();
        RestClient restClient = restClient();

        return restClient.get()
                .uri(uriBuilder -> baseQuery(uriBuilder, "/estkRs.json")
                        .queryParam("corp_code", corpCode)
                        .queryParam("bgn_de", startDate.format(DART_DATE))
                        .queryParam("end_de", endDate.format(DART_DATE))
                        .build())
                .retrieve()
                .body(String.class);
    }

    public String fetchCompanyOverview(String corpCode) {
        validateApiKey();
        RestClient restClient = restClient();

        return restClient.get()
                .uri(uriBuilder -> baseQuery(uriBuilder, "/company.json")
                        .queryParam("corp_code", corpCode)
                        .build())
                .retrieve()
                .body(String.class);
    }

    private UriBuilder baseQuery(UriBuilder uriBuilder, String path) {
        return uriBuilder
                .path(path)
                .queryParam("crtfc_key", properties.apiKey());
    }

    private RestClient restClient() {
        return RestClient.builder()
                .baseUrl(properties.baseUrl())
                .build();
    }

    private void validateApiKey() {
        if (!properties.hasApiKey()) {
            throw new IllegalStateException("OpenDART API key is not configured.");
        }
    }
}
