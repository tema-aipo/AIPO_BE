package com.aipo.backend.domain.ipo.dto;

import java.time.LocalDate;

public record DateRange(
        LocalDate startDate,
        LocalDate endDate
) {
}
