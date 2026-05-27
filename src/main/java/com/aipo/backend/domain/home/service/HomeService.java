package com.aipo.backend.domain.home.service;

import com.aipo.backend.domain.home.dto.*;
import com.aipo.backend.domain.home.type.HomeTab;
import com.aipo.backend.domain.investmentprofile.entity.InvestmentProfileType;
import com.aipo.backend.domain.investmentprofile.repository.UserInvestmentProfileResultRepository;
import com.aipo.backend.domain.ipo.repository.AttractivenessIpoProjection;
import com.aipo.backend.domain.ipo.repository.IpoStockRepository;
import com.aipo.backend.domain.ipo.service.AttractivenessService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service // ??쑴已??됰뮞 嚥≪뮇彛?筌ｌ꼶???④쑴留?
@RequiredArgsConstructor
public class HomeService {

    private static final int FEATURED_LISTING_START_DAYS = 1;
    private static final int FEATURED_LISTING_END_DAYS = 7;

    private final IpoStockRepository ipoStockRepository;
    private final UserInvestmentProfileResultRepository userInvestmentProfileResultRepository;
    private final AttractivenessService attractivenessService;

    public HomeResponse getHome(String tabValue, Long userId) {
        HomeTab tab = HomeTab.from(tabValue);
        List<FeaturedIpoItem> featured = getFeaturedIpos(userId);
        List<TrendingIpoItem> trending = applyTrendingRank(ipoStockRepository.findTrendingIpos());
        List<AttractivenessItem> attractivenessItems = getAttractivenessItems(tab, userId);

        return new HomeResponse(
                featured,
                trending,
                new AttractivenessResponse(tab.getValue(), attractivenessItems)
        );
    }

