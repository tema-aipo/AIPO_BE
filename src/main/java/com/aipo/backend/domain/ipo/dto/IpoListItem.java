package com.aipo.backend.domain.ipo.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "IPO list card item")
public record IpoListItem(
        @Schema(description = "IPO id", example = "1")
        Long ipoId,
        @Schema(description = "Stock name", example = "AIPO")
        String stockName,
        @Schema(description = "Company name", example = "AIPO")
        String companyName,
        @Schema(description = "Market type", example = "KOSDAQ")
        String marketType,
        @Schema(description = "One line description")
        String oneLineDescription,
        @Schema(description = "Confirmed offer price", example = "15000.00")
        BigDecimal confirmedOfferPrice,
        @Schema(description = "Subscription start date", example = "2026-04-28")
        LocalDate subscriptionStartDate,
        @Schema(description = "Subscription end date", example = "2026-04-29")
        LocalDate subscriptionEndDate,
        @Schema(description = "Listing date", example = "2026-05-08")
        LocalDate listingDate,
        @Schema(description = "Attraction score", example = "87.5")
        BigDecimal attractionScore,
        @Schema(description = "Recent growth score", example = "91")
        Integer recentGrowthScore,
        String demandForecastDate,
        String refundDate
) {
    public IpoListItem(
            Long ipoId,
            String stockName,
            String companyName,
            String marketType,
            String oneLineDescription,
            BigDecimal confirmedOfferPrice,
            LocalDate subscriptionStartDate,
            LocalDate subscriptionEndDate,
            LocalDate listingDate,
            BigDecimal attractionScore,
            Integer recentGrowthScore
    ) {
        this(
                ipoId,
                stockName,
                companyName,
                marketType,
                oneLineDescription,
                confirmedOfferPrice,
                subscriptionStartDate,
                subscriptionEndDate,
                listingDate,
                attractionScore,
                recentGrowthScore,
                null,
                null
        );
    }
}
