package com.aipo.backend.domain.ipo.dto;

import java.math.BigDecimal;

public record DemandForecastSection(
        BigDecimal institutionalCompetitionRate,
        Integer participatingInstitutionCount,
        BigDecimal aboveUpperPriceCompetitionRate,
        Integer aboveUpperPriceInstitutionCount,
        BigDecimal lockupCompetitionRate,
        Integer lockupInstitutionCount,
        BigDecimal lockupRate
) {
}
