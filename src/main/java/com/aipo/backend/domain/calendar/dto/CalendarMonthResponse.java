package com.aipo.backend.domain.calendar.dto;

import java.util.List;

public record CalendarMonthResponse(
        Integer year,
        Integer month,
        List<CalendarCellItem> calendarCells,
        SelectedDateSection selectedDateSection
) {
}
