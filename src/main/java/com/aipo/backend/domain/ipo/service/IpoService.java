package com.aipo.backend.domain.ipo.service;

import com.aipo.backend.domain.ipo.dto.*;
import com.aipo.backend.domain.ipo.entity.*;
import com.aipo.backend.domain.ipo.repository.*;
import com.aipo.backend.global.exception.CustomException;
import com.aipo.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class IpoService {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 50;

    private final IpoStockRepository ipoStockRepository;
    private final IpoLeadManagerRepository ipoLeadManagerRepository;
    private final IpoAttractionScoreRepository ipoAttractionScoreRepository;
    private final IpoAttractionReasonRepository ipoAttractionReasonRepository;
    private final IpoDemandForecastRepository ipoDemandForecastRepository;
    private final IpoSubscriptionCompetitionRepository ipoSubscriptionCompetitionRepository;
    private final IpoScheduleRepository ipoScheduleRepository;
    private final IpoDepositInfoRepository ipoDepositInfoRepository;
    private final IpoOfferingInfoRepository ipoOfferingInfoRepository;
    private final UserFavoriteStockRepository userFavoriteStockRepository;

    public IpoListResponse getIpos(int page, int size, String keyword, String sort, String direction) {
        int normalizedPage = Math.max(page, DEFAULT_PAGE);
        int normalizedSize = normalizeSize(size);
        String normalizedKeyword = normalizeKeyword(keyword);

        List<IpoListItem> items = ipoStockRepository.findIpoList(
                normalizedPage,
                normalizedSize,
                normalizedKeyword,
                sort,
                direction
        );
        long totalElements = ipoStockRepository.countIpoList(normalizedKeyword);

        return IpoListResponse.of(items, normalizedPage, normalizedSize, totalElements);
    }

    public IpoDetailResponse getIpoDetail(Long ipoId, Long userId) {
        IpoStock ipoStock = ipoStockRepository.findById(ipoId)
                .orElseThrow(() -> new CustomException(ErrorCode.IPO_NOT_FOUND));

        List<IpoLeadManager> leadManagers = ipoLeadManagerRepository.findAllByStock_IdOrderByDisplayOrderAsc(ipoId);
        IpoAttractionScore attractionScore = ipoAttractionScoreRepository
                .findTopByStock_IdOrderByCalculatedAtDesc(ipoId)
                .orElse(null);
        List<IpoAttractionReason> attractionReasons =
                ipoAttractionReasonRepository.findAllByStock_IdOrderByDisplayOrderAsc(ipoId);
        IpoDemandForecast demandForecast = ipoDemandForecastRepository.findByStock_Id(ipoId).orElse(null);
        IpoSubscriptionCompetition subscriptionCompetition =
                ipoSubscriptionCompetitionRepository.findByStock_Id(ipoId).orElse(null);
        List<IpoSchedule> schedules = ipoScheduleRepository.findAllByStock_Id(ipoId);
        List<IpoDepositInfo> depositInfos = ipoDepositInfoRepository.findAllByStock_IdOrderByDisplayOrderAsc(ipoId);
        IpoOfferingInfo offeringInfo = ipoOfferingInfoRepository.findByStock_Id(ipoId).orElse(null);

        return new IpoDetailResponse(
                ipoStock.getId(),
                buildSummary(ipoStock, leadManagers, userId),
                buildAttraction(attractionScore, attractionReasons),
                buildDemandForecast(demandForecast),
                buildSubscriptionCompetition(subscriptionCompetition),
                buildSchedule(ipoStock, schedules),
                buildDepositInfos(depositInfos),
                buildOfferingInfo(offeringInfo)
        );
    }

    private SummarySection buildSummary(IpoStock ipoStock, List<IpoLeadManager> leadManagers, Long userId) {
        boolean isFavorite = userId != null
                && userFavoriteStockRepository.existsByUserIdAndStock_Id(userId, ipoStock.getId());

        return new SummarySection(
                ipoStock.getCompanyName(),
                ipoStock.getOneLineDescription(),
                ipoStock.getConfirmedOfferPrice(),
                leadManagers.stream()
                        .map(IpoLeadManager::getManagerName)
                        .toList(),
                new DateRange(ipoStock.getSubscriptionStartDate(), ipoStock.getSubscriptionEndDate()),
                isFavorite,
                ipoStock.getMarketType()
        );
    }

    private AttractionSection buildAttraction(
            IpoAttractionScore attractionScore,
            List<IpoAttractionReason> attractionReasons
    ) {
        BigDecimal totalScore = attractionScore == null
                ? null
                : BigDecimal.valueOf(attractionScore.getTotalScore());

        List<AttractionReasonItem> reasons = attractionReasons == null
                ? Collections.emptyList()
                : attractionReasons.stream()
                .map(reason -> new AttractionReasonItem(
                        reason.getDisplayOrder(),
                        reason.getTitle(),
                        reason.getDescription()
                ))
                .toList();

        return new AttractionSection(totalScore, reasons);
    }

    private DemandForecastSection buildDemandForecast(IpoDemandForecast demandForecast) {
        if (demandForecast == null) {
            return new DemandForecastSection(null, null, null, null, null, null, null);
        }

        return new DemandForecastSection(
                demandForecast.getInstitutionalCompetitionRate(),
                demandForecast.getParticipatingInstitutionCount(),
                demandForecast.getAboveUpperPriceCompetitionRate(),
                demandForecast.getAboveUpperPriceInstitutionCount(),
                demandForecast.getLockupCompetitionRate(),
                demandForecast.getLockupInstitutionCount(),
                demandForecast.getLockupRate()
        );
    }

    private SubscriptionCompetitionSection buildSubscriptionCompetition(
            IpoSubscriptionCompetition subscriptionCompetition
    ) {
        if (subscriptionCompetition == null) {
            return new SubscriptionCompetitionSection(
                    null,
                    new CompetitionInfo(null, null),
                    new CompetitionInfo(null, null)
            );
        }

        return new SubscriptionCompetitionSection(
                mapCompetitionTab(subscriptionCompetition.getDefaultTab()),
                new CompetitionInfo(
                        subscriptionCompetition.getEqualExpectedAllocationQuantity(),
                        subscriptionCompetition.getEqualCompetitionRate()
                ),
                new CompetitionInfo(
                        subscriptionCompetition.getProportionalExpectedAllocationQuantity(),
                        subscriptionCompetition.getProportionalCompetitionRate()
                )
        );
    }

    private ScheduleSection buildSchedule(IpoStock ipoStock, List<IpoSchedule> schedules) {
        LocalDate demandForecastStartDate = findScheduleDate(schedules, ScheduleType.DEMAND_FORECAST_START);
        LocalDate demandForecastEndDate = findScheduleDate(schedules, ScheduleType.DEMAND_FORECAST_END);
        LocalDate refundDate = findScheduleDate(schedules, ScheduleType.REFUND);
        LocalDate listingDate = findScheduleDate(schedules, ScheduleType.LISTING);

        return new ScheduleSection(
                new DateRange(demandForecastStartDate, demandForecastEndDate),
                new DateRange(ipoStock.getSubscriptionStartDate(), ipoStock.getSubscriptionEndDate()),
                refundDate,
                listingDate != null ? listingDate : ipoStock.getListingDate()
        );
    }

    private List<DepositInfoItem> buildDepositInfos(List<IpoDepositInfo> depositInfos) {
        if (depositInfos == null || depositInfos.isEmpty()) {
            return Collections.emptyList();
        }

        return depositInfos.stream()
                .map(depositInfo -> new DepositInfoItem(
                        depositInfo.getDisplayOrder(),
                        depositInfo.getSecuritiesCompanyName(),
                        depositInfo.getAmountForTenShares()
                ))
                .toList();
    }

    private OfferingInfoSection buildOfferingInfo(IpoOfferingInfo offeringInfo) {
        if (offeringInfo == null) {
            return new OfferingInfoSection(null, null, null, null);
        }

        return new OfferingInfoSection(
                offeringInfo.getMarketCap(),
                offeringInfo.getEqualAllocationRatio(),
                offeringInfo.getCirculatingRatio(),
                offeringInfo.getOldShareSaleRatio()
        );
    }

    private LocalDate findScheduleDate(List<IpoSchedule> schedules, ScheduleType scheduleType) {
        if (schedules == null || schedules.isEmpty()) {
            return null;
        }

        return schedules.stream()
                .filter(schedule -> schedule.getScheduleType() == scheduleType)
                .map(IpoSchedule::getScheduleDate)
                .findFirst()
                .orElse(null);
    }

    private CompetitionTab mapCompetitionTab(CompetitionTabType competitionTabType) {
        if (competitionTabType == null) {
            return null;
        }

        return CompetitionTab.valueOf(competitionTabType.name());
    }

    private int normalizeSize(int size) {
        if (size <= 0) {
            return DEFAULT_SIZE;
        }
        return Math.min(size, MAX_SIZE);
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        return keyword.trim();
    }
}
