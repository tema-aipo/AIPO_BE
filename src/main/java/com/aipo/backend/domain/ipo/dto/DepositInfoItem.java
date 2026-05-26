package com.aipo.backend.domain.ipo.dto;

public record DepositInfoItem(
        Integer displayOrder,
        String securitiesCompanyName,
        Integer allocatedShareCount,
        Integer subscriptionLimitShareCount,
        String note
) {
}
