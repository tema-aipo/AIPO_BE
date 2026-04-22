package com.aipo.backend.domain.calendar.controller;

import com.aipo.backend.domain.calendar.dto.CalendarCellItem;
import com.aipo.backend.domain.calendar.dto.CalendarCellScheduleItem;
import com.aipo.backend.domain.calendar.dto.CalendarMonthResponse;
import com.aipo.backend.domain.calendar.dto.SelectedDateCompanyItem;
import com.aipo.backend.domain.calendar.dto.SelectedDateSection;
import com.aipo.backend.domain.calendar.service.CalendarService;
import com.aipo.backend.global.config.SecurityConfig;
import com.aipo.backend.global.security.jwt.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CalendarController.class)
@Import(SecurityConfig.class)
class CalendarControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CalendarService calendarService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @Test
    @DisplayName("비로그인 사용자도 캘린더 월별 일정을 조회할 수 있다")
    void getMonthlyCalendar_anonymousUserCanAccess() throws Exception {
        CalendarMonthResponse response = createResponse();

        when(calendarService.getMonthlyCalendar(2026, 4, LocalDate.of(2026, 4, 28)))
                .thenReturn(response);

        mockMvc.perform(get("/api/v1/calendar")
                        .queryParam("year", "2026")
                        .queryParam("month", "4")
                        .queryParam("selectedDate", "2026-04-28")
                        .with(SecurityMockMvcRequestPostProcessors.anonymous()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.year").value(2026))
                .andExpect(jsonPath("$.month").value(4));

        verify(calendarService).getMonthlyCalendar(2026, 4, LocalDate.of(2026, 4, 28));
    }

    @Test
    @DisplayName("selectedDate가 없으면 null로 서비스에 전달한다")
    void getMonthlyCalendar_withoutSelectedDate_passesNull() throws Exception {
        CalendarMonthResponse response = createResponse();

        when(calendarService.getMonthlyCalendar(2026, 4, null))
                .thenReturn(response);

        mockMvc.perform(get("/api/v1/calendar")
                        .queryParam("year", "2026")
                        .queryParam("month", "4"))
                .andExpect(status().isOk());

        verify(calendarService).getMonthlyCalendar(2026, 4, null);
    }

    @Test
    @DisplayName("응답 JSON은 캘린더 월별 일정 구조를 따른다")
    void getMonthlyCalendar_returnsExpectedJsonStructure() throws Exception {
        when(calendarService.getMonthlyCalendar(2026, 4, LocalDate.of(2026, 4, 28)))
                .thenReturn(createResponse());

        mockMvc.perform(get("/api/v1/calendar")
                        .queryParam("year", "2026")
                        .queryParam("month", "4")
                        .queryParam("selectedDate", "2026-04-28"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.year").value(2026))
                .andExpect(jsonPath("$.month").value(4))
                .andExpect(jsonPath("$.calendarCells").isArray())
                .andExpect(jsonPath("$.calendarCells[0].date").value("2026-04-27"))
                .andExpect(jsonPath("$.calendarCells[0].inCurrentMonth").value(true))
                .andExpect(jsonPath("$.calendarCells[0].items").isArray())
                .andExpect(jsonPath("$.calendarCells[0].items[0].ipoId").value(101))
                .andExpect(jsonPath("$.calendarCells[0].items[0].companyName").value("AIPO"))
                .andExpect(jsonPath("$.calendarCells[0].items[0].scheduleType").value("SUBSCRIPTION_START"))
                .andExpect(jsonPath("$.selectedDateSection").isMap())
                .andExpect(jsonPath("$.selectedDateSection.selectedDate").value("2026-04-28"))
                .andExpect(jsonPath("$.selectedDateSection.companies").isArray())
                .andExpect(jsonPath("$.selectedDateSection.companies[0].ipoId").value(101))
                .andExpect(jsonPath("$.selectedDateSection.companies[0].securitiesCompanyName").value("미래에셋증권"))
                .andExpect(jsonPath("$.selectedDateSection.companies[0].attractionScore").value(88))
                .andExpect(jsonPath("$.selectedDateSection.companies[0].scheduleLabel").value("청약 시작"));

        verify(calendarService).getMonthlyCalendar(2026, 4, LocalDate.of(2026, 4, 28));
    }

    @Test
    @DisplayName("selectedDate가 조회 월 밖이면 공통 예외 응답을 반환한다")
    void getMonthlyCalendar_whenSelectedDateOutOfMonth_returnsBadRequest() throws Exception {
        when(calendarService.getMonthlyCalendar(2026, 4, LocalDate.of(2026, 5, 1)))
                .thenThrow(new IllegalArgumentException("selectedDate must be within the requested year and month."));

        mockMvc.perform(get("/api/v1/calendar")
                        .queryParam("year", "2026")
                        .queryParam("month", "4")
                        .queryParam("selectedDate", "2026-05-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("selectedDate must be within the requested year and month."));
    }

    private CalendarMonthResponse createResponse() {
        return new CalendarMonthResponse(
                2026,
                4,
                List.of(
                        new CalendarCellItem(
                                LocalDate.of(2026, 4, 27),
                                true,
                                List.of(
                                        new CalendarCellScheduleItem(
                                                101L,
                                                "AIPO",
                                                "SUBSCRIPTION_START",
                                                "청약 시작"
                                        )
                                )
                        )
                ),
                new SelectedDateSection(
                        LocalDate.of(2026, 4, 28),
                        List.of(
                                new SelectedDateCompanyItem(
                                        101L,
                                        "AIPO",
                                        "미래에셋증권",
                                        new BigDecimal("88"),
                                        "SUBSCRIPTION_START",
                                        "청약 시작"
                                )
                        )
                )
        );
    }
}
