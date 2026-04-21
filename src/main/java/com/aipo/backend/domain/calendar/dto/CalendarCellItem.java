package com.aipo.backend.domain.calendar.dto;

import java.time.LocalDate;
import java.util.List;

public record CalendarCellItem(
        LocalDate date,
        Boolean inCurrentMonth,
        List<CalendarCellScheduleItem> items
) {
}
