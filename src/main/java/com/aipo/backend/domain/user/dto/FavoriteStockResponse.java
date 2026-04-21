package com.aipo.backend.domain.user.dto;

import com.aipo.backend.domain.ipo.dto.DateRange;

import java.math.BigDecimal;

public record FavoriteStockResponse(
        Long ipoId,
        String companyName,
        String oneLineDescription,
        BigDecimal confirmedOfferPrice,
        DateRange subscriptionPeriod,
        Boolean isFavorite
) {
}
