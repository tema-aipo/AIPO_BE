package com.aipo.backend.domain.calendar.dto;

import java.time.LocalDate;
import java.util.List;

public record SelectedDateSection(
        LocalDate selectedDate,
        List<SelectedDateCompanyItem> companies
) {
}
