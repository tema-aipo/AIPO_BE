package com.aipo.backend.domain.home.service;

import com.aipo.backend.domain.home.dto.*;
import com.aipo.backend.domain.home.type.HomeTab;
import com.aipo.backend.domain.investmentprofile.entity.InvestmentProfileType;
import com.aipo.backend.domain.investmentprofile.repository.UserInvestmentProfileResultRepository;
import com.aipo.backend.domain.ipo.entity.IpoLeadManager;
import com.aipo.backend.domain.ipo.repository.AttractivenessIpoProjection;
import com.aipo.backend.domain.ipo.repository.IpoLeadManagerRepository;
import com.aipo.backend.domain.ipo.repository.IpoStockRepository;
import com.aipo.backend.domain.ipo.service.AttractivenessService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service // 비즈니스 로직 처리 계층
@RequiredArgsConstructor
public class HomeService {

    private static final int FEATURED_LISTING_START_DAYS = 1;
    private static final int FEATURED_LISTING_END_DAYS = 7;

    private final IpoStockRepository ipoStockRepository;
    private final UserInvestmentProfileResultRepository userInvestmentProfileResultRepository;
    private final AttractivenessService attractivenessService;
    private final IpoLeadManagerRepository ipoLeadManagerRepository; // 추가

    public HomeResponse getHome(String tabValue, Long userId) {
        // 요청 파라미터 문자열을 enum으로 변환
        HomeTab tab = HomeTab.from(tabValue);

        // 홈 상단 대표 공모주 조회 후 순위 부여
        List<FeaturedIpoItem> featured =
                getFeaturedIpos(userId);

        // 실시간 조회 급등 조회 후 순위 부여
        List<TrendingIpoItem> trending =
                applyTrendingRank(ipoStockRepository.findTrendingIpos());

        // 탭에 따라 매력지수 리스트 조회
        List<AttractivenessItem> attractivenessItems =
                getAttractivenessItems(tab, userId);

        // 홈 화면 전체 응답 조합
        return new HomeResponse(
                featured,
                trending,
                new AttractivenessResponse(tab.getValue(), attractivenessItems)
        );
    }

    private List<AttractivenessItem> getAttractivenessItems(HomeTab tab, Long userId) {
        // 선택된 탭에 따라 다른 조회 메서드 호출
        List<AttractivenessItem> items = switch (tab) {
            case RECENT_GROWTH -> ipoStockRepository.findAttractivenessByRecentGrowth();
            case SUBSCRIPTION_UPCOMING -> ipoStockRepository.findAttractivenessBySubscriptionUpcoming();
            case FAVORITE -> ipoStockRepository.findAttractivenessByFavorite();
        };

        items = sortAndLimitItems(tab, items);
        if (items.isEmpty()) return items;

        // 배치 조회: 종목 ID 목록으로 주관사 한 번에 로드
        List<Long> stockIds = items.stream().map(AttractivenessItem::ipoId).toList();
        List<IpoLeadManager> managers = ipoLeadManagerRepository.findAllByStock_IdIn(stockIds);

        // stockId → 첫 번째 주관사명 매핑 (displayOrder 최솟값)
        Map<Long, String> leadManagerMap = new HashMap<>(managers.stream()
                .filter(manager -> hasTextValue(manager.getManagerName()))
                .collect(Collectors.toMap(
                        m -> m.getStock().getId(),
                        manager -> manager.getManagerName().trim(),
                        (existing, replacement) -> existing  // 중복 시 첫 번째 유지
                )));

        // 기존 items에 leadManager 주입하여 새 객체 생성
        ipoStockRepository.findUnderwritersByStockIds(stockIds)
                .forEach((stockId, underwriter) ->
                        leadManagerMap.putIfAbsent(stockId, firstUnderwriter(underwriter)));

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
                    .sorted(Comparator
                            .comparing(AttractivenessItem::listingDate, Comparator.nullsLast(Comparator.reverseOrder()))
                            .thenComparing(AttractivenessItem::ipoId))
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
        // 조회 결과에 1,2,3... 순위를 붙여서 새 객체로 반환
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
        // 급등 결과에도 1,2,3... 순위를 붙여서 반환
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
