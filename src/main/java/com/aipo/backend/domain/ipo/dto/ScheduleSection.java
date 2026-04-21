package com.aipo.backend.domain.ipo.dto;

import java.time.LocalDate;

public record ScheduleSection(
        DateRange demandForecastPeriod,
        DateRange subscriptionPeriod,
        LocalDate refundDate,
        LocalDate listingDate
) {
}
