package com.aipo.backend.domain.calendar.service;

import com.aipo.backend.domain.calendar.dto.CalendarCellItem;
import com.aipo.backend.domain.calendar.dto.CalendarCellScheduleItem;
import com.aipo.backend.domain.calendar.dto.CalendarMonthResponse;
import com.aipo.backend.domain.calendar.dto.SelectedDateCompanyItem;
import com.aipo.backend.domain.calendar.dto.SelectedDateSection;
import com.aipo.backend.domain.ipo.entity.IpoLeadManager;
import com.aipo.backend.domain.ipo.entity.IpoSchedule;
import com.aipo.backend.domain.ipo.entity.ScheduleType;
import com.aipo.backend.domain.ipo.repository.IpoLeadManagerRepository;
import com.aipo.backend.domain.ipo.repository.IpoScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CalendarService {

    private final IpoScheduleRepository ipoScheduleRepository;
    private final IpoLeadManagerRepository ipoLeadManagerRepository;

    public CalendarMonthResponse getMonthlyCalendar(int year, int month, LocalDate selectedDate) {
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate monthStart = yearMonth.atDay(1);
        LocalDate monthEnd = yearMonth.atEndOfMonth();
        List<IpoSchedule> schedules = ipoScheduleRepository
                .findAllByScheduleDateBetweenOrderByScheduleDateAsc(monthStart, monthEnd);

        Map<LocalDate, List<IpoSchedule>> schedulesByDate = schedules.stream()
                .collect(LinkedHashMap::new,
                        (map, schedule) -> map.computeIfAbsent(schedule.getScheduleDate(), key -> new ArrayList<>()).add(schedule),
                        LinkedHashMap::putAll);

        LocalDate resolvedSelectedDate = resolveSelectedDate(yearMonth, selectedDate, schedulesByDate.keySet());

        return new CalendarMonthResponse(
                year,
                month,
                buildCalendarCells(yearMonth, schedulesByDate),
                buildSelectedDateSection(resolvedSelectedDate, schedulesByDate.getOrDefault(resolvedSelectedDate, Collections.emptyList()))
        );
    }

    LocalDate getToday() {
        return LocalDate.now();
    }

    private List<CalendarCellItem> buildCalendarCells(
            YearMonth yearMonth,
            Map<LocalDate, List<IpoSchedule>> schedulesByDate
    ) {
        LocalDate gridStart = yearMonth.atDay(1).with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY));
        LocalDate gridEnd = yearMonth.atEndOfMonth().with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY));
        List<CalendarCellItem> cells = new ArrayList<>();

        for (LocalDate date = gridStart; !date.isAfter(gridEnd); date = date.plusDays(1)) {
            List<CalendarCellScheduleItem> items = schedulesByDate.getOrDefault(date, Collections.emptyList()).stream()
                    .sorted(Comparator
                            .comparing((IpoSchedule schedule) -> schedule.getStock().getId())
                            .thenComparing(IpoSchedule::getScheduleType))
                    .map(this::toCalendarCellScheduleItem)
                    .toList();

            cells.add(new CalendarCellItem(
                    date,
                    date.getYear() == yearMonth.getYear() && date.getMonthValue() == yearMonth.getMonthValue(),
                    items
            ));
        }

        return cells;
    }

    private SelectedDateSection buildSelectedDateSection(LocalDate selectedDate, List<IpoSchedule> schedules) {
        List<SelectedDateCompanyItem> companies = schedules.stream()
                .sorted(Comparator
                        .comparing((IpoSchedule schedule) -> schedule.getStock().getId())
                        .thenComparing(IpoSchedule::getScheduleType))
                .map(this::toSelectedDateCompanyItem)
                .toList();

        return new SelectedDateSection(selectedDate, companies);
    }

    private CalendarCellScheduleItem toCalendarCellScheduleItem(IpoSchedule schedule) {
        return new CalendarCellScheduleItem(
                schedule.getStock().getId(),
                schedule.getStock().getCompanyName(),
                schedule.getScheduleType().name(),
                getScheduleLabel(schedule.getScheduleType())
        );
    }

    private SelectedDateCompanyItem toSelectedDateCompanyItem(IpoSchedule schedule) {
        Long stockId = schedule.getStock().getId();

        return new SelectedDateCompanyItem(
                stockId,
                schedule.getStock().getCompanyName(),
                getRepresentativeSecuritiesCompanyName(stockId),
                toAttractionScore(schedule.getStock().getAttractScore()),
                schedule.getScheduleType().name(),
                getScheduleLabel(schedule.getScheduleType())
        );
    }

    private LocalDate resolveSelectedDate(YearMonth yearMonth, LocalDate selectedDate, Iterable<LocalDate> scheduledDates) {
        if (selectedDate != null) {
            validateSelectedDate(yearMonth, selectedDate);
            return selectedDate;
        }

        LocalDate today = getToday();
        if (YearMonth.from(today).equals(yearMonth)) {
            return today;
        }

        LocalDate firstScheduledDate = null;
        for (LocalDate scheduledDate : scheduledDates) {
            firstScheduledDate = scheduledDate;
            break;
        }

        if (firstScheduledDate != null) {
            return firstScheduledDate;
        }

        return yearMonth.atDay(1);
    }

    private void validateSelectedDate(YearMonth yearMonth, LocalDate selectedDate) {
        if (!YearMonth.from(selectedDate).equals(yearMonth)) {
            throw new IllegalArgumentException("selectedDate must be within the requested year and month.");
        }
    }

    private String getRepresentativeSecuritiesCompanyName(Long stockId) {
        return ipoLeadManagerRepository.findAllByStock_IdOrderByDisplayOrderAsc(stockId).stream()
                .findFirst()
                .map(IpoLeadManager::getManagerName)
                .orElse(null);
    }

    private BigDecimal toAttractionScore(Float attractScore) {
        return attractScore == null ? null : BigDecimal.valueOf(attractScore);
    }

    private String getScheduleLabel(ScheduleType scheduleType) {
        return switch (scheduleType) {
            case DEMAND_FORECAST_START -> "수요예측 시작";
            case DEMAND_FORECAST_END -> "수요예측 종료";
            case SUBSCRIPTION_START -> "청약 시작";
            case SUBSCRIPTION_END -> "청약 종료";
            case REFUND -> "환불일";
            case LISTING -> "상장일";
        };
    }
}