    private List<AttractivenessItem> getAttractivenessItems(HomeTab tab, Long userId) {
        // ?醫뤾문????肉??怨뺤뵬 ??삘뀲 鈺곌퀬??筌롫뗄苑???紐꾪뀱
        List<AttractivenessItem> items = switch (tab) {
            case RECENT_GROWTH -> ipoStockRepository.findAttractivenessByRecentGrowth();
            case SUBSCRIPTION_UPCOMING -> ipoStockRepository.findAttractivenessBySubscriptionUpcoming();
            case FAVORITE -> ipoStockRepository.findAttractivenessByFavorite();
        };

        items = sortAndLimitItems(tab, items);
        if (items.isEmpty()) return items;

        // 獄쏄퀣??鈺곌퀬?? ?ル굝??ID 筌뤴뫖以??곗쨮 雅뚯눊?????甕곕뜆肉?嚥≪뮆諭?
        List<Long> stockIds = items.stream().map(AttractivenessItem::ipoId).toList();
        Map<Long, String> leadManagerMap = ipoStockRepository.findUnderwritersByStockIds(stockIds)
                .entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> firstUnderwriter(entry.getValue())));

        // stockId ??筌?甕곕뜆??雅뚯눊???梨?筌띲끋釉?(displayOrder 筌ㅼ뮇?뽩첎?
        Map<Long, Integer> scoreByStockId = calculateHomeScores(items, currentProfileType(userId));

        return items.stream()
                .map(item -> new AttractivenessItem(
                        item.ipoId(),
                        item.name(),
                        scoreByStockId.getOrDefault(item.ipoId(), item.score()),
                        item.subscriptionStartDate(),
                        item.subscriptionEndDate(),
                        leadManagerMap.getOrDefault(item.ipoId(), "-"),
                        item.demandForecastDate(),
                        item.refundDate(),
                        item.listingDate()
                ))
                .toList();
    }

    private List<AttractivenessItem> sortAndLimitItems(HomeTab tab, List<AttractivenessItem> items) {
        return switch (tab) {
            case RECENT_GROWTH -> items.stream()
                    .limit(10)
                    .toList();
            case SUBSCRIPTION_UPCOMING -> {
                LocalDate today = LocalDate.now();
                yield items.stream()
                        .filter(item -> isSubscriptionActiveOrUpcoming(item, today))
                        .sorted(Comparator
                                .comparing((AttractivenessItem item) -> subscriptionUpcomingSortDate(item, today),
                                        Comparator.nullsLast(Comparator.naturalOrder()))
                                .thenComparing(AttractivenessItem::ipoId))
                        .limit(10)
                        .toList();
            }
            case FAVORITE -> items;
        };
    }

    private boolean isSubscriptionActiveOrUpcoming(AttractivenessItem item, LocalDate today) {
        LocalDate startDate = item.subscriptionStartDate();
        LocalDate endDate = item.subscriptionEndDate();

        if (endDate != null) {
            return !endDate.isBefore(today);
        }
        return startDate != null && !startDate.isBefore(today);
    }

    private LocalDate subscriptionUpcomingSortDate(AttractivenessItem item, LocalDate today) {
        LocalDate startDate = item.subscriptionStartDate();
        if (startDate == null || startDate.isBefore(today)) {
            return today;
        }
        return startDate;
    }

    private List<FeaturedIpoItem> getFeaturedIpos(Long userId) {
        List<FeaturedIpoCandidate> candidates = ipoStockRepository.findFeaturedIpoCandidates();
        if (candidates.isEmpty()) {
            return List.of();
        }

        LocalDate today = LocalDate.now();
        candidates = candidates.stream()
                .filter(candidate -> isFeaturedListingCandidate(candidate.listingDate(), today))
                .toList();
        if (candidates.isEmpty()) {
            return List.of();
        }

        Map<Long, Integer> scoreByStockId = calculateHomeScores(
                candidates.stream()
                        .map(this::toAttractivenessItem)
                        .toList(),
                currentProfileType(userId)
        );

        List<FeaturedIpoItem> featured = candidates.stream()
                .sorted(Comparator
                        .comparing((FeaturedIpoCandidate candidate) ->
                                scoreByStockId.getOrDefault(candidate.ipoId(), 0), Comparator.reverseOrder())
                        .thenComparing(FeaturedIpoCandidate::viewCount, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(FeaturedIpoCandidate::ipoId))
                .limit(5)
                .map(candidate -> new FeaturedIpoItem(
                        candidate.ipoId(),
                        0,
                        candidate.name(),
                        candidate.viewCount()
                ))
                .toList();

        return applyFeaturedRank(featured);
    }

    private boolean isFeaturedListingCandidate(LocalDate listingDate, LocalDate today) {
        if (listingDate == null) {
            return false;
        }

        long daysUntilListing = ChronoUnit.DAYS.between(today, listingDate);
        return daysUntilListing >= FEATURED_LISTING_START_DAYS
                && daysUntilListing <= FEATURED_LISTING_END_DAYS;
    }

    private AttractivenessItem toAttractivenessItem(FeaturedIpoCandidate candidate) {
        return new AttractivenessItem(
                candidate.ipoId(),
                candidate.name(),
                0,
                null,
                null,
                null,
                null,
                null,
                candidate.listingDate()
        );
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

    private Map<Long, Integer> calculateHomeScores(
            List<AttractivenessItem> items,
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
                        AttractivenessItem::ipoId,
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

    private AttractivenessIpoProjection fallbackAttractivenessProjection(AttractivenessItem item) {
        return new SimpleAttractivenessIpoProjection(item.ipoId(), item.name(), null, null, null, null);
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

    private List<FeaturedIpoItem> applyFeaturedRank(List<FeaturedIpoItem> items) {
        // 鈺곌퀬??野껉퀗???1,2,3... ??뽰맄???븐늿肉????揶쏆빘猿쒏에?獄쏆꼹??
        return IntStream.range(0, items.size())
                .mapToObj(i -> new FeaturedIpoItem(
                        items.get(i).ipoId(),
                        i + 1,
                        items.get(i).name(),
                        items.get(i).viewCount()
                ))
                .toList();
    }

    private List<TrendingIpoItem> applyTrendingRank(List<TrendingIpoItem> items) {
        // 疫뀀맧踰?野껉퀗??癒?즲 1,2,3... ??뽰맄???븐늿肉??獄쏆꼹??
        return IntStream.range(0, items.size())
                .mapToObj(i -> new TrendingIpoItem(
                        items.get(i).ipoId(),
                        i + 1,
                        items.get(i).name(),
                        items.get(i).changeRate(),
                        items.get(i).viewCount()
                ))
                .toList();
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
