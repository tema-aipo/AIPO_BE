package com.aipo.backend.domain.home.dto;

import java.time.LocalDate;

public record AttractivenessItem(
        Long ipoId,
        String name,
        Integer score,
        LocalDate subscriptionStartDate,
        LocalDate subscriptionEndDate,
        String leadManager,
        String demandForecastDate,
        String refundDate,
        LocalDate listingDate
) {
    public AttractivenessItem(
            Long ipoId,
            String name,
            Integer score,
            LocalDate subscriptionStartDate,
            LocalDate subscriptionEndDate
    ) {
        this(ipoId, name, score, subscriptionStartDate, subscriptionEndDate, null, null, null, null);
    }

    public AttractivenessItem(
            Long ipoId,
            String name,
            Integer score,
            LocalDate subscriptionStartDate,
            LocalDate subscriptionEndDate,
            String leadManager
    ) {
        this(ipoId, name, score, subscriptionStartDate, subscriptionEndDate, leadManager, null, null, null);
    }
}
