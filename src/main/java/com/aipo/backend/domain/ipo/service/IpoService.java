package com.aipo.backend.domain.ipo.service;

import com.aipo.backend.domain.investmentprofile.entity.InvestmentProfileType;
import com.aipo.backend.domain.investmentprofile.repository.UserInvestmentProfileResultRepository;
import com.aipo.backend.domain.ipo.dto.*;
import com.aipo.backend.domain.ipo.entity.*;
import com.aipo.backend.domain.ipo.repository.*;
import com.aipo.backend.global.exception.CustomException;
import com.aipo.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class IpoService {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 50;
    private static final String PROFILE_AGGRESSIVE = "aggressive";
    private static final String PROFILE_BALANCED = "balanced";
    private static final String PROFILE_CONSERVATIVE = "conservative";
    private static final double AGGRESSIVE_REMAINING_WEIGHT = 0.70;
    private static final double BALANCED_REMAINING_WEIGHT = 0.85;
    private static final double CONSERVATIVE_REMAINING_WEIGHT = 0.95;

    private final IpoStockRepository ipoStockRepository;
    private final IpoLeadManagerRepository ipoLeadManagerRepository;
    private final IpoAttractionReasonRepository ipoAttractionReasonRepository;
    private final IpoDemandForecastRepository ipoDemandForecastRepository;
    private final IpoSubscriptionCompetitionRepository ipoSubscriptionCompetitionRepository;
    private final IpoDepositInfoRepository ipoDepositInfoRepository;
    private final IpoOfferingInfoRepository ipoOfferingInfoRepository;
    private final UserFavoriteStockRepository userFavoriteStockRepository;
    private final UserInvestmentProfileResultRepository userInvestmentProfileResultRepository;
    private final AttractivenessService attractivenessService;

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

    @Transactional
    public IpoDetailResponse getIpoDetail(Long ipoId, Long userId) {
        IpoDetailProjection ipo = ipoStockRepository.findDetailByStockId(ipoId)
                .orElseThrow(() -> new CustomException(ErrorCode.IPO_NOT_FOUND));
        ipoStockRepository.incrementRecentGrowthScore(ipoId);

        List<IpoLeadManager> leadManagers = ipoLeadManagerRepository.findAllByStock_IdOrderByDisplayOrderAsc(ipoId);
        List<IpoAttractionReason> attractionReasons =
                ipoAttractionReasonRepository.findAllByStock_IdOrderByDisplayOrderAsc(ipoId);
        IpoDemandForecast demandForecast = ipoDemandForecastRepository.findByStock_Id(ipoId).orElse(null);
        IpoSubscriptionCompetition subscriptionCompetition =
                ipoSubscriptionCompetitionRepository.findByStock_Id(ipoId).orElse(null);
        List<IpoDepositInfo> depositInfos = ipoDepositInfoRepository.findAllByStock_IdOrderByDisplayOrderAsc(ipoId);
        IpoOfferingInfo offeringInfo = ipoOfferingInfoRepository.findByStock_Id(ipoId).orElse(null);
        AttractivenessResponse attractiveness = buildAttractiveness(ipo, userId);

        return new IpoDetailResponse(
                ipo.getStockId(),
                buildSummary(ipo, leadManagers, userId),
                buildAttraction(attractiveness, ipo.getAttractScore(), attractionReasons),
                attractiveness,
                buildDemandForecast(demandForecast),
                buildSubscriptionCompetition(subscriptionCompetition),
                buildSchedule(ipo),
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
            AttractivenessResponse attractiveness,
            Float attractScore,
            List<IpoAttractionReason> attractionReasons
    ) {
        // 기존 상세 응답 호환 필드도 사용자 성향이 반영된 선택 매력지수로 내려준다.
        BigDecimal totalScore = attractiveness != null
                ? BigDecimal.valueOf(attractiveness.selected().score())
                : attractScore == null ? null : BigDecimal.valueOf(attractScore);

        List<AttractionReasonItem> reasons = buildFactorScoreReasons(attractiveness);
        if (reasons.isEmpty()) {
            reasons = attractionReasons == null
                    ? Collections.emptyList()
                    : attractionReasons.stream()
                    .map(reason -> new AttractionReasonItem(
                            reason.getDisplayOrder(),
                            reason.getTitle(),
                            reason.getDescription()
                    ))
                    .toList();
        }

        return new AttractionSection(totalScore, reasons);
    }

    private List<AttractionReasonItem> buildFactorScoreReasons(AttractivenessResponse attractiveness) {
        if (attractiveness == null || attractiveness.factorScores() == null) {
            return Collections.emptyList();
        }

        FactorScoresResponse factorScores = attractiveness.factorScores();
        double[] weights = selectedProfileWeights(attractiveness.selectedProfile());
        int selectedScore = attractiveness.selected() == null ? 0 : attractiveness.selected().score();
        List<FactorScoreReason> factors = List.of(
                new FactorScoreReason(1, "\uAE30\uAD00 \uACBD\uC7C1\uB960", factorScores.competitionScore(), weights[0]),
                new FactorScoreReason(2, "\uC758\uBB34\uBCF4\uC720\uD655\uC57D", factorScores.instCommitmentScore(), weights[1]),
                new FactorScoreReason(3, "\uC720\uD1B5\uAC00\uB2A5\uBB3C\uB7C9", factorScores.floatingStockScore(), weights[2]),
                new FactorScoreReason(4, "\uBCF4\uD638\uC608\uC218", factorScores.lockupScore(), weights[3])
        );
        List<Integer> contributionScores = calculateContributionScores(factors, selectedScore);

        List<AttractionReasonItem> reasons = new ArrayList<>();
        for (int i = 0; i < factors.size(); i++) {
            FactorScoreReason factor = factors.get(i);
            int contributionScore = contributionScores.get(i);
            reasons.add(new AttractionReasonItem(
                    factor.displayOrder(),
                    factor.title() + " \uBC18\uC601\uC810\uC218 " + contributionScore + "\uC810",
                    formatWeightedFactorScore(factor.rawScore(), factor.weight(), contributionScore)
            ));
        }
        return reasons;
    }

    private double[] selectedProfileWeights(String selectedProfile) {
        return switch (selectedProfile == null ? PROFILE_BALANCED : selectedProfile) {
            case PROFILE_AGGRESSIVE -> new double[]{
                    0.35 / AGGRESSIVE_REMAINING_WEIGHT,
                    0.15 / AGGRESSIVE_REMAINING_WEIGHT,
                    0.15 / AGGRESSIVE_REMAINING_WEIGHT,
                    0.05 / AGGRESSIVE_REMAINING_WEIGHT
            };
            case PROFILE_CONSERVATIVE -> new double[]{
                    0.15 / CONSERVATIVE_REMAINING_WEIGHT,
                    0.35 / CONSERVATIVE_REMAINING_WEIGHT,
                    0.30 / CONSERVATIVE_REMAINING_WEIGHT,
                    0.15 / CONSERVATIVE_REMAINING_WEIGHT
            };
            default -> new double[]{
                    0.30 / BALANCED_REMAINING_WEIGHT,
                    0.25 / BALANCED_REMAINING_WEIGHT,
                    0.25 / BALANCED_REMAINING_WEIGHT,
                    0.05 / BALANCED_REMAINING_WEIGHT
            };
        };
    }

    private List<Integer> calculateContributionScores(List<FactorScoreReason> factors, int selectedScore) {
        List<Integer> contributionScores = new ArrayList<>();
        List<Integer> orderByFraction = new ArrayList<>();
        int floorSum = 0;

        for (int i = 0; i < factors.size(); i++) {
            double exactScore = rawScore(factors.get(i).rawScore()) * factors.get(i).weight();
            int floorScore = (int) Math.floor(exactScore);
            contributionScores.add(floorScore);
            orderByFraction.add(i);
            floorSum += floorScore;
        }

        orderByFraction.sort(Comparator.comparingDouble(
                index -> -(rawScore(factors.get(index).rawScore()) * factors.get(index).weight()
                        - Math.floor(rawScore(factors.get(index).rawScore()) * factors.get(index).weight()))
        ));

        int remainder = selectedScore - floorSum;
        for (int i = 0; i < remainder && i < orderByFraction.size(); i++) {
            int index = orderByFraction.get(i);
            contributionScores.set(index, contributionScores.get(index) + 1);
        }

        return contributionScores;
    }

    private String formatWeightedFactorScore(Integer rawScore, double weight, int contributionScore) {
        return "%d\uC810 \u00D7 %.1f%% = %d\uC810".formatted(
                rawScore(rawScore),
                weight * 100,
                contributionScore
        );
    }

    private int rawScore(Integer score) {
        return score == null ? 0 : score;
    }

    private record FactorScoreReason(
            int displayOrder,
            String title,
            Integer rawScore,
            double weight
    ) {
    }

    private AttractivenessResponse buildAttractiveness(IpoDetailProjection ipo, Long userId) {
        List<AttractivenessIpoProjection> allIpos;
        try {
            allIpos = ipoStockRepository.findAllForAttractiveness();
        } catch (DataAccessException exception) {
            allIpos = List.of(toAttractivenessProjection(ipo));
        }
        if (allIpos == null || allIpos.isEmpty()) {
            allIpos = List.of(toAttractivenessProjection(ipo));
        }

        AttractivenessIpoProjection targetIpo = allIpos.stream()
                .filter(candidate -> ipo.getStockId().equals(candidate.getStockId()))
                .findFirst()
                .orElseGet(() -> toAttractivenessProjection(ipo));

        InvestmentProfileType currentProfileType = userId == null
                ? null
                : userInvestmentProfileResultRepository
                .findTopByUserIdAndCurrentTrueOrderByCreatedAtDescIdDesc(userId)
                .map(result -> result.getProfileType())
                .orElse(null);

        return attractivenessService.calculateForIpo(targetIpo, allIpos, currentProfileType);
    }

    private AttractivenessIpoProjection toAttractivenessProjection(IpoDetailProjection ipo) {
        return new SimpleAttractivenessIpoProjection(
                ipo.getStockId(),
                ipo.getCorpName(),
                null,
                null,
                null,
                null
        );
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

    private ScheduleSection buildSchedule(IpoDetailProjection ipo) {
        LocalDate demandForecastStartDate = IpoStockViewMapper.parseDateText(ipo.getDemandForecastDate(), 0);
        LocalDate demandForecastEndDate = IpoStockViewMapper.parseDateText(ipo.getDemandForecastDate(), 1);
        LocalDate refundDate = IpoStockViewMapper.parseDateText(ipo.getRefundDate(), 0);
        LocalDate listingDate = IpoStockViewMapper.parseIsoDate(ipo.getListingDate());

        return new ScheduleSection(
                new DateRange(demandForecastStartDate, demandForecastEndDate),
                new DateRange(
                        subscriptionStartDate(ipo),
                        subscriptionEndDate(ipo)
                ),
                refundDate,
                listingDate,
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
