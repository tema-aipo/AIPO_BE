package com.aipo.backend.domain.ipo.service;

import com.aipo.backend.domain.investmentprofile.entity.InvestmentProfileType;
import com.aipo.backend.domain.investmentprofile.repository.UserInvestmentProfileResultRepository;
import com.aipo.backend.domain.ipo.dto.AttractionReasonItem;
import com.aipo.backend.domain.ipo.dto.AttractionSection;
import com.aipo.backend.domain.ipo.dto.AttractivenessResponse;
import com.aipo.backend.domain.ipo.dto.CompetitionInfo;
import com.aipo.backend.domain.ipo.dto.DateRange;
import com.aipo.backend.domain.ipo.dto.DemandForecastSection;
import com.aipo.backend.domain.ipo.dto.DepositInfoItem;
import com.aipo.backend.domain.ipo.dto.FactorScoresResponse;
import com.aipo.backend.domain.ipo.dto.IpoDetailResponse;
import com.aipo.backend.domain.ipo.dto.IpoListItem;
import com.aipo.backend.domain.ipo.dto.IpoListResponse;
import com.aipo.backend.domain.ipo.dto.OfferingInfoSection;
import com.aipo.backend.domain.ipo.dto.ScheduleSection;
import com.aipo.backend.domain.ipo.dto.SubscriptionCompetitionSection;
import com.aipo.backend.domain.ipo.dto.SummarySection;
import com.aipo.backend.domain.ipo.entity.IpoDemandForecast;
import com.aipo.backend.domain.ipo.entity.IpoStock;
import com.aipo.backend.domain.ipo.entity.IpoSubscriptionCompany;
import com.aipo.backend.domain.ipo.entity.IpoViewLog;
import com.aipo.backend.domain.ipo.repository.AttractivenessIpoProjection;
import com.aipo.backend.domain.ipo.repository.IpoDemandForecastRepository;
import com.aipo.backend.domain.ipo.repository.IpoDetailProjection;
import com.aipo.backend.domain.ipo.repository.IpoStockRepository;
import com.aipo.backend.domain.ipo.repository.IpoSubscriptionCompanyRepository;
import com.aipo.backend.domain.ipo.repository.IpoViewLogRepository;
import com.aipo.backend.domain.ipo.repository.UserFavoriteStockRepository;
import com.aipo.backend.global.exception.CustomException;
import com.aipo.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

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
    private static final String VIEW_SOURCE_DETAIL = "DETAIL";
    private static final double AGGRESSIVE_REMAINING_WEIGHT = 0.70;
    private static final double BALANCED_REMAINING_WEIGHT = 0.85;
    private static final double CONSERVATIVE_REMAINING_WEIGHT = 0.95;
    private static final ZoneId KOREA_ZONE = ZoneId.of("Asia/Seoul");

    private final IpoStockRepository ipoStockRepository;
    private final IpoDemandForecastRepository ipoDemandForecastRepository;
    private final IpoSubscriptionCompanyRepository ipoSubscriptionCompanyRepository;
    private final IpoViewLogRepository ipoViewLogRepository;
    private final UserFavoriteStockRepository userFavoriteStockRepository;
    private final UserInvestmentProfileResultRepository userInvestmentProfileResultRepository;
    private final AttractivenessService attractivenessService;

    public IpoListResponse getIpos(int page, int size, String keyword, String sort, String direction, Long userId) {
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
        items = applyCalculatedListValues(items, userId);
        long totalElements = ipoStockRepository.countIpoList(normalizedKeyword);

        return IpoListResponse.of(items, normalizedPage, normalizedSize, totalElements);
    }

    @Transactional
    public IpoDetailResponse getIpoDetail(Long ipoId, Long userId) {
        IpoDetailProjection ipo = ipoStockRepository.findDetailByStockId(ipoId)
                .orElseThrow(() -> new CustomException(ErrorCode.IPO_NOT_FOUND));
        saveViewLog(ipoId, userId);

        IpoDemandForecast demandForecast = ipoDemandForecastRepository.findByStock_Id(ipoId).orElse(null);
        List<IpoSubscriptionCompany> depositInfos =
                ipoSubscriptionCompanyRepository.findAllByStock_IdOrderByDisplayOrderAsc(ipoId);
        AttractivenessResponse attractiveness = buildAttractiveness(ipo, userId);

        return new IpoDetailResponse(
                ipo.getStockId(),
                buildSummary(ipo, userId),
                buildAttraction(attractiveness, ipo.getAttractScore()),
                attractiveness,
                buildDemandForecast(demandForecast),
                buildSubscriptionCompetition(),
                buildSchedule(ipo),
                buildDepositInfos(depositInfos),
                buildOfferingInfo()
        );
    }

    private void saveViewLog(Long ipoId, Long userId) {
        IpoStock stockReference = ipoStockRepository.getReferenceById(ipoId);
        ipoViewLogRepository.save(IpoViewLog.create(userId, stockReference, VIEW_SOURCE_DETAIL));
    }

    private SummarySection buildSummary(IpoDetailProjection ipo, Long userId) {
        boolean isFavorite = userId != null
                && userFavoriteStockRepository.existsByUserIdAndStock_Id(userId, ipo.getStockId());

        return new SummarySection(
                IpoStockViewMapper.displayCompanyName(ipo.getCompanyName(), ipo.getStockName(), ipo.getCorpName()),
                ipo.getOneLineDescription(),
                IpoStockViewMapper.offerPrice(ipo.getConfirmedOfferPrice(), ipo.getOfferingPrice()),
                leadManagerNames(ipo),
                new DateRange(
                        subscriptionStartDate(ipo),
                        subscriptionEndDate(ipo)
                ),
                isFavorite,
                ipo.getMarketType()
        );
    }

    private List<String> leadManagerNames(IpoDetailProjection ipo) {
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
            Float attractScore
    ) {
        // 기존 상세 응답 호환 필드도 사용자 성향이 반영된 선택 매력지수로 내려준다.
        BigDecimal totalScore = attractiveness != null
                ? BigDecimal.valueOf(attractiveness.selected().score())
                : attractScore == null ? null : BigDecimal.valueOf(attractScore);

        List<AttractionReasonItem> reasons = buildFactorScoreReasons(attractiveness);

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

        return attractivenessService.calculateForIpo(targetIpo, allIpos, currentProfileType(userId));
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

    private SubscriptionCompetitionSection buildSubscriptionCompetition() {
        return new SubscriptionCompetitionSection(
                null,
                new CompetitionInfo(null, null),
                new CompetitionInfo(null, null)
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

    private List<DepositInfoItem> buildDepositInfos(List<IpoSubscriptionCompany> depositInfos) {
        if (depositInfos == null || depositInfos.isEmpty()) {
            return Collections.emptyList();
        }

        return depositInfos.stream()
                .map(depositInfo -> new DepositInfoItem(
                        depositInfo.getDisplayOrder(),
                        depositInfo.getSecuritiesCompanyName(),
                        depositInfo.getAllocatedShareCount(),
                        depositInfo.getSubscriptionLimitShareCount(),
                        depositInfo.getNote()
                ))
                .toList();
    }

    private OfferingInfoSection buildOfferingInfo() {
        return new OfferingInfoSection(null, null, null, null);
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

    private InvestmentProfileType currentProfileType(Long userId) {
        if (userId == null) {
            return null;
        }
        return userInvestmentProfileResultRepository
                .findTopByUserIdAndCurrentTrueOrderByCreatedAtDescIdDesc(userId)
                .map(result -> result.getProfileType())
                .orElse(null);
    }

    private List<IpoListItem> applyCalculatedListValues(List<IpoListItem> items, Long userId) {
        if (items == null || items.isEmpty()) {
            return items;
        }

        Map<Long, Integer> scoreByStockId = calculateListScores(items, currentProfileType(userId));
        Map<Long, Integer> todayViewCountByStockId = countTodayViews(items.stream()
                .map(IpoListItem::ipoId)
                .toList());

        return items.stream()
                .map(item -> new IpoListItem(
                        item.ipoId(),
                        item.stockName(),
                        item.companyName(),
                        item.marketType(),
                        item.oneLineDescription(),
                        item.confirmedOfferPrice(),
                        item.subscriptionStartDate(),
                        item.subscriptionEndDate(),
                        item.listingDate(),
                        BigDecimal.valueOf(scoreByStockId.getOrDefault(
                                item.ipoId(),
                                item.attractionScore() == null ? 0 : item.attractionScore().intValue()
                        )),
                        todayViewCountByStockId.getOrDefault(item.ipoId(), 0),
                        item.demandForecastDate(),
                        item.refundDate()
                ))
                .toList();
    }

    private Map<Long, Integer> countTodayViews(List<Long> stockIds) {
        LocalDate today = LocalDate.now(KOREA_ZONE);
        Map<Long, Integer> viewCounts = ipoStockRepository.countViewsByStockIds(
                stockIds,
                today.atStartOfDay(),
                today.plusDays(1).atStartOfDay()
        );
        return viewCounts == null ? Map.of() : viewCounts;
    }

    private Map<Long, Integer> calculateListScores(
            List<IpoListItem> items,
            InvestmentProfileType currentProfileType
    ) {
        List<AttractivenessIpoProjection> allIpos;
        try {
            allIpos = ipoStockRepository.findAllForAttractiveness();
        } catch (DataAccessException exception) {
            allIpos = items.stream()
                    .map(this::fallbackAttractivenessProjection)
                    .toList();
        }

        if (allIpos == null || allIpos.isEmpty()) {
            allIpos = items.stream()
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

        return items.stream()
                .collect(Collectors.toMap(
                        IpoListItem::ipoId,
                        item -> {
                            AttractivenessIpoProjection target = projectionByStockId.getOrDefault(
                                    item.ipoId(),
                                    fallbackAttractivenessProjection(item)
                            );
                            return attractivenessService
                                    .calculateForIpo(target, finalAllIpos, currentProfileType)
                                    .selected()
                                    .score();
                        },
                        (existing, replacement) -> existing
                ));
    }

    private AttractivenessIpoProjection fallbackAttractivenessProjection(IpoListItem item) {
        return new SimpleAttractivenessIpoProjection(item.ipoId(), item.companyName(), null, null, null, null);
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
