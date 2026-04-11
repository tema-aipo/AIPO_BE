package com.aipo.backend.domain.ipo.repository;

import com.aipo.backend.domain.home.dto.AttractivenessItem;
import com.aipo.backend.domain.home.dto.FeaturedIpoItem;
import com.aipo.backend.domain.home.dto.TrendingIpoItem;

import java.util.List;

// 홈 API에서 필요한 전용 조회 메서드를 선언하는 인터페이스
public interface IpoStockRepositoryCustom {

    // 홈 상단 대표 공모주 조회
    List<FeaturedIpoItem> findFeaturedIpos();

    // 실시간 조회 급등 조회
    List<TrendingIpoItem> findTrendingIpos();

    // 최근 성장순 매력지수 리스트 조회
    List<AttractivenessItem> findAttractivenessByRecentGrowth();

    // 청약 예정순 매력지수 리스트 조회
    List<AttractivenessItem> findAttractivenessBySubscriptionUpcoming();

    // 관심 종목순 매력지수 리스트 조회
    List<AttractivenessItem> findAttractivenessByFavorite();
}