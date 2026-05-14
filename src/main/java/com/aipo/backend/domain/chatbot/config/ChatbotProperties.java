package com.aipo.backend.domain.chatbot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "chatbot")
public record ChatbotProperties(
        String baseUrl,
        Integer timeoutSeconds
) {

    public String resolvedBaseUrl() {
        if (baseUrl == null || baseUrl.isBlank()) {
            return "http://localhost:8000";
        }
        return baseUrl;
    }

    public int resolvedTimeoutSeconds() {
        if (timeoutSeconds == null || timeoutSeconds < 1) {
            return 30;
        }
        return timeoutSeconds;
    }
}
