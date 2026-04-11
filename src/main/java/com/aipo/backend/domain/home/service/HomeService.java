package com.aipo.backend.domain.home.service;

import com.aipo.backend.domain.home.dto.*;
import com.aipo.backend.domain.home.type.HomeTab;
import com.aipo.backend.domain.ipo.repository.IpoStockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.IntStream;

@Service // 비즈니스 로직 처리 계층
@RequiredArgsConstructor
public class HomeService {

    private final IpoStockRepository ipoStockRepository;

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
        return switch (tab) {
            case RECENT_GROWTH -> ipoStockRepository.findAttractivenessByRecentGrowth();
            case SUBSCRIPTION_UPCOMING -> ipoStockRepository.findAttractivenessBySubscriptionUpcoming();
            case FAVORITE -> ipoStockRepository.findAttractivenessByFavorite();
        };
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