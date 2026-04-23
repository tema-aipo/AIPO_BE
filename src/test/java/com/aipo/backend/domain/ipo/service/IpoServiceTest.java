package com.aipo.backend.domain.ipo.service;

import com.aipo.backend.domain.ipo.dto.CompetitionTab;
import com.aipo.backend.domain.ipo.dto.IpoDetailResponse;
import com.aipo.backend.domain.ipo.dto.IpoListItem;
import com.aipo.backend.domain.ipo.dto.IpoListResponse;
import com.aipo.backend.domain.ipo.entity.CompetitionTabType;
import com.aipo.backend.domain.ipo.entity.IpoAttractionReason;
import com.aipo.backend.domain.ipo.entity.IpoAttractionScore;
import com.aipo.backend.domain.ipo.entity.IpoDemandForecast;
import com.aipo.backend.domain.ipo.entity.IpoDepositInfo;
import com.aipo.backend.domain.ipo.entity.IpoLeadManager;
import com.aipo.backend.domain.ipo.entity.IpoOfferingInfo;
import com.aipo.backend.domain.ipo.entity.IpoSchedule;
import com.aipo.backend.domain.ipo.entity.IpoStock;
import com.aipo.backend.domain.ipo.entity.IpoSubscriptionCompetition;
import com.aipo.backend.domain.ipo.entity.ScheduleType;
import com.aipo.backend.domain.ipo.repository.IpoAttractionReasonRepository;
import com.aipo.backend.domain.ipo.repository.IpoAttractionScoreRepository;
import com.aipo.backend.domain.ipo.repository.IpoDemandForecastRepository;
import com.aipo.backend.domain.ipo.repository.IpoDepositInfoRepository;
import com.aipo.backend.domain.ipo.repository.IpoLeadManagerRepository;
import com.aipo.backend.domain.ipo.repository.IpoOfferingInfoRepository;
import com.aipo.backend.domain.ipo.repository.IpoScheduleRepository;
import com.aipo.backend.domain.ipo.repository.IpoStockRepository;
import com.aipo.backend.domain.ipo.repository.IpoSubscriptionCompetitionRepository;
import com.aipo.backend.domain.ipo.repository.UserFavoriteStockRepository;
import com.aipo.backend.global.exception.CustomException;
import com.aipo.backend.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Constructor;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IpoServiceTest {

    @Mock
    private IpoStockRepository ipoStockRepository;

    @Mock
    private IpoLeadManagerRepository ipoLeadManagerRepository;

    @Mock
    private IpoAttractionScoreRepository ipoAttractionScoreRepository;

    @Mock
    private IpoAttractionReasonRepository ipoAttractionReasonRepository;

    @Mock
    private IpoDemandForecastRepository ipoDemandForecastRepository;

    @Mock
    private IpoSubscriptionCompetitionRepository ipoSubscriptionCompetitionRepository;

    @Mock
    private IpoScheduleRepository ipoScheduleRepository;

    @Mock
    private IpoDepositInfoRepository ipoDepositInfoRepository;

    @Mock
    private IpoOfferingInfoRepository ipoOfferingInfoRepository;

    @Mock
    private UserFavoriteStockRepository userFavoriteStockRepository;

    @InjectMocks
    private IpoService ipoService;

    @Test
    @DisplayName("공모주 목록 조회 시 페이지 정보를 조합해 반환한다")
    void getIpos_success() {
        List<IpoListItem> items = List.of(new IpoListItem(
                1L,
                "AIPO",
                "에이아이피오",
                "KOSDAQ",
                "공시문서 분석 기업",
                new BigDecimal("15000.00"),
                LocalDate.of(2026, 4, 28),
                LocalDate.of(2026, 4, 29),
                LocalDate.of(2026, 5, 8),
                new BigDecimal("87.5"),
                91
        ));

        when(ipoStockRepository.findIpoList(0, 20, "에이", "subscriptionStartDate", "asc"))
                .thenReturn(items);
        when(ipoStockRepository.countIpoList("에이")).thenReturn(21L);

        IpoListResponse response = ipoService.getIpos(0, 20, " 에이 ", "subscriptionStartDate", "asc");

        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).ipoId()).isEqualTo(1L);
        assertThat(response.page()).isEqualTo(0);
        assertThat(response.size()).isEqualTo(20);
        assertThat(response.totalElements()).isEqualTo(21L);
        assertThat(response.totalPages()).isEqualTo(2);
        assertThat(response.hasNext()).isTrue();
        verify(ipoStockRepository).findIpoList(0, 20, "에이", "subscriptionStartDate", "asc");
        verify(ipoStockRepository).countIpoList("에이");
    }

    @Test
    @DisplayName("공모주 상세 조회를 정상적으로 조립한다")
    void getIpoDetail_success() {
        Long ipoId = 1L;
        Long userId = 10L;
        IpoStock ipoStock = ipoStock(
                ipoId,
                "AIPO",
                "공시문서 기반 기업",
                new BigDecimal("15000.00"),
                LocalDate.of(2026, 4, 28),
                LocalDate.of(2026, 4, 29),
                LocalDate.of(2026, 5, 8)
        );

        when(ipoStockRepository.findById(ipoId)).thenReturn(Optional.of(ipoStock));
        when(ipoLeadManagerRepository.findAllByStock_IdOrderByDisplayOrderAsc(ipoId)).thenReturn(List.of(
                leadManager(ipoStock, "주관사A", 1),
                leadManager(ipoStock, "주관사B", 2)
        ));
        when(ipoAttractionScoreRepository.findTopByStock_IdOrderByCalculatedAtDesc(ipoId))
                .thenReturn(Optional.of(attractionScore(ipoStock, 88, LocalDateTime.of(2026, 4, 21, 9, 0))));
        when(ipoAttractionReasonRepository.findAllByStock_IdOrderByDisplayOrderAsc(ipoId)).thenReturn(List.of(
                attractionReason(ipoStock, "수요예측 강세", "기관 경쟁률이 높습니다.", 1),
                attractionReason(ipoStock, "유통물량 부담 적음", "상장 직후 유통 가능 물량이 제한적입니다.", 2)
        ));
        when(ipoDemandForecastRepository.findByStock_Id(ipoId))
                .thenReturn(Optional.of(demandForecast(ipoStock)));
        when(ipoSubscriptionCompetitionRepository.findByStock_Id(ipoId))
                .thenReturn(Optional.of(subscriptionCompetition(ipoStock)));
        when(ipoScheduleRepository.findAllByStock_Id(ipoId)).thenReturn(List.of(
                schedule(ipoStock, ScheduleType.DEMAND_FORECAST_START, LocalDate.of(2026, 4, 20)),
                schedule(ipoStock, ScheduleType.DEMAND_FORECAST_END, LocalDate.of(2026, 4, 21)),
                schedule(ipoStock, ScheduleType.REFUND, LocalDate.of(2026, 5, 2)),
                schedule(ipoStock, ScheduleType.LISTING, LocalDate.of(2026, 5, 9))
        ));
        when(ipoDepositInfoRepository.findAllByStock_IdOrderByDisplayOrderAsc(ipoId)).thenReturn(List.of(
                depositInfo(ipoStock, "증권사A", new BigDecimal("75000.00"), 1),
                depositInfo(ipoStock, "증권사B", new BigDecimal("80000.00"), 2)
        ));
        when(ipoOfferingInfoRepository.findByStock_Id(ipoId))
                .thenReturn(Optional.of(offeringInfo(ipoStock)));
        when(userFavoriteStockRepository.existsByUserIdAndStock_Id(userId, ipoId)).thenReturn(true);

        IpoDetailResponse response = ipoService.getIpoDetail(ipoId, userId);

        assertThat(response.ipoId()).isEqualTo(ipoId);
        assertThat(response.summary().companyName()).isEqualTo("AIPO");
        assertThat(response.summary().oneLineDescription()).isEqualTo("공시문서 기반 기업");
        assertThat(response.summary().confirmedOfferPrice()).isEqualByComparingTo("15000.00");
        assertThat(response.summary().leadManagers()).containsExactly("주관사A", "주관사B");
        assertThat(response.summary().subscriptionPeriod().startDate()).isEqualTo(LocalDate.of(2026, 4, 28));
        assertThat(response.summary().subscriptionPeriod().endDate()).isEqualTo(LocalDate.of(2026, 4, 29));
        assertThat(response.summary().isFavorite()).isTrue();
        assertThat(response.attraction().totalScore()).isEqualByComparingTo("88");
        assertThat(response.attraction().reasons()).hasSize(2);
        assertThat(response.demandForecast().institutionalCompetitionRate()).isEqualByComparingTo("1234.56");
        assertThat(response.subscriptionCompetition().defaultTab()).isEqualTo(CompetitionTab.EQUAL);
        assertThat(response.subscriptionCompetition().equalAllocation().competitionRate()).isEqualByComparingTo("150.25");
        assertThat(response.schedule().demandForecastPeriod().startDate()).isEqualTo(LocalDate.of(2026, 4, 20));
        assertThat(response.schedule().demandForecastPeriod().endDate()).isEqualTo(LocalDate.of(2026, 4, 21));
        assertThat(response.schedule().refundDate()).isEqualTo(LocalDate.of(2026, 5, 2));
        assertThat(response.schedule().listingDate()).isEqualTo(LocalDate.of(2026, 5, 9));
        assertThat(response.depositInfos()).hasSize(2);
        assertThat(response.depositInfos().get(0).securitiesCompanyName()).isEqualTo("증권사A");
        assertThat(response.offeringInfo().marketCap()).isEqualByComparingTo("250000000000.00");
    }

    @Test
    @DisplayName("없는 공모주를 조회하면 IPO_NOT_FOUND 예외가 발생한다")
    void getIpoDetail_whenIpoDoesNotExist_throwIpoNotFound() {
        Long ipoId = 999L;

        when(ipoStockRepository.findById(ipoId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ipoService.getIpoDetail(ipoId, 1L))
                .isInstanceOf(CustomException.class)
                .extracting(exception -> ((CustomException) exception).getErrorCode())
                .isEqualTo(ErrorCode.IPO_NOT_FOUND);
    }

    @Test
    @DisplayName("비로그인 사용자는 관심 종목 여부가 false 이고 관심 종목 조회를 하지 않는다")
    void getIpoDetail_whenUserIsAnonymous_isFavoriteFalse() {
        Long ipoId = 2L;
        IpoStock ipoStock = ipoStock(
                ipoId,
                "비로그인 테스트 기업",
                "설명",
                new BigDecimal("10000.00"),
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 2),
                LocalDate.of(2026, 5, 10)
        );
        stubDefaultDetailSources(ipoId, ipoStock);

        IpoDetailResponse response = ipoService.getIpoDetail(ipoId, null);

        assertThat(response.summary().isFavorite()).isFalse();
        verify(userFavoriteStockRepository, never()).existsByUserIdAndStock_Id(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyLong());
    }

    @Test
    @DisplayName("일부 보조 데이터가 없으면 null 과 빈 리스트로 응답한다")
    void getIpoDetail_whenOptionalDataMissing_returnsNullAndEmptyCollections() {
        Long ipoId = 3L;
        IpoStock ipoStock = ipoStock(
                ipoId,
                "보조데이터 없는 기업",
                "설명 없음",
                null,
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 2),
                LocalDate.of(2026, 6, 15)
        );

        when(ipoStockRepository.findById(ipoId)).thenReturn(Optional.of(ipoStock));
        when(ipoLeadManagerRepository.findAllByStock_IdOrderByDisplayOrderAsc(ipoId)).thenReturn(Collections.emptyList());
        when(ipoAttractionScoreRepository.findTopByStock_IdOrderByCalculatedAtDesc(ipoId)).thenReturn(Optional.empty());
        when(ipoAttractionReasonRepository.findAllByStock_IdOrderByDisplayOrderAsc(ipoId)).thenReturn(Collections.emptyList());
        when(ipoDemandForecastRepository.findByStock_Id(ipoId)).thenReturn(Optional.empty());
        when(ipoSubscriptionCompetitionRepository.findByStock_Id(ipoId)).thenReturn(Optional.empty());
        when(ipoScheduleRepository.findAllByStock_Id(ipoId)).thenReturn(Collections.emptyList());
        when(ipoDepositInfoRepository.findAllByStock_IdOrderByDisplayOrderAsc(ipoId)).thenReturn(Collections.emptyList());
        when(ipoOfferingInfoRepository.findByStock_Id(ipoId)).thenReturn(Optional.empty());

        IpoDetailResponse response = ipoService.getIpoDetail(ipoId, null);

        assertThat(response.summary().leadManagers()).isEmpty();
        assertThat(response.summary().isFavorite()).isFalse();
        assertThat(response.attraction().totalScore()).isNull();
        assertThat(response.attraction().reasons()).isEmpty();
        assertThat(response.demandForecast().institutionalCompetitionRate()).isNull();
        assertThat(response.subscriptionCompetition().defaultTab()).isNull();
        assertThat(response.subscriptionCompetition().equalAllocation().expectedAllocationQuantity()).isNull();
        assertThat(response.schedule().demandForecastPeriod().startDate()).isNull();
        assertThat(response.schedule().demandForecastPeriod().endDate()).isNull();
        assertThat(response.schedule().refundDate()).isNull();
        assertThat(response.schedule().listingDate()).isEqualTo(LocalDate.of(2026, 6, 15));
        assertThat(response.depositInfos()).isEmpty();
        assertThat(response.offeringInfo().marketCap()).isNull();
    }

    @Test
    @DisplayName("매력지수는 최신 1건 조회 결과를 사용한다")
    void getIpoDetail_usesLatestAttractionScore() {
        Long ipoId = 4L;
        IpoStock ipoStock = ipoStock(
                ipoId,
                "매력지수 기업",
                "설명",
                new BigDecimal("12000.00"),
                LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 7, 2),
                LocalDate.of(2026, 7, 20)
        );
        IpoAttractionScore latestScore = attractionScore(
                ipoStock,
                91,
                LocalDateTime.of(2026, 4, 21, 10, 30)
        );

        stubDefaultDetailSources(ipoId, ipoStock);
        when(ipoAttractionScoreRepository.findTopByStock_IdOrderByCalculatedAtDesc(ipoId))
                .thenReturn(Optional.of(latestScore));

        IpoDetailResponse response = ipoService.getIpoDetail(ipoId, null);

        assertThat(response.attraction().totalScore()).isEqualByComparingTo("91");
        verify(ipoAttractionScoreRepository).findTopByStock_IdOrderByCalculatedAtDesc(ipoId);
    }

    @Test
    @DisplayName("일정 리스트를 ScheduleSection 으로 재구성한다")
    void getIpoDetail_rebuildsScheduleSectionFromScheduleList() {
        Long ipoId = 5L;
        IpoStock ipoStock = ipoStock(
                ipoId,
                "일정 재구성 기업",
                "설명",
                new BigDecimal("13000.00"),
                LocalDate.of(2026, 8, 4),
                LocalDate.of(2026, 8, 5),
                LocalDate.of(2026, 8, 20)
        );

        stubDefaultDetailSources(ipoId, ipoStock);
        when(ipoScheduleRepository.findAllByStock_Id(ipoId)).thenReturn(List.of(
                schedule(ipoStock, ScheduleType.REFUND, LocalDate.of(2026, 8, 8)),
                schedule(ipoStock, ScheduleType.DEMAND_FORECAST_END, LocalDate.of(2026, 7, 31)),
                schedule(ipoStock, ScheduleType.DEMAND_FORECAST_START, LocalDate.of(2026, 7, 30)),
                schedule(ipoStock, ScheduleType.LISTING, LocalDate.of(2026, 8, 21))
        ));

        IpoDetailResponse response = ipoService.getIpoDetail(ipoId, null);

        assertThat(response.schedule().demandForecastPeriod().startDate()).isEqualTo(LocalDate.of(2026, 7, 30));
        assertThat(response.schedule().demandForecastPeriod().endDate()).isEqualTo(LocalDate.of(2026, 7, 31));
        assertThat(response.schedule().subscriptionPeriod().startDate()).isEqualTo(LocalDate.of(2026, 8, 4));
        assertThat(response.schedule().subscriptionPeriod().endDate()).isEqualTo(LocalDate.of(2026, 8, 5));
        assertThat(response.schedule().refundDate()).isEqualTo(LocalDate.of(2026, 8, 8));
        assertThat(response.schedule().listingDate()).isEqualTo(LocalDate.of(2026, 8, 21));
    }

    private void stubDefaultDetailSources(Long ipoId, IpoStock ipoStock) {
        when(ipoStockRepository.findById(ipoId)).thenReturn(Optional.of(ipoStock));
        when(ipoLeadManagerRepository.findAllByStock_IdOrderByDisplayOrderAsc(ipoId)).thenReturn(Collections.emptyList());
        when(ipoAttractionScoreRepository.findTopByStock_IdOrderByCalculatedAtDesc(ipoId)).thenReturn(Optional.empty());
        when(ipoAttractionReasonRepository.findAllByStock_IdOrderByDisplayOrderAsc(ipoId)).thenReturn(Collections.emptyList());
        when(ipoDemandForecastRepository.findByStock_Id(ipoId)).thenReturn(Optional.empty());
        when(ipoSubscriptionCompetitionRepository.findByStock_Id(ipoId)).thenReturn(Optional.empty());
        when(ipoScheduleRepository.findAllByStock_Id(ipoId)).thenReturn(Collections.emptyList());
        when(ipoDepositInfoRepository.findAllByStock_IdOrderByDisplayOrderAsc(ipoId)).thenReturn(Collections.emptyList());
        when(ipoOfferingInfoRepository.findByStock_Id(ipoId)).thenReturn(Optional.empty());
    }

    private IpoStock ipoStock(
            Long id,
            String companyName,
            String oneLineDescription,
            BigDecimal confirmedOfferPrice,
            LocalDate subscriptionStartDate,
            LocalDate subscriptionEndDate,
            LocalDate listingDate
    ) {
        IpoStock ipoStock = instantiate(IpoStock.class);
        ReflectionTestUtils.setField(ipoStock, "id", id);
        ReflectionTestUtils.setField(ipoStock, "companyName", companyName);
        ReflectionTestUtils.setField(ipoStock, "oneLineDescription", oneLineDescription);
        ReflectionTestUtils.setField(ipoStock, "confirmedOfferPrice", confirmedOfferPrice);
        ReflectionTestUtils.setField(ipoStock, "subscriptionStartDate", subscriptionStartDate);
        ReflectionTestUtils.setField(ipoStock, "subscriptionEndDate", subscriptionEndDate);
        ReflectionTestUtils.setField(ipoStock, "listingDate", listingDate);
        ReflectionTestUtils.setField(ipoStock, "createdAt", LocalDateTime.now());
        ReflectionTestUtils.setField(ipoStock, "updatedAt", LocalDateTime.now());
        return ipoStock;
    }

    private IpoLeadManager leadManager(IpoStock ipoStock, String managerName, int displayOrder) {
        IpoLeadManager leadManager = instantiate(IpoLeadManager.class);
        ReflectionTestUtils.setField(leadManager, "stock", ipoStock);
        ReflectionTestUtils.setField(leadManager, "managerName", managerName);
        ReflectionTestUtils.setField(leadManager, "displayOrder", displayOrder);
        ReflectionTestUtils.setField(leadManager, "createdAt", LocalDateTime.now());
        return leadManager;
    }

    private IpoAttractionScore attractionScore(IpoStock ipoStock, int totalScore, LocalDateTime calculatedAt) {
        IpoAttractionScore attractionScore = instantiate(IpoAttractionScore.class);
        ReflectionTestUtils.setField(attractionScore, "stock", ipoStock);
        ReflectionTestUtils.setField(attractionScore, "totalScore", totalScore);
        ReflectionTestUtils.setField(attractionScore, "calculatedAt", calculatedAt);
        return attractionScore;
    }

    private IpoAttractionReason attractionReason(
            IpoStock ipoStock,
            String title,
            String description,
            int displayOrder
    ) {
        IpoAttractionReason reason = instantiate(IpoAttractionReason.class);
        ReflectionTestUtils.setField(reason, "stock", ipoStock);
        ReflectionTestUtils.setField(reason, "title", title);
        ReflectionTestUtils.setField(reason, "description", description);
        ReflectionTestUtils.setField(reason, "displayOrder", displayOrder);
        ReflectionTestUtils.setField(reason, "createdAt", LocalDateTime.now());
        ReflectionTestUtils.setField(reason, "updatedAt", LocalDateTime.now());
        return reason;
    }

    private IpoDemandForecast demandForecast(IpoStock ipoStock) {
        IpoDemandForecast demandForecast = instantiate(IpoDemandForecast.class);
        ReflectionTestUtils.setField(demandForecast, "stock", ipoStock);
        ReflectionTestUtils.setField(demandForecast, "institutionalCompetitionRate", new BigDecimal("1234.56"));
        ReflectionTestUtils.setField(demandForecast, "participatingInstitutionCount", 2450);
        ReflectionTestUtils.setField(demandForecast, "aboveUpperPriceCompetitionRate", new BigDecimal("92.10"));
        ReflectionTestUtils.setField(demandForecast, "aboveUpperPriceInstitutionCount", 2300);
        ReflectionTestUtils.setField(demandForecast, "lockupCompetitionRate", new BigDecimal("80.50"));
        ReflectionTestUtils.setField(demandForecast, "lockupInstitutionCount", 1800);
        ReflectionTestUtils.setField(demandForecast, "lockupRate", new BigDecimal("15.30"));
        ReflectionTestUtils.setField(demandForecast, "createdAt", LocalDateTime.now());
        ReflectionTestUtils.setField(demandForecast, "updatedAt", LocalDateTime.now());
        return demandForecast;
    }

    private IpoSubscriptionCompetition subscriptionCompetition(IpoStock ipoStock) {
        IpoSubscriptionCompetition subscriptionCompetition = instantiate(IpoSubscriptionCompetition.class);
        ReflectionTestUtils.setField(subscriptionCompetition, "stock", ipoStock);
        ReflectionTestUtils.setField(subscriptionCompetition, "defaultTab", CompetitionTabType.EQUAL);
        ReflectionTestUtils.setField(subscriptionCompetition, "equalExpectedAllocationQuantity", new BigDecimal("2.50"));
        ReflectionTestUtils.setField(subscriptionCompetition, "equalCompetitionRate", new BigDecimal("150.25"));
        ReflectionTestUtils.setField(subscriptionCompetition, "proportionalExpectedAllocationQuantity", new BigDecimal("1.25"));
        ReflectionTestUtils.setField(subscriptionCompetition, "proportionalCompetitionRate", new BigDecimal("320.75"));
        ReflectionTestUtils.setField(subscriptionCompetition, "createdAt", LocalDateTime.now());
        ReflectionTestUtils.setField(subscriptionCompetition, "updatedAt", LocalDateTime.now());
        return subscriptionCompetition;
    }

    private IpoSchedule schedule(IpoStock ipoStock, ScheduleType scheduleType, LocalDate scheduleDate) {
        IpoSchedule schedule = instantiate(IpoSchedule.class);
        ReflectionTestUtils.setField(schedule, "stock", ipoStock);
        ReflectionTestUtils.setField(schedule, "scheduleType", scheduleType);
        ReflectionTestUtils.setField(schedule, "scheduleDate", scheduleDate);
        ReflectionTestUtils.setField(schedule, "createdAt", LocalDateTime.now());
        ReflectionTestUtils.setField(schedule, "updatedAt", LocalDateTime.now());
        return schedule;
    }

    private IpoDepositInfo depositInfo(
            IpoStock ipoStock,
            String securitiesCompanyName,
            BigDecimal amountForTenShares,
            int displayOrder
    ) {
        IpoDepositInfo depositInfo = instantiate(IpoDepositInfo.class);
        ReflectionTestUtils.setField(depositInfo, "stock", ipoStock);
        ReflectionTestUtils.setField(depositInfo, "securitiesCompanyName", securitiesCompanyName);
        ReflectionTestUtils.setField(depositInfo, "amountForTenShares", amountForTenShares);
        ReflectionTestUtils.setField(depositInfo, "displayOrder", displayOrder);
        ReflectionTestUtils.setField(depositInfo, "createdAt", LocalDateTime.now());
        ReflectionTestUtils.setField(depositInfo, "updatedAt", LocalDateTime.now());
        return depositInfo;
    }

    private IpoOfferingInfo offeringInfo(IpoStock ipoStock) {
        IpoOfferingInfo offeringInfo = instantiate(IpoOfferingInfo.class);
        ReflectionTestUtils.setField(offeringInfo, "stock", ipoStock);
        ReflectionTestUtils.setField(offeringInfo, "marketCap", new BigDecimal("250000000000.00"));
        ReflectionTestUtils.setField(offeringInfo, "equalAllocationRatio", new BigDecimal("50.00"));
        ReflectionTestUtils.setField(offeringInfo, "circulatingRatio", new BigDecimal("18.40"));
        ReflectionTestUtils.setField(offeringInfo, "oldShareSaleRatio", new BigDecimal("5.10"));
        ReflectionTestUtils.setField(offeringInfo, "createdAt", LocalDateTime.now());
        ReflectionTestUtils.setField(offeringInfo, "updatedAt", LocalDateTime.now());
        return offeringInfo;
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
