package com.aipo.backend.domain.home.dto;

import java.time.LocalDate;

public record FeaturedIpoCandidate(
        Long ipoId,
        String name,
        Long viewCount,
        LocalDate listingDate,
        LocalDate subscriptionStartDate,
        LocalDate subscriptionEndDate
) {
}
