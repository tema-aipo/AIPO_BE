package com.aipo.backend.domain.external.opendart.repository;

import com.aipo.backend.domain.external.opendart.entity.ExternalApiResponse;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ExternalApiResponseRepository extends JpaRepository<ExternalApiResponse, Long> {

    Optional<ExternalApiResponse> findByProviderAndApiNameAndRequestKey(
            String provider,
            String apiName,
            String requestKey
    );
}
