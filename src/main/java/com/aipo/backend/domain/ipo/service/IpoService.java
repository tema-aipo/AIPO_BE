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
import java.util.Arrays;
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
        IpoDetailProjection ipo = ipoStockRepository.findDetailByStockId(ipoId)
                .orElseThrow(() -> new CustomException(ErrorCode.IPO_NOT_FOUND));

        List<IpoLeadManager> leadManagers = ipoLeadManagerRepository.findAllByStock_IdOrderByDisplayOrderAsc(ipoId);
        List<IpoAttractionReason> attractionReasons =
                ipoAttractionReasonRepository.findAllByStock_IdOrderByDisplayOrderAsc(ipoId);
        IpoDemandForecast demandForecast = ipoDemandForecastRepository.findByStock_Id(ipoId).orElse(null);
        IpoSubscriptionCompetition subscriptionCompetition =
                ipoSubscriptionCompetitionRepository.findByStock_Id(ipoId).orElse(null);
        List<IpoSchedule> schedules = ipoScheduleRepository.findAllByStock_Id(ipoId);
        List<IpoDepositInfo> depositInfos = ipoDepositInfoRepository.findAllByStock_IdOrderByDisplayOrderAsc(ipoId);
        IpoOfferingInfo offeringInfo = ipoOfferingInfoRepository.findByStock_Id(ipoId).orElse(null);

        return new IpoDetailResponse(
                ipo.getStockId(),
                buildSummary(ipo, leadManagers, userId),
                buildAttraction(ipo.getAttractScore(), attractionReasons),
                buildDemandForecast(demandForecast),
                buildSubscriptionCompetition(subscriptionCompetition),
                buildSchedule(ipo, schedules),
                buildDepositInfos(depositInfos),
                buildOfferingInfo(offeringInfo)
        );
    }

    private SummarySection buildSummary(IpoDetailProjection ipo, List<IpoLeadManager> leadManagers, Long userId) {
        boolean isFavorite = userId != null
                && userFavoriteStockRepository.existsByUserIdAndStock_Id(userId, ipo.getStockId());

        return new SummarySection(
                IpoStockViewMapper.displayCompanyName(ipo.getCompanyName(), ipo.getStockName(), ipo.getCorpName()),
                ipo.getOneLineDescription(),
                IpoStockViewMapper.offerPrice(ipo.getConfirmedOfferPrice(), ipo.getOfferingPrice()),
                leadManagerNames(ipo, leadManagers),
                new DateRange(
                        subscriptionStartDate(ipo),
                        subscriptionEndDate(ipo)
                ),
                isFavorite,
                ipo.getMarketType()
        );
    }

    private List<String> leadManagerNames(IpoDetailProjection ipo, List<IpoLeadManager> leadManagers) {
        if (leadManagers != null && !leadManagers.isEmpty()) {
            return leadManagers.stream()
                    .map(IpoLeadManager::getManagerName)
                    .toList();
        }
        if (ipo.getUnderwriter() == null || ipo.getUnderwriter().isBlank()) {
            return Collections.emptyList();
        }
        return Arrays.stream(ipo.getUnderwriter().split(","))
                .map(String::trim)
                .filter(name -> !name.isEmpty())
                .toList();
    }

    private AttractionSection buildAttraction(
            Float attractScore,
            List<IpoAttractionReason> attractionReasons
    ) {
        BigDecimal totalScore = attractScore == null
                ? null
                : BigDecimal.valueOf(attractScore);

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

    private ScheduleSection buildSchedule(IpoDetailProjection ipo, List<IpoSchedule> schedules) {
        LocalDate demandForecastStartDate = findScheduleDate(schedules, ScheduleType.DEMAND_FORECAST_START);
        LocalDate demandForecastEndDate = findScheduleDate(schedules, ScheduleType.DEMAND_FORECAST_END);
        LocalDate refundDate = findScheduleDate(schedules, ScheduleType.REFUND);
        LocalDate listingDate = findScheduleDate(schedules, ScheduleType.LISTING);

        if (demandForecastStartDate == null) {
            demandForecastStartDate = IpoStockViewMapper.parseDateText(ipo.getDemandForecastDate(), 0);
        }
        if (demandForecastEndDate == null) {
            demandForecastEndDate = IpoStockViewMapper.parseDateText(ipo.getDemandForecastDate(), 1);
        }
        if (refundDate == null) {
            refundDate = IpoStockViewMapper.parseDateText(ipo.getRefundDate(), 0);
        }

        return new ScheduleSection(
                new DateRange(demandForecastStartDate, demandForecastEndDate),
                new DateRange(
                        subscriptionStartDate(ipo),
                        subscriptionEndDate(ipo)
                ),
                refundDate,
                listingDate != null ? listingDate : IpoStockViewMapper.parseIsoDate(ipo.getListingDate()),
                ipo.getDemandForecastDate(),
                ipo.getRefundDate()
        );
    }

    private LocalDate subscriptionStartDate(IpoDetailProjection ipo) {
        LocalDate startDate = IpoStockViewMapper.parseIsoDate(ipo.getSubscriptionStartDate());
        if (startDate != null) {
            return startDate;
        }
        return IpoStockViewMapper.parseSubscriptionDateText(ipo.getSubscriptionDate(), 0);
    }

    private LocalDate subscriptionEndDate(IpoDetailProjection ipo) {
        LocalDate endDate = IpoStockViewMapper.parseIsoDate(ipo.getSubscriptionEndDate());
        if (endDate != null) {
            return endDate;
        }
        return IpoStockViewMapper.parseSubscriptionDateText(ipo.getSubscriptionDate(), 1);
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
