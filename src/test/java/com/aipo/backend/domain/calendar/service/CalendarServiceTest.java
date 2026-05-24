package com.aipo.backend.domain.calendar.service;

import com.aipo.backend.domain.calendar.dto.CalendarMonthResponse;
import com.aipo.backend.domain.investmentprofile.repository.UserInvestmentProfileResultRepository;
import com.aipo.backend.domain.ipo.dto.AttractivenessResponse;
import com.aipo.backend.domain.ipo.dto.ProfileAttractivenessScore;
import com.aipo.backend.domain.ipo.entity.IpoLeadManager;
import com.aipo.backend.domain.ipo.entity.IpoStock;
import com.aipo.backend.domain.ipo.repository.AttractivenessIpoProjection;
import com.aipo.backend.domain.ipo.repository.IpoLeadManagerRepository;
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
    private IpoLeadManagerRepository ipoLeadManagerRepository;

    @Mock
    private UserInvestmentProfileResultRepository userInvestmentProfileResultRepository;

    @Mock
    private AttractivenessService attractivenessService;

    @Test
    @DisplayName("월 렌더링용 전체 날짜 셀과 선택 날짜 상세 목록을 조립한다")
    void getMonthlyCalendar_success() {
        CalendarService calendarService = spy(new CalendarService(
                ipoStockRepository,
                ipoLeadManagerRepository,
                userInvestmentProfileResultRepository,
                attractivenessService
        ));

        Long ipoId = 1L;
        IpoStock stock = ipoStock(ipoId, "AIPO");
        ReflectionTestUtils.setField(stock, "attractScore", 88F);
        ReflectionTestUtils.setField(stock, "subscriptionDate", "2026.04.28 ~ 04.29");
        ReflectionTestUtils.setField(stock, "refundDate", "2026.04.30");

        when(ipoStockRepository.findAll()).thenReturn(List.of(stock));
        when(ipoLeadManagerRepository.findAllByStock_IdIn(List.of(ipoId))).thenReturn(List.of(
                leadManager(stock, "주관사A", 1),
                leadManager(stock, "주관사B", 2)
        ));
        when(ipoStockRepository.findUnderwritersByStockIds(List.of(ipoId))).thenReturn(Map.of());
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
        assertThat(response.calendarCells().get(30).items().get(0).scheduleLabel()).isEqualTo("청약 시작");
        assertThat(response.selectedDateSection().selectedDate()).isEqualTo(LocalDate.of(2026, 4, 28));
        assertThat(response.selectedDateSection().companies()).hasSize(1);
        assertThat(response.selectedDateSection().companies().get(0).ipoId()).isEqualTo(ipoId);
        assertThat(response.selectedDateSection().companies().get(0).securitiesCompanyName()).isEqualTo("주관사A");
        assertThat(response.selectedDateSection().companies().get(0).attractionScore()).isEqualByComparingTo("77");
        assertThat(response.selectedDateSection().companies().get(0).scheduleLabel()).isEqualTo("청약 시작");
    }

    @Test
    @DisplayName("selectedDate가 없고 조회 월이 현재 월이면 오늘 날짜를 기본 선택한다")
    void getMonthlyCalendar_whenSelectedDateMissingInCurrentMonth_defaultsToToday() {
        CalendarService calendarService = spy(new CalendarService(
                ipoStockRepository,
                ipoLeadManagerRepository,
                userInvestmentProfileResultRepository,
                attractivenessService
        ));

        IpoStock stock = ipoStock(1L, "AIPO");
        ReflectionTestUtils.setField(stock, "listingDate", LocalDate.of(2026, 4, 30));

        when(calendarService.getToday()).thenReturn(LocalDate.of(2026, 4, 21));
        when(ipoStockRepository.findAll()).thenReturn(List.of(stock));

        CalendarMonthResponse response = calendarService.getMonthlyCalendar(2026, 4, null, null);

        assertThat(response.selectedDateSection().selectedDate()).isEqualTo(LocalDate.of(2026, 4, 21));
        assertThat(response.selectedDateSection().companies()).isEmpty();
    }

    @Test
    @DisplayName("selectedDate가 없고 현재 월이 아니면 첫 일정 날짜를 기본 선택한다")
    void getMonthlyCalendar_whenSelectedDateMissingOutsideCurrentMonth_defaultsToFirstScheduledDate() {
        CalendarService calendarService = spy(new CalendarService(
                ipoStockRepository,
                ipoLeadManagerRepository,
                userInvestmentProfileResultRepository,
                attractivenessService
        ));

        IpoStock firstStock = ipoStock(1L, "AIPO");
        IpoStock secondStock = ipoStock(2L, "BIPO");
        ReflectionTestUtils.setField(firstStock, "demandForecastDate", "2026.05.10 ~ 05.11");
        ReflectionTestUtils.setField(secondStock, "subscriptionDate", "2026.05.12 ~ 05.13");

        when(calendarService.getToday()).thenReturn(LocalDate.of(2026, 4, 21));
        when(ipoStockRepository.findAll()).thenReturn(List.of(firstStock, secondStock));
        when(ipoLeadManagerRepository.findAllByStock_IdIn(List.of(1L))).thenReturn(List.of());
        when(ipoStockRepository.findUnderwritersByStockIds(List.of(1L))).thenReturn(Map.of());
        when(attractivenessService.calculateForIpo(any(AttractivenessIpoProjection.class), anyList(), isNull()))
                .thenReturn(attractiveness(61));

        CalendarMonthResponse response = calendarService.getMonthlyCalendar(2026, 5, null, null);

        assertThat(response.selectedDateSection().selectedDate()).isEqualTo(LocalDate.of(2026, 5, 10));
        assertThat(response.selectedDateSection().companies()).hasSize(1);
        assertThat(response.selectedDateSection().companies().get(0).companyName()).isEqualTo("AIPO");
    }

    @Test
    @DisplayName("일정이 없는 달은 해당 월 1일을 기본 선택한다")
    void getMonthlyCalendar_whenNoSchedules_defaultsToFirstDayOfMonth() {
        CalendarService calendarService = spy(new CalendarService(
                ipoStockRepository,
                ipoLeadManagerRepository,
                userInvestmentProfileResultRepository,
                attractivenessService
        ));

        when(calendarService.getToday()).thenReturn(LocalDate.of(2026, 4, 21));
        when(ipoStockRepository.findAll()).thenReturn(List.of());

        CalendarMonthResponse response = calendarService.getMonthlyCalendar(2026, 6, null, null);

        assertThat(response.calendarCells()).hasSize(35);
        assertThat(response.selectedDateSection().selectedDate()).isEqualTo(LocalDate.of(2026, 6, 1));
        assertThat(response.selectedDateSection().companies()).isEmpty();
    }

    @Test
    @DisplayName("selectedDate가 조회 월 밖이면 예외가 발생한다")
    void getMonthlyCalendar_whenSelectedDateOutOfMonth_throwIllegalArgumentException() {
        CalendarService calendarService = new CalendarService(
                ipoStockRepository,
                ipoLeadManagerRepository,
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
        ReflectionTestUtils.setField(ipoStock, "companyName", companyName);
        ReflectionTestUtils.setField(ipoStock, "createdAt", LocalDateTime.now());
        ReflectionTestUtils.setField(ipoStock, "updatedAt", LocalDateTime.now());
        return ipoStock;
    }

    private IpoLeadManager leadManager(IpoStock stock, String managerName, int displayOrder) {
        IpoLeadManager leadManager = instantiate(IpoLeadManager.class);
        ReflectionTestUtils.setField(leadManager, "stock", stock);
        ReflectionTestUtils.setField(leadManager, "managerName", managerName);
        ReflectionTestUtils.setField(leadManager, "displayOrder", displayOrder);
        ReflectionTestUtils.setField(leadManager, "createdAt", LocalDateTime.now());
        return leadManager;
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
}
