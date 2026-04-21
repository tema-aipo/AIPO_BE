package com.aipo.backend.domain.ipo.dto;

import java.math.BigDecimal;

public record CompetitionInfo(
        BigDecimal expectedAllocationQuantity,
        BigDecimal competitionRate
) {
}
