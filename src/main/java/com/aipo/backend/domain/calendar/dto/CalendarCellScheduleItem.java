package com.aipo.backend.domain.calendar.dto;

public record CalendarCellScheduleItem(
        Long ipoId,
        String companyName,
        String scheduleType,
        String scheduleLabel
) {
}
