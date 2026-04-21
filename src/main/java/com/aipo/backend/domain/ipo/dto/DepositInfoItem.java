package com.aipo.backend.domain.ipo.dto;

import java.math.BigDecimal;

public record DepositInfoItem(
        Integer displayOrder,
        String securitiesCompanyName,
        BigDecimal amountForTenShares
) {
}
