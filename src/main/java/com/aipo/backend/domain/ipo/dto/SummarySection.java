package com.aipo.backend.domain.ipo.dto;

import java.math.BigDecimal;
import java.util.List;

public record SummarySection(
        String companyName,
        String oneLineDescription,
        BigDecimal confirmedOfferPrice,
        List<String> leadManagers,
        DateRange subscriptionPeriod,
        Boolean isFavorite,
        String marketType
) {
}
