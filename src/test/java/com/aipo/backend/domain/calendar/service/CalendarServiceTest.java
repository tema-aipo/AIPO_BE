package com.aipo.backend.domain.calendar.service;

import com.aipo.backend.domain.calendar.dto.CalendarMonthResponse;
import com.aipo.backend.domain.investmentprofile.repository.UserInvestmentProfileResultRepository;
import com.aipo.backend.domain.ipo.dto.AttractivenessResponse;
import com.aipo.backend.domain.ipo.dto.ProfileAttractivenessScore;
import com.aipo.backend.domain.ipo.entity.IpoStock;
import com.aipo.backend.domain.ipo.repository.AttractivenessIpoProjection;
import com.aipo.backend.domain.ipo.repository.CalendarIpoProjection;
import com.aipo.backend.domain.ipo.repository.IpoStockRepository;
import com.aipo.backend.domain.ipo.service.AttractivenessService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Constructor;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CalendarServiceTest {

    @Mock
    private IpoStockRepository ipoStockRepository;
    @Mock
    private UserInvestmentProfileResultRepository userInvestmentProfileResultRepository;

    @Mock
    private AttractivenessService attractivenessService;

    @Test
    @DisplayName("?????쐭筌띻낯???袁⑷퍥 ?醫롮? ?????醫뤾문 ?醫롮? ?怨멸쉭 筌뤴뫖以??鈺곌퀡???뺣뼄")
    void getMonthlyCalendar_success() {
        CalendarService calendarService = spy(new CalendarService(
                ipoStockRepository,
                userInvestmentProfileResultRepository,
                attractivenessService
        ));

        Long ipoId = 1L;
        IpoStock stock = ipoStock(ipoId, "AIPO");
        ReflectionTestUtils.setField(stock, "attractScore", 88F);
        ReflectionTestUtils.setField(stock, "subscriptionDate", "2026.04.28 ~ 04.29");
        ReflectionTestUtils.setField(stock, "refundDate", "2026.04.30");

        when(ipoStockRepository.findAllForCalendar()).thenReturn(List.of(calendarProjection(stock)));        when(ipoStockRepository.findUnderwritersByStockIds(List.of(ipoId))).thenReturn(Map.of(ipoId, "LeadA, LeadB"));
        when(attractivenessService.calculateForIpo(any(AttractivenessIpoProjection.class), anyList(), isNull()))
                .thenReturn(attractiveness(77));
        CalendarMonthResponse response = calendarService.getMonthlyCalendar(
                2026,
                4,
                LocalDate.of(2026, 4, 28),
                null
        );

        assertThat(response.year()).isEqualTo(2026);
        assertThat(response.month()).isEqualTo(4);
        assertThat(response.calendarCells()).hasSize(35);
        assertThat(response.calendarCells().get(0).date()).isEqualTo(LocalDate.of(2026, 3, 29));
        assertThat(response.calendarCells().get(0).inCurrentMonth()).isFalse();
        assertThat(response.calendarCells().get(30).date()).isEqualTo(LocalDate.of(2026, 4, 28));
        assertThat(response.calendarCells().get(30).items()).hasSize(1);
        assertThat(response.calendarCells().get(30).items().get(0).ipoId()).isEqualTo(ipoId);
        assertThat(response.calendarCells().get(30).items().get(0).companyName()).isEqualTo("AIPO");
        assertThat(response.calendarCells().get(30).items().get(0).scheduleType()).isEqualTo("SUBSCRIPTION_START");
        assertThat(response.calendarCells().get(30).items().get(0).scheduleLabel()).isNotBlank();
        assertThat(response.selectedDateSection().selectedDate()).isEqualTo(LocalDate.of(2026, 4, 28));
        assertThat(response.selectedDateSection().companies()).hasSize(1);
        assertThat(response.selectedDateSection().companies().get(0).ipoId()).isEqualTo(ipoId);
        assertThat(response.selectedDateSection().companies().get(0).securitiesCompanyName()).isEqualTo("LeadA");
        assertThat(response.selectedDateSection().companies().get(0).attractionScore()).isEqualByComparingTo("77");
        assertThat(response.selectedDateSection().companies().get(0).scheduleLabel()).isNotBlank();
    }

    @Test
    @DisplayName("???????ル‘? ??????ルㅎ臾???湲곌컙 以묎컙???쇱젙???ы븿?쒕떎")
    void getMonthlyCalendar_selectedDateSectionIncludesScheduleRangeMiddleDate() {
        CalendarService calendarService = spy(new CalendarService(
                ipoStockRepository,
                userInvestmentProfileResultRepository,
                attractivenessService
        ));

        Long ipoId = 1L;
        IpoStock stock = ipoStock(ipoId, "AIPO");
        ReflectionTestUtils.setField(stock, "subscriptionDate", "2026.04.28 ~ 04.30");

        when(ipoStockRepository.findAllForCalendar()).thenReturn(List.of(calendarProjection(stock)));
        when(ipoStockRepository.findUnderwritersByStockIds(List.of(ipoId))).thenReturn(Map.of(ipoId, "LeadA"));
        when(attractivenessService.calculateForIpo(any(AttractivenessIpoProjection.class), anyList(), isNull()))
                .thenReturn(attractiveness(77));

        CalendarMonthResponse response = calendarService.getMonthlyCalendar(
                2026,
                4,
                LocalDate.of(2026, 4, 29),
                null
        );

        assertThat(response.calendarCells().get(31).date()).isEqualTo(LocalDate.of(2026, 4, 29));
        assertThat(response.calendarCells().get(31).items()).isEmpty();
        assertThat(response.selectedDateSection().selectedDate()).isEqualTo(LocalDate.of(2026, 4, 29));
        assertThat(response.selectedDateSection().companies()).hasSize(1);
        assertThat(response.selectedDateSection().companies().get(0).ipoId()).isEqualTo(ipoId);
        assertThat(response.selectedDateSection().companies().get(0).scheduleType()).isEqualTo("SUBSCRIPTION_START");
        assertThat(response.selectedDateSection().companies().get(0).scheduleLabel()).isEqualTo("\uCCAD\uC57D \uAE30\uAC04");
    }

    @Test
    @DisplayName("selectedDate揶쎛 ??얩?鈺곌퀬???遺우뵠 ?袁⑹삺 ?遺우뵠筌???삳뮎 ?醫롮???疫꿸퀡???醫뤾문??뺣뼄")
    void getMonthlyCalendar_whenSelectedDateMissingInCurrentMonth_defaultsToToday() {
        CalendarService calendarService = spy(new CalendarService(
                ipoStockRepository,
                userInvestmentProfileResultRepository,
                attractivenessService
        ));

        IpoStock stock = ipoStock(1L, "AIPO");
        ReflectionTestUtils.setField(stock, "listingDate", LocalDate.of(2026, 4, 30));

        when(calendarService.getToday()).thenReturn(LocalDate.of(2026, 4, 21));
        when(ipoStockRepository.findAllForCalendar()).thenReturn(List.of(calendarProjection(stock)));

        CalendarMonthResponse response = calendarService.getMonthlyCalendar(2026, 4, null, null);

        assertThat(response.selectedDateSection().selectedDate()).isEqualTo(LocalDate.of(2026, 4, 21));
        assertThat(response.selectedDateSection().companies()).isEmpty();
    }

    @Test
    @DisplayName("selectedDate揶쎛 ??얩??袁⑹삺 ?遺우뵠 ?袁⑤빍筌?筌???깆젟 ?醫롮???疫꿸퀡???醫뤾문??뺣뼄")
    void getMonthlyCalendar_whenSelectedDateMissingOutsideCurrentMonth_defaultsToFirstScheduledDate() {
        CalendarService calendarService = spy(new CalendarService(
                ipoStockRepository,
                userInvestmentProfileResultRepository,
                attractivenessService
        ));

        IpoStock firstStock = ipoStock(1L, "AIPO");
        IpoStock secondStock = ipoStock(2L, "BIPO");
        ReflectionTestUtils.setField(firstStock, "demandForecastDate", "2026.05.10 ~ 05.11");
        ReflectionTestUtils.setField(secondStock, "subscriptionDate", "2026.05.12 ~ 05.13");

        when(calendarService.getToday()).thenReturn(LocalDate.of(2026, 4, 21));
        when(ipoStockRepository.findAllForCalendar()).thenReturn(List.of(
                calendarProjection(firstStock),
                calendarProjection(secondStock)
        ));        when(ipoStockRepository.findUnderwritersByStockIds(List.of(1L))).thenReturn(Map.of());
        when(attractivenessService.calculateForIpo(any(AttractivenessIpoProjection.class), anyList(), isNull()))
                .thenReturn(attractiveness(61));

        CalendarMonthResponse response = calendarService.getMonthlyCalendar(2026, 5, null, null);

        assertThat(response.selectedDateSection().selectedDate()).isEqualTo(LocalDate.of(2026, 5, 10));
        assertThat(response.selectedDateSection().companies()).hasSize(1);
        assertThat(response.selectedDateSection().companies().get(0).companyName()).isEqualTo("AIPO");
    }

    @Test
    @DisplayName("??깆젟????용뮉 ??? ??????1??깆뱽 疫꿸퀡???醫뤾문??뺣뼄")
    void getMonthlyCalendar_whenNoSchedules_defaultsToFirstDayOfMonth() {
        CalendarService calendarService = spy(new CalendarService(
                ipoStockRepository,
                userInvestmentProfileResultRepository,
                attractivenessService
        ));

        when(calendarService.getToday()).thenReturn(LocalDate.of(2026, 4, 21));
        when(ipoStockRepository.findAllForCalendar()).thenReturn(List.of());

        CalendarMonthResponse response = calendarService.getMonthlyCalendar(2026, 6, null, null);

        assertThat(response.calendarCells()).hasSize(35);
        assertThat(response.selectedDateSection().selectedDate()).isEqualTo(LocalDate.of(2026, 6, 1));
        assertThat(response.selectedDateSection().companies()).isEmpty();
    }

    @Test
    @DisplayName("selectedDate揶쎛 鈺곌퀬????獄쏅쉼?좑쭖???됱뇚揶쎛 獄쏆뮇源??뺣뼄")
    void getMonthlyCalendar_whenSelectedDateOutOfMonth_throwIllegalArgumentException() {
        CalendarService calendarService = new CalendarService(
                ipoStockRepository,
                userInvestmentProfileResultRepository,
                attractivenessService
        );

        assertThatThrownBy(() -> calendarService.getMonthlyCalendar(2026, 4, LocalDate.of(2026, 5, 1), null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("selectedDate must be within the requested year and month.");
    }

    private AttractivenessResponse attractiveness(int score) {
        ProfileAttractivenessScore selected = new ProfileAttractivenessScore(score, "grade", "reason");
        return new AttractivenessResponse(selected, null, selected, selected, selected, selected, null, null);
    }

    private IpoStock ipoStock(Long id, String companyName) {
        IpoStock ipoStock = instantiate(IpoStock.class);
        ReflectionTestUtils.setField(ipoStock, "id", id);
        ReflectionTestUtils.setField(ipoStock, "corpName", companyName);
        ReflectionTestUtils.setField(ipoStock, "createdAt", LocalDateTime.now());
        ReflectionTestUtils.setField(ipoStock, "updatedAt", LocalDateTime.now());
        return ipoStock;
    }

    private CalendarIpoProjection calendarProjection(IpoStock stock) {
        return new TestCalendarIpoProjection(
                stock.getId(),
                stock.getCorpName(),
                stock.getStockCode(),
                stock.getAttractScore(),
                stock.getDemandForecastDate(),
                stock.getSubscriptionDate(),
                stock.getRefundDate(),
                stock.getListingDate()
        );
    }

    private <T> T instantiate(Class<T> type) {
        try {
            Constructor<T> constructor = type.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to instantiate " + type.getSimpleName(), exception);
        }
    }

    private record TestCalendarIpoProjection(
            Long stockId,
            String corpName,
            String stockCode,
            Float attractScore,
            String demandForecastDate,
            String subscriptionDate,
            String refundDate,
            LocalDate listingDate
    ) implements CalendarIpoProjection {

        @Override
        public Long getStockId() {
            return stockId;
        }

        @Override
        public String getCorpName() {
            return corpName;
        }

        @Override
        public String getStockCode() {
            return stockCode;
        }

        @Override
        public Float getAttractScore() {
            return attractScore;
        }

        @Override
        public String getDemandForecastDate() {
            return demandForecastDate;
        }

        @Override
        public String getSubscriptionDate() {
            return subscriptionDate;
        }

        @Override
        public String getRefundDate() {
            return refundDate;
        }

        @Override
        public LocalDate getListingDate() {
            return listingDate;
        }
    }
}
