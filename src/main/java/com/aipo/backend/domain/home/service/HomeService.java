package com.aipo.backend.domain.home.service;

import com.aipo.backend.domain.home.dto.*;
import com.aipo.backend.domain.home.type.HomeTab;
import com.aipo.backend.domain.ipo.entity.IpoLeadManager;
import com.aipo.backend.domain.ipo.repository.IpoLeadManagerRepository;
import com.aipo.backend.domain.ipo.repository.IpoStockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service // 비즈니스 로직 처리 계층
@RequiredArgsConstructor
public class HomeService {

    private final IpoStockRepository ipoStockRepository;
    private final IpoLeadManagerRepository ipoLeadManagerRepository; // 추가

    public HomeResponse getHome(String tabValue) {
        // 요청 파라미터 문자열을 enum으로 변환
        HomeTab tab = HomeTab.from(tabValue);

        // 홈 상단 대표 공모주 조회 후 순위 부여
        List<FeaturedIpoItem> featured =
                applyFeaturedRank(ipoStockRepository.findFeaturedIpos());

        // 실시간 조회 급등 조회 후 순위 부여
        List<TrendingIpoItem> trending =
                applyTrendingRank(ipoStockRepository.findTrendingIpos());

        // 탭에 따라 매력지수 리스트 조회
        List<AttractivenessItem> attractivenessItems =
                getAttractivenessItems(tab);

        // 홈 화면 전체 응답 조합
        return new HomeResponse(
                featured,
                trending,
                new AttractivenessResponse(tab.getValue(), attractivenessItems)
        );
    }

    private List<AttractivenessItem> getAttractivenessItems(HomeTab tab) {
        // 선택된 탭에 따라 다른 조회 메서드 호출
        List<AttractivenessItem> items = switch (tab) {
            case RECENT_GROWTH -> ipoStockRepository.findAttractivenessByRecentGrowth();
            case SUBSCRIPTION_UPCOMING -> ipoStockRepository.findAttractivenessBySubscriptionUpcoming();
            case FAVORITE -> ipoStockRepository.findAttractivenessByFavorite();
        };

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

        return items.stream()
                .map(item -> new AttractivenessItem(
                        item.ipoId(),
                        item.name(),
                        item.score(),
                        item.subscriptionStartDate(),
                        item.subscriptionEndDate(),
                        leadManagerMap.getOrDefault(item.ipoId(), "-"),
                        item.demandForecastDate(),
                        item.refundDate(),
                        item.listingDate()
                ))
                .toList();
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
}
