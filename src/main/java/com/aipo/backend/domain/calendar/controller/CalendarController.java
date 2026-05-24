package com.aipo.backend.domain.calendar.controller;

import com.aipo.backend.domain.calendar.dto.CalendarMonthResponse;
import com.aipo.backend.domain.calendar.service.CalendarService;
import com.aipo.backend.global.security.service.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/calendar")
public class CalendarController {

    private final CalendarService calendarService;

    @GetMapping
    @Operation(summary = "캘린더 월별 일정 조회", description = "연도, 월, 선택 날짜 기준으로 캘린더 월별 일정과 선택 날짜 상세 목록을 조회합니다.")
    public ResponseEntity<CalendarMonthResponse> getMonthlyCalendar(
            @RequestParam int year,
            @RequestParam int month,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate selectedDate,
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        Long userId = principal != null ? principal.getUserId() : null;
        CalendarMonthResponse response = calendarService.getMonthlyCalendar(year, month, selectedDate, userId);
        return ResponseEntity.ok(response);
    }
}
