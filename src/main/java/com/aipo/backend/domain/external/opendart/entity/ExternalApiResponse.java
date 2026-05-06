package com.aipo.backend.domain.external.opendart.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "external_api_response",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_external_api_response_request",
                        columnNames = {"provider", "api_name", "request_key"}
                )
        }
)
public class ExternalApiResponse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "external_api_response_id")
    private Long id;

    @Column(name = "provider", nullable = false, length = 30)
    private String provider;

    @Column(name = "api_name", nullable = false, length = 80)
    private String apiName;

    @Column(name = "request_key", nullable = false, length = 500)
    private String requestKey;

    @Column(name = "response_body", nullable = false, columnDefinition = "TEXT")
    private String responseBody;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "fetched_at", nullable = false)
    private LocalDateTime fetchedAt;

    public static ExternalApiResponse create(
            String provider,
            String apiName,
            String requestKey,
            String responseBody,
            String status
    ) {
        ExternalApiResponse response = new ExternalApiResponse();
        response.provider = provider;
        response.apiName = apiName;
        response.requestKey = requestKey;
        response.responseBody = responseBody;
        response.status = status;
        response.fetchedAt = LocalDateTime.now();
        return response;
    }
}
