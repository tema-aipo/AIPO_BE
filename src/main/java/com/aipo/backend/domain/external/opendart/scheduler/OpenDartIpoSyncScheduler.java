package com.aipo.backend.domain.external.opendart.scheduler;

import com.aipo.backend.domain.external.opendart.config.OpenDartProperties;
import com.aipo.backend.domain.external.opendart.service.OpenDartCombinedIpoSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component
@RequiredArgsConstructor
public class OpenDartIpoSyncScheduler {

    private final OpenDartProperties properties;
    private final OpenDartCombinedIpoSyncService combinedIpoSyncService;
    private final AtomicBoolean running = new AtomicBoolean(false);

    @Scheduled(cron = "${external.opendart.sync.cron:0 0 3 * * *}", zone = "${external.opendart.sync.zone:Asia/Seoul}")
    public void syncAutomatically() {
        if (!properties.syncEnabled()) {
            return;
        }
        if (!properties.hasApiKey()) {
            log.warn("OpenDART automatic IPO sync skipped because API key is not configured.");
            return;
        }
        if (!running.compareAndSet(false, true)) {
            log.warn("OpenDART automatic IPO sync skipped because a previous sync is still running.");
            return;
        }

        try {
            log.info("OpenDART automatic IPO sync started. collectionMonths={}", properties.collectionMonths());
            combinedIpoSyncService.syncConfiguredPeriod();
            log.info("OpenDART automatic IPO sync completed.");
        } catch (Exception e) {
            log.error("OpenDART automatic IPO sync failed.", e);
        } finally {
            running.set(false);
        }
    }
}
