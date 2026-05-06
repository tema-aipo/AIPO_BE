package com.aipo.backend.domain.external.kind.client;

import com.aipo.backend.domain.external.kind.config.KindProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
public class KindClient {

    private final KindProperties properties;

    public String fetchPublicOfferingSchedule(int year, int month) {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("method", "searchPubofrScholCalnd");
        body.add("forward", "pubofrSchol_sub");
        body.add("marketType", "");
        body.add("scholType", "");
        body.add("selYear", String.valueOf(year));
        body.add("selMonth", "%02d".formatted(month));

        return restClient().post()
                .uri("/listinvstg/pubofrschdl.do")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .headers(headers -> {
                    headers.set("User-Agent", "Mozilla/5.0");
                    headers.set("Referer", properties.baseUrl() + "/listinvstg/pubofrschdl.do?method=searchPubofrScholMain");
                    headers.set("X-Requested-With", "XMLHttpRequest");
                })
                .body(body)
                .retrieve()
                .body(String.class);
    }

    public String fetchPublicOfferingCompanies(String fromDate, String toDate) {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("method", "searchPubofrProgComSub");
        body.add("forward", "pubofrprogcom_sub");
        body.add("marketType", "");
        body.add("currentPageSize", "3000");
        body.add("pageIndex", "1");
        body.add("orderMode", "1");
        body.add("orderStat", "D");
        body.add("searchCorpName", "");
        body.add("fromDate", fromDate);
        body.add("toDate", toDate);

        return restClient().post()
                .uri("/listinvstg/pubofrprogcom.do")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .headers(headers -> {
                    headers.set("User-Agent", "Mozilla/5.0");
                    headers.set("Referer", properties.baseUrl() + "/listinvstg/pubofrprogcom.do?method=searchPubofrProgComMain");
                    headers.set("X-Requested-With", "XMLHttpRequest");
                })
                .body(body)
                .retrieve()
                .body(String.class);
    }

    private RestClient restClient() {
        return RestClient.builder()
                .baseUrl(properties.baseUrl())
                .build();
    }
}
