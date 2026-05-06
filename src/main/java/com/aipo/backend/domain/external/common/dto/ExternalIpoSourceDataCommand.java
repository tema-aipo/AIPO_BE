package com.aipo.backend.domain.external.common.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ExternalIpoSourceDataCommand(
        String provider,
        String sourceType,
        String externalKey,
        String corpName,
        String dartCorpCode,
        String stockCode,
        String marketType,
        LocalDate subscriptionStartDate,
        LocalDate subscriptionEndDate,
        LocalDate demandForecastStartDate,
        LocalDate demandForecastEndDate,
        LocalDate refundDate,
        LocalDate listingDate,
        BigDecimal confirmedOfferPrice,
        String leadManagers,
        Long rawResponseId,
        Integer confidence
) {
}
