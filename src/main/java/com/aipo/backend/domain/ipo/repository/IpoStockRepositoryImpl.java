package com.aipo.backend.domain.ipo.repository;

import com.aipo.backend.domain.home.dto.AttractivenessItem;
import com.aipo.backend.domain.home.dto.FeaturedIpoItem;
import com.aipo.backend.domain.home.dto.TrendingIpoItem;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class IpoStockRepositoryImpl implements IpoStockRepositoryCustom {

    @PersistenceContext
    private EntityManager em;

    @Override
    public List<FeaturedIpoItem> findFeaturedIpos() {
        // 조회 로그를 기준으로 많이 본 공모주를 상단 대표 종목으로 사용
        // 순위는 서비스에서 1,2 형태로 다시 붙인다.
        return em.createQuery("""
                select new com.aipo.backend.domain.home.dto.FeaturedIpoItem(
                    s.id,
                    0,
                    s.stockName,
                    count(v.id)
                )
                from IpoViewLog v
                join v.stock s
                group by s.id, s.stockName
                order by count(v.id) desc
                """, FeaturedIpoItem.class)
                .setMaxResults(2)
                .getResultList();
    }

    @Override
    public List<TrendingIpoItem> findTrendingIpos() {
        // 지금 버전에서는 recentGrowthScore를 changeRate처럼 사용한다.
        // 이후 실제 로그 비교 로직으로 바꿀 수 있다.
        return em.createQuery("""
                select new com.aipo.backend.domain.home.dto.TrendingIpoItem(
                    s.id,
                    0,
                    s.stockName,
                    coalesce(s.recentGrowthScore, 0),
                    count(v.id)
                )
                from IpoStock s
                left join IpoViewLog v on v.stock = s
                group by s.id, s.stockName, s.recentGrowthScore
                order by coalesce(s.recentGrowthScore, 0) desc, count(v.id) desc
                """, TrendingIpoItem.class)
                .setMaxResults(3)
                .getResultList();
    }

    @Override
    public List<AttractivenessItem> findAttractivenessByRecentGrowth() {
        // 최근 성장순 탭:
        // 1) recentGrowthScore 높은 순
        // 2) 같은 경우 totalScore 높은 순
        return em.createQuery("""
                select new com.aipo.backend.domain.home.dto.AttractivenessItem(
                    s.id,
                    s.stockName,
                    a.totalScore,
                    s.subscriptionStartDate,
                    s.subscriptionEndDate
                )
                from IpoAttractionScore a
                join a.stock s
                where a.calculatedAt = (
                    select max(a2.calculatedAt)
                    from IpoAttractionScore a2
                    where a2.stock = s
                )
                order by coalesce(s.recentGrowthScore, 0) desc, a.totalScore desc
                """, AttractivenessItem.class)
                .setMaxResults(10)
                .getResultList();
    }

    @Override
    public List<AttractivenessItem> findAttractivenessBySubscriptionUpcoming() {
        // 청약 예정순 탭:
        // 청약 시작일이 빠른 순으로 정렬
        return em.createQuery("""
                select new com.aipo.backend.domain.home.dto.AttractivenessItem(
                    s.id,
                    s.stockName,
                    a.totalScore,
                    s.subscriptionStartDate,
                    s.subscriptionEndDate
                )
                from IpoAttractionScore a
                join a.stock s
                where a.calculatedAt = (
                    select max(a2.calculatedAt)
                    from IpoAttractionScore a2
                    where a2.stock = s
                )
                order by s.subscriptionStartDate asc, a.totalScore desc
                """, AttractivenessItem.class)
                .setMaxResults(10)
                .getResultList();
    }

    @Override
    public List<AttractivenessItem> findAttractivenessByFavorite() {
        // 관심 종목순 탭:
        // user_favorite_stock에 많이 등록된 종목부터 정렬
        return em.createQuery("""
                select new com.aipo.backend.domain.home.dto.AttractivenessItem(
                    s.id,
                    s.stockName,
                    a.totalScore,
                    s.subscriptionStartDate,
                    s.subscriptionEndDate
                )
                from IpoAttractionScore a
                join a.stock s
                where a.calculatedAt = (
                    select max(a2.calculatedAt)
                    from IpoAttractionScore a2
                    where a2.stock = s
                )
                order by (
                    select count(f.id)
                    from UserFavoriteStock f
                    where f.stock = s
                ) desc, a.totalScore desc
                """, AttractivenessItem.class)
                .setMaxResults(10)
                .getResultList();
    }
}