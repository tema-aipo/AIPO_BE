package com.aipo.backend.domain.ipo.dto;

import java.math.BigDecimal;

public record OfferingInfoSection(
        BigDecimal marketCap,
        BigDecimal equalAllocationRatio,
        BigDecimal circulatingRatio,
        BigDecimal oldShareSaleRatio
) {
}
