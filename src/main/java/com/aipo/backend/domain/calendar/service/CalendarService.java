package com.aipo.backend.domain.calendar.service;

import com.aipo.backend.domain.calendar.dto.CalendarCellItem;
import com.aipo.backend.domain.calendar.dto.CalendarCellScheduleItem;
import com.aipo.backend.domain.calendar.dto.CalendarMonthResponse;
import com.aipo.backend.domain.calendar.dto.SelectedDateCompanyItem;
import com.aipo.backend.domain.calendar.dto.SelectedDateSection;
import com.aipo.backend.domain.investmentprofile.entity.InvestmentProfileType;
import com.aipo.backend.domain.investmentprofile.repository.UserInvestmentProfileResultRepository;
import com.aipo.backend.domain.ipo.entity.ScheduleType;
import com.aipo.backend.domain.ipo.repository.AttractivenessIpoProjection;
import com.aipo.backend.domain.ipo.repository.CalendarIpoProjection;
import com.aipo.backend.domain.ipo.repository.IpoStockRepository;
import com.aipo.backend.domain.ipo.service.AttractivenessService;
import com.aipo.backend.domain.ipo.service.IpoStockViewMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
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
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CalendarService {

    private final IpoStockRepository ipoStockRepository;
    private final UserInvestmentProfileResultRepository userInvestmentProfileResultRepository;
    private final AttractivenessService attractivenessService;

    public CalendarMonthResponse getMonthlyCalendar(int year, int month, LocalDate selectedDate, Long userId) {
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate monthStart = yearMonth.atDay(1);
        LocalDate monthEnd = yearMonth.atEndOfMonth();
        List<CalendarSchedule> schedules = buildSchedulesFromIpoMain(monthStart, monthEnd);

        Map<LocalDate, List<CalendarSchedule>> schedulesByDate = schedules.stream()
                .collect(LinkedHashMap::new,
                        (map, schedule) -> map.computeIfAbsent(schedule.scheduleDate(), key -> new ArrayList<>()).add(schedule),
                        LinkedHashMap::putAll);

        LocalDate resolvedSelectedDate = resolveSelectedDate(yearMonth, selectedDate, schedulesByDate.keySet());

        return new CalendarMonthResponse(
                year,
                month,
                buildCalendarCells(yearMonth, schedulesByDate),
                buildSelectedDateSection(
                        resolvedSelectedDate,
                        schedulesByDate.getOrDefault(resolvedSelectedDate, Collections.emptyList()),
                        userId
                )
        );
    }

    LocalDate getToday() {
        return LocalDate.now();
    }

    private List<CalendarCellItem> buildCalendarCells(
            YearMonth yearMonth,
            Map<LocalDate, List<CalendarSchedule>> schedulesByDate
    ) {
        LocalDate gridStart = yearMonth.atDay(1).with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY));
        LocalDate gridEnd = yearMonth.atEndOfMonth().with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY));
        List<CalendarCellItem> cells = new ArrayList<>();

        for (LocalDate date = gridStart; !date.isAfter(gridEnd); date = date.plusDays(1)) {
            List<CalendarCellScheduleItem> items = schedulesByDate.getOrDefault(date, Collections.emptyList()).stream()
                    .sorted(Comparator
                            .comparing((CalendarSchedule schedule) -> schedule.stock().stockId())
                            .thenComparing(CalendarSchedule::scheduleType))
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

    private SelectedDateSection buildSelectedDateSection(
            LocalDate selectedDate,
            List<CalendarSchedule> schedules,
            Long userId
    ) {
        List<Long> stockIds = schedules.stream()
                .map(schedule -> schedule.stock().stockId())
                .distinct()
                .toList();
        Map<Long, String> leadManagerMap = buildLeadManagerMap(stockIds);
        Map<Long, Integer> scoreByStockId = calculateSelectedScores(schedules, currentProfileType(userId));

        List<SelectedDateCompanyItem> companies = schedules.stream()
                .sorted(Comparator
                        .comparing((CalendarSchedule schedule) -> schedule.stock().stockId())
                        .thenComparing(CalendarSchedule::scheduleType))
                .map(schedule -> toSelectedDateCompanyItem(schedule, leadManagerMap, scoreByStockId))
                .toList();

        return new SelectedDateSection(selectedDate, companies);
    }

    private CalendarCellScheduleItem toCalendarCellScheduleItem(CalendarSchedule schedule) {
        return new CalendarCellScheduleItem(
                schedule.stock().stockId(),
                schedule.stock().displayName(),
                schedule.scheduleType().name(),
                getScheduleLabel(schedule.scheduleType())
        );
    }

    private SelectedDateCompanyItem toSelectedDateCompanyItem(
            CalendarSchedule schedule,
            Map<Long, String> leadManagerMap,
            Map<Long, Integer> scoreByStockId
    ) {
        Long stockId = schedule.stock().stockId();

        return new SelectedDateCompanyItem(
                stockId,
                schedule.stock().displayName(),
                leadManagerMap.getOrDefault(stockId, "-"),
                toAttractionScore(scoreByStockId.get(stockId)),
                schedule.scheduleType().name(),
                getScheduleLabel(schedule.scheduleType())
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

    private Map<Long, String> buildLeadManagerMap(List<Long> stockIds) {
        if (stockIds.isEmpty()) {
            return Map.of();
        }

        return ipoStockRepository.findUnderwritersByStockIds(stockIds)
                .entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> firstUnderwriter(entry.getValue())));
    }

    private String firstUnderwriter(String underwriter) {
        if (underwriter == null || underwriter.isBlank()) {
            return "-";
        }

        for (String name : underwriter.split(",")) {
            String trimmed = name.trim();
            if (hasTextValue(trimmed)) {
                return trimmed;
            }
        }
        return "-";
    }

    private boolean hasTextValue(String value) {
        return value != null && !value.isBlank() && !"-".equals(value.trim());
    }

    private BigDecimal toAttractionScore(Integer attractScore) {
        return attractScore == null ? null : BigDecimal.valueOf(attractScore);
    }

    private InvestmentProfileType currentProfileType(Long userId) {
        if (userId == null) {
            return null;
        }
        return userInvestmentProfileResultRepository
                .findTopByUserIdAndCurrentTrueOrderByCreatedAtDescIdDesc(userId)
                .map(result -> result.getProfileType())
                .orElse(null);
    }

    private Map<Long, Integer> calculateSelectedScores(
            List<CalendarSchedule> schedules,
            InvestmentProfileType currentProfileType
    ) {
        if (schedules.isEmpty()) {
            return Map.of();
        }

        List<CalendarStock> selectedStocks = schedules.stream()
                .map(CalendarSchedule::stock)
                .collect(Collectors.toMap(
                        CalendarStock::stockId,
                        Function.identity(),
                        (existing, replacement) -> existing
                ))
                .values()
                .stream()
                .toList();

        List<AttractivenessIpoProjection> allIpos;
        try {
            allIpos = ipoStockRepository.findAllForAttractiveness();
        } catch (DataAccessException exception) {
            allIpos = selectedStocks.stream()
                    .map(this::fallbackAttractivenessProjection)
                    .toList();
        }

        if (allIpos == null || allIpos.isEmpty()) {
            allIpos = selectedStocks.stream()
                    .map(this::fallbackAttractivenessProjection)
                    .toList();
        }

        Map<Long, AttractivenessIpoProjection> projectionByStockId = allIpos.stream()
                .collect(Collectors.toMap(
                        AttractivenessIpoProjection::getStockId,
                        Function.identity(),
                        (existing, replacement) -> existing
                ));
        List<AttractivenessIpoProjection> finalAllIpos = allIpos;

        return selectedStocks.stream()
                .collect(Collectors.toMap(
                        CalendarStock::stockId,
                        stock -> {
                            AttractivenessIpoProjection target = projectionByStockId.getOrDefault(
                                    stock.stockId(),
                                    fallbackAttractivenessProjection(stock)
                            );
                            return attractivenessService
                                    .calculateForIpo(target, finalAllIpos, currentProfileType)
                                    .selected()
                                    .score();
                        },
                        (existing, replacement) -> existing
                ));
    }

    private AttractivenessIpoProjection fallbackAttractivenessProjection(CalendarStock stock) {
        return new SimpleAttractivenessIpoProjection(
                stock.stockId(),
                stock.displayName(),
                null,
                null,
                null,
                null
        );
    }

    private List<CalendarSchedule> buildSchedulesFromIpoMain(LocalDate monthStart, LocalDate monthEnd) {
        List<CalendarSchedule> schedules = new ArrayList<>();
        for (CalendarIpoProjection projection : ipoStockRepository.findAllForCalendar()) {
            CalendarStock stock = CalendarStock.from(projection);
            addSchedule(schedules, stock, ScheduleType.DEMAND_FORECAST_START,
                    IpoStockViewMapper.parseDateText(stock.demandForecastDate(), 0), monthStart, monthEnd);
            addSchedule(schedules, stock, ScheduleType.DEMAND_FORECAST_END,
                    IpoStockViewMapper.parseDateText(stock.demandForecastDate(), 1), monthStart, monthEnd);
            addSchedule(schedules, stock, ScheduleType.SUBSCRIPTION_START,
                    stock.subscriptionStartDate(), monthStart, monthEnd);
            addSchedule(schedules, stock, ScheduleType.SUBSCRIPTION_END,
                    stock.subscriptionEndDate(), monthStart, monthEnd);
            addSchedule(schedules, stock, ScheduleType.REFUND,
                    IpoStockViewMapper.parseDateText(stock.refundDate(), 0), monthStart, monthEnd);
            addSchedule(schedules, stock, ScheduleType.LISTING,
                    stock.listingDate(), monthStart, monthEnd);
        }
        schedules.sort(Comparator
                .comparing(CalendarSchedule::scheduleDate)
                .thenComparing(schedule -> schedule.stock().stockId())
                .thenComparing(CalendarSchedule::scheduleType));
        return schedules;
    }

    private void addSchedule(
            List<CalendarSchedule> schedules,
            CalendarStock stock,
            ScheduleType scheduleType,
            LocalDate scheduleDate,
            LocalDate monthStart,
            LocalDate monthEnd
    ) {
        if (scheduleDate == null || scheduleDate.isBefore(monthStart) || scheduleDate.isAfter(monthEnd)) {
            return;
        }
        schedules.add(new CalendarSchedule(stock, scheduleType, scheduleDate));
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

    private record CalendarSchedule(
            CalendarStock stock,
            ScheduleType scheduleType,
            LocalDate scheduleDate
    ) {
    }

    private record CalendarStock(
            Long stockId,
            String corpName,
            String stockCode,
            Float attractScore,
            String demandForecastDate,
            String subscriptionDate,
            String refundDate,
            LocalDate listingDate
    ) {

        private static CalendarStock from(CalendarIpoProjection projection) {
            return new CalendarStock(
                    projection.getStockId(),
                    projection.getCorpName(),
                    projection.getStockCode(),
                    projection.getAttractScore(),
                    projection.getDemandForecastDate(),
                    projection.getSubscriptionDate(),
                    projection.getRefundDate(),
                    projection.getListingDate()
            );
        }

        private String displayName() {
            if (corpName != null && !corpName.isBlank()) {
                return corpName.trim();
            }
            return stockCode == null ? "" : stockCode.trim();
        }

        private LocalDate subscriptionStartDate() {
            return IpoStockViewMapper.parseSubscriptionDateText(subscriptionDate, 0);
        }

        private LocalDate subscriptionEndDate() {
            LocalDate endDate = IpoStockViewMapper.parseSubscriptionDateText(subscriptionDate, 1);
            return endDate != null ? endDate : subscriptionStartDate();
        }
    }

    private record SimpleAttractivenessIpoProjection(
            Long stockId,
            String corpName,
            String competitionRatio,
            String instCommitmentRatio,
            String floatingStockRatio,
            String lockupTotalRatio
    ) implements AttractivenessIpoProjection {

        @Override
        public Long getStockId() {
            return stockId;
        }

        @Override
        public String getCorpName() {
            return corpName;
        }

        @Override
        public String getCompetitionRatio() {
            return competitionRatio;
        }

        @Override
        public String getInstCommitmentRatio() {
            return instCommitmentRatio;
        }

        @Override
        public String getFloatingStockRatio() {
            return floatingStockRatio;
        }

        @Override
        public String getLockupTotalRatio() {
            return lockupTotalRatio;
        }
    }
}
