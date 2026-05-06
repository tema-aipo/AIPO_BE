package com.aipo.backend.domain.external.opendart.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "external.opendart")
public record OpenDartProperties(
        String apiKey,
        String baseUrl,
        Sync sync
) {
    public boolean hasApiKey() {
        return apiKey != null && !apiKey.isBlank();
    }

    public int collectionMonths() {
        if (sync == null || sync.collectionMonths == null || sync.collectionMonths < 1) {
            return 3;
        }
        return sync.collectionMonths;
    }

    public boolean syncEnabled() {
        return sync == null || sync.enabled == null || sync.enabled;
    }

    public String syncCron() {
        if (sync == null || sync.cron == null || sync.cron.isBlank()) {
            return "0 0 3 * * *";
        }
        return sync.cron;
    }

    public String syncZone() {
        if (sync == null || sync.zone == null || sync.zone.isBlank()) {
            return "Asia/Seoul";
        }
        return sync.zone;
    }

    public record Sync(
            Boolean enabled,
            Integer collectionMonths,
            String cron,
            String zone
    ) {
    }
}
