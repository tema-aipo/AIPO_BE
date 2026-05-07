package com.aipo.backend.domain.ipo.service;

import com.aipo.backend.domain.document.entity.Document;
import com.aipo.backend.domain.document.repository.DocumentRepository;
import com.aipo.backend.domain.pipeline.entity.PipelineJob;
import com.aipo.backend.domain.pipeline.repository.PipelineJobRepository;
import com.aipo.backend.domain.user.entity.User;
import com.aipo.backend.domain.user.repository.UserRepository;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class IpoCrawlerService {

    private static final String DART_LIST_URL = "https://opendart.fss.or.kr/api/list.json";
    private static final String DART_DOC_URL  = "https://opendart.fss.or.kr/api/document.xml";
    private static final String SYSTEM_LOGIN_ID = "system";

    private final DocumentRepository documentRepository;
    private final PipelineJobRepository pipelineJobRepository;
    private final UserRepository userRepository;
    private final RestTemplate restTemplate;

    @Value("${ipo.crawler.dart-api-key:}")
    private String dartApiKey;

    @Value("${upload.dir}")
    private String uploadDir;

    /** 크롤링 기준 일수: 오늘부터 며칠 전까지 조회할지 설정 (기본 1일) */
    @Value("${ipo.crawler.lookback-days:1}")
    private int lookbackDays;

    /**
     * DART OpenAPI에서 당일~전일 IPO 증권신고서(지분증권) 공시 목록을 가져와
     * 신규 건만 파일로 저장하고 PipelineJob(PDF_PARSE, QUEUED)을 생성한다.
     */
    public void crawlAndQueue() {
        if (dartApiKey == null || dartApiKey.isBlank()) {
            log.warn("[IPO Crawler] DART API 키가 설정되지 않았습니다. 크롤링이 비활성화되었습니다. " +
                     "DART_API_KEY 환경변수를 설정하세요.");
            return;
        }

        User systemUser = getOrCreateSystemUser();

        LocalDate today    = LocalDate.now();
        LocalDate fromDate = today.minusDays(lookbackDays);
        String bgnDe = fromDate.format(DateTimeFormatter.BASIC_ISO_DATE);
        String endDe = today.format(DateTimeFormatter.BASIC_ISO_DATE);

        List<DartDisclosure> disclosures = fetchDisclosures(bgnDe, endDe);
        log.info("[IPO Crawler] {}~{} 공시 조회 결과: {}건", bgnDe, endDe, disclosures.size());

        int queued = 0;
        for (DartDisclosure disclosure : disclosures) {
            if (documentRepository.existsByExternalId(disclosure.getRceptNo())) {
                log.debug("[IPO Crawler] 이미 처리된 공시 건너뜀: {}", disclosure.getRceptNo());
                continue;
            }
            try {
                saveAndQueue(systemUser, disclosure);
                queued++;
            } catch (Exception e) {
                log.error("[IPO Crawler] 공시 처리 실패 (rcept_no={}): {}",
                        disclosure.getRceptNo(), e.getMessage(), e);
            }
        }
        log.info("[IPO Crawler] 완료. 신규 큐잉: {}건", queued);
    }

    @Transactional(readOnly = true)
    public boolean isDartApiKeyConfigured() {
        return dartApiKey != null && !dartApiKey.isBlank();
    }

    // ── private helpers ──────────────────────────────────────────────────────

    private void saveAndQueue(User uploader, DartDisclosure disclosure) throws IOException {
        byte[] docBytes  = downloadDocument(disclosure.getRceptNo());
        String storedName = disclosure.getRceptNo() + ".zip";

        Path uploadPath = Paths.get(uploadDir);
        Files.createDirectories(uploadPath);
        Path filePath = uploadPath.resolve(storedName);
        Files.write(filePath, docBytes);

        String originalName = disclosure.getReportNm()
                + "_" + disclosure.getCorpName()
                + "_" + disclosure.getRceptDt()
                + ".zip";

        Document document = new Document(
                uploader,
                originalName,
                storedName,
                filePath.toString(),
                (long) docBytes.length,
                "application/zip",
                disclosure.getRceptNo()
        );
        Document saved = documentRepository.save(document);

        PipelineJob job = new PipelineJob(saved, "PDF_PARSE");
        pipelineJobRepository.save(job);

        log.info("[IPO Crawler] 문서 저장 및 큐잉 완료: {} ({})", originalName, disclosure.getRceptNo());
    }

    private List<DartDisclosure> fetchDisclosures(String bgnDe, String endDe) {
        String url = UriComponentsBuilder.fromHttpUrl(DART_LIST_URL)
                .queryParam("crtfc_key", dartApiKey)
                .queryParam("pblntf_ty", "B")
                .queryParam("pblntf_detail_ty", "B001")
                .queryParam("bgn_de", bgnDe)
                .queryParam("end_de", endDe)
                .queryParam("page_no", 1)
                .queryParam("page_count", 100)
                .toUriString();

        DartListResponse response = restTemplate.getForObject(url, DartListResponse.class);

        if (response == null || !"000".equals(response.getStatus())) {
            log.warn("[IPO Crawler] DART API 응답 이상 — status={}, message={}",
                    response != null ? response.getStatus() : "null",
                    response != null ? response.getMessage() : "null");
            return Collections.emptyList();
        }
        return response.getList() != null ? response.getList() : Collections.emptyList();
    }

    private byte[] downloadDocument(String rceptNo) {
        String url = UriComponentsBuilder.fromHttpUrl(DART_DOC_URL)
                .queryParam("crtfc_key", dartApiKey)
                .queryParam("rcept_no", rceptNo)
                .toUriString();
        byte[] bytes = restTemplate.getForObject(url, byte[].class);
        if (bytes == null) {
            throw new IllegalStateException(
                    "DART API에서 문서를 다운로드할 수 없습니다 (응답 없음): rcept_no=" + rceptNo);
        }
        return bytes;
    }

    private User getOrCreateSystemUser() {
        return userRepository.findByLoginId(SYSTEM_LOGIN_ID)
                .orElseGet(() -> {
                    log.info("[IPO Crawler] 시스템 유저 생성: loginId={}", SYSTEM_LOGIN_ID);
                    // 랜덤 UUID로 password_hash를 설정하여 일반 로그인 경로를 통한
                    // 인증이 불가능하도록 한다.
                    return userRepository.save(
                            new User(SYSTEM_LOGIN_ID,
                                    java.util.UUID.randomUUID().toString(),
                                    "System Crawler",
                                    null)
                    );
                });
    }

    // ── DART API 응답 DTO ─────────────────────────────────────────────────────

    @Getter
    public static class DartListResponse {
        private String status;
        private String message;
        private List<DartDisclosure> list;
    }

    @Getter
    public static class DartDisclosure {
        @JsonProperty("rcept_no")
        private String rceptNo;

        @JsonProperty("corp_name")
        private String corpName;

        @JsonProperty("report_nm")
        private String reportNm;

        @JsonProperty("rcept_dt")
        private String rceptDt;
    }
}
