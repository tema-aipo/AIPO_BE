package com.aipo.backend.domain.ipo.dto;

import java.time.LocalDate;

public record ScheduleSection(
        DateRange demandForecastPeriod,
        DateRange subscriptionPeriod,
        LocalDate refundDate,
        LocalDate listingDate,
        String demandForecastDate,
        String refundDateText
) {
    public ScheduleSection(
            DateRange demandForecastPeriod,
            DateRange subscriptionPeriod,
            LocalDate refundDate,
            LocalDate listingDate
    ) {
        this(demandForecastPeriod, subscriptionPeriod, refundDate, listingDate, null, null);
    }
}
