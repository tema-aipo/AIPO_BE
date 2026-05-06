package com.aipo.backend.domain.external.kind.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "external.kind")
public record KindProperties(
        String baseUrl
) {
}
