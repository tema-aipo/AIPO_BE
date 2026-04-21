package com.aipo.backend.domain.calendar.dto;

import java.math.BigDecimal;

public record SelectedDateCompanyItem(
        Long ipoId,
        String companyName,
        String securitiesCompanyName,
        BigDecimal attractionScore,
        String scheduleType,
        String scheduleLabel
) {
}
