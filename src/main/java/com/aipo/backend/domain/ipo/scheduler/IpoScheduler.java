package com.aipo.backend.domain.ipo.scheduler;

import com.aipo.backend.domain.ipo.service.IpoCrawlerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class IpoScheduler {

    private final IpoCrawlerService ipoCrawlerService;

    /**
     * 평일 오전 8시(KST)에 DART OpenAPI를 통해 신규 IPO 공시를 수집하고
     * PipelineJob 큐에 등록한다.
     * cron 표현식은 application.yaml의 ipo.crawler.cron으로 재정의 가능하다.
     */
    @Scheduled(cron = "${ipo.crawler.cron:0 0 8 * * MON-FRI}", zone = "Asia/Seoul")
    public void crawlDailyIpoDisclosures() {
        log.info("[IPO Scheduler] 정기 크롤링 시작");
        try {
            ipoCrawlerService.crawlAndQueue();
        } catch (Exception e) {
            log.error("[IPO Scheduler] 크롤링 중 예외 발생: {}", e.getMessage(), e);
        }
    }
}
