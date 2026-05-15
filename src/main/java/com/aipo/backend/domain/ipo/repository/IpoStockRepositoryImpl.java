package com.aipo.backend.domain.ipo.repository;

import com.aipo.backend.domain.home.dto.AttractivenessItem;
import com.aipo.backend.domain.home.dto.FeaturedIpoItem;
import com.aipo.backend.domain.home.dto.TrendingIpoItem;
import com.aipo.backend.domain.ipo.dto.IpoListItem;
import com.aipo.backend.domain.ipo.entity.IpoStock;
import com.aipo.backend.domain.ipo.service.IpoStockViewMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public class IpoStockRepositoryImpl implements IpoStockRepositoryCustom {

    private static final String DEFAULT_SORT_EXPRESSION = "s.subscriptionStartDate";

    @PersistenceContext
    private EntityManager em;

    @Override
    public List<FeaturedIpoItem> findFeaturedIpos() {
        return em.createQuery("""
                select s, count(v.id)
                from IpoStock s
                left join IpoViewLog v on v.stock = s
                group by s
                order by count(v.id) desc,
                    coalesce(s.recentGrowthScore, 0) desc,
                    coalesce(s.attractScore, 0.0) desc,
                    s.createdAt desc
                """, Object[].class)
                .setMaxResults(5)
                .getResultList()
                .stream()
                .map(row -> {
                    IpoStock stock = (IpoStock) row[0];
                    Long viewCount = (Long) row[1];
                    return new FeaturedIpoItem(stock.getId(), 0, IpoStockViewMapper.displayName(stock), viewCount);
                })
                .toList();
    }

    @Override
    public List<TrendingIpoItem> findTrendingIpos() {
        return em.createQuery("""
                select s, count(v.id)
                from IpoStock s
                left join IpoViewLog v on v.stock = s
                group by s
                order by coalesce(s.recentGrowthScore, 0) desc,
                    coalesce(s.attractScore, 0.0) desc,
                    count(v.id) desc
                """, Object[].class)
                .setMaxResults(3)
                .getResultList()
                .stream()
                .map(row -> {
                    IpoStock stock = (IpoStock) row[0];
                    Long viewCount = (Long) row[1];
                    return new TrendingIpoItem(
                            stock.getId(),
                            0,
                            IpoStockViewMapper.displayName(stock),
                            IpoStockViewMapper.displayScore(stock),
                            viewCount
                    );
                })
                .toList();
    }

    @Override
    public List<AttractivenessItem> findAttractivenessByRecentGrowth() {
        return em.createQuery("""
                select s
                from IpoStock s
                order by coalesce(s.recentGrowthScore, 0) desc,
                    coalesce(s.attractScore, 0.0) desc,
                    s.createdAt desc
                """, IpoStock.class)
                .setMaxResults(10)
                .getResultList()
                .stream()
                .map(this::toAttractivenessItem)
                .toList();
    }

    @Override
    public List<AttractivenessItem> findAttractivenessBySubscriptionUpcoming() {
        List<IpoStock> stocks = em.createQuery("""
                select s
                from IpoStock s
                where s.subscriptionStartDate >= :today
                    or s.subscriptionEndDate >= :today
                order by coalesce(s.subscriptionStartDate, s.subscriptionEndDate) asc,
                    coalesce(s.recentGrowthScore, 0) desc,
                    coalesce(s.attractScore, 0.0) desc
                """, IpoStock.class)
                .setParameter("today", LocalDate.now())
                .setMaxResults(10)
                .getResultList();

        if (stocks.isEmpty()) {
            stocks = em.createQuery("""
                    select s
                    from IpoStock s
                    order by coalesce(s.recentGrowthScore, 0) desc,
                        coalesce(s.attractScore, 0.0) desc,
                        s.createdAt desc
                    """, IpoStock.class)
                    .setMaxResults(5)
                    .getResultList();
        }

        return stocks.stream()
                .map(this::toAttractivenessItem)
                .toList();
    }

    @Override
    public List<AttractivenessItem> findAttractivenessByFavorite() {
        return em.createQuery("""
                select s
                from IpoStock s
                order by (
                    select count(f.id)
                    from UserFavoriteStock f
                    where f.stock = s
                ) desc,
                coalesce(s.recentGrowthScore, 0) desc,
                coalesce(s.attractScore, 0.0) desc,
                s.createdAt desc
                """, IpoStock.class)
                .setMaxResults(10)
                .getResultList()
                .stream()
                .map(this::toAttractivenessItem)
                .toList();
    }

    @Override
    public List<IpoListItem> findIpoList(int page, int size, String keyword, String sort, String direction) {
        String orderBy = resolveSortExpression(sort) + " " + resolveDirection(direction) + ", s.id asc";
        String keywordPattern = toKeywordPattern(keyword);

        var query = em.createQuery("""
                select s, a.totalScore
                from IpoStock s
                left join IpoAttractionScore a
                    on a.stock = s
                    and a.calculatedAt = (
                        select max(a2.calculatedAt)
                        from IpoAttractionScore a2
                        where a2.stock = s
                    )
                where (:keyword is null
                    or lower(s.stockName) like :keyword
                    or lower(s.companyName) like :keyword
                    or lower(s.corpName) like :keyword)
                order by %s
                """.formatted(orderBy), Object[].class);

        query.setParameter("keyword", keywordPattern);
        return query
                .setFirstResult(page * size)
                .setMaxResults(size)
                .getResultList()
                .stream()
                .map(row -> toIpoListItem((IpoStock) row[0], (Integer) row[1]))
                .toList();
    }

    @Override
    public long countIpoList(String keyword) {
        String keywordPattern = toKeywordPattern(keyword);

        return em.createQuery("""
                select count(s.id)
                from IpoStock s
                where (:keyword is null
                    or lower(s.stockName) like :keyword
                    or lower(s.companyName) like :keyword
                    or lower(s.corpName) like :keyword)
                """, Long.class)
                .setParameter("keyword", keywordPattern)
                .getSingleResult();
    }

    private AttractivenessItem toAttractivenessItem(IpoStock stock) {
        return new AttractivenessItem(
                stock.getId(),
                IpoStockViewMapper.displayName(stock),
                IpoStockViewMapper.displayScore(stock),
                IpoStockViewMapper.subscriptionStartDate(stock),
                IpoStockViewMapper.subscriptionEndDate(stock),
                null,
                stock.getDemandForecastDate(),
                stock.getRefundDate()
        );
    }

    private IpoListItem toIpoListItem(IpoStock stock, Integer attractionScore) {
        return new IpoListItem(
                stock.getId(),
                IpoStockViewMapper.displayStockName(stock),
                IpoStockViewMapper.displayCompanyName(stock),
                stock.getMarketType(),
                stock.getOneLineDescription(),
                IpoStockViewMapper.offerPrice(stock),
                IpoStockViewMapper.subscriptionStartDate(stock),
                IpoStockViewMapper.subscriptionEndDate(stock),
                stock.getListingDate(),
                BigDecimal.valueOf(attractionScore != null ? attractionScore : IpoStockViewMapper.displayScore(stock)),
                IpoStockViewMapper.displayScore(stock),
                stock.getDemandForecastDate(),
                stock.getRefundDate()
        );
    }

    private String resolveSortExpression(String sort) {
        if (sort == null || sort.isBlank()) {
            return DEFAULT_SORT_EXPRESSION;
        }

        return switch (sort) {
            case "subscriptionStartDate" -> "s.subscriptionStartDate";
            case "subscriptionEndDate" -> "s.subscriptionEndDate";
            case "listingDate" -> "s.listingDate";
            case "confirmedOfferPrice" -> "s.confirmedOfferPrice";
            case "attractionScore" -> "coalesce(s.recentGrowthScore, 0)";
            case "recentGrowthScore" -> "coalesce(s.recentGrowthScore, 0)";
            case "stockName" -> "s.stockName";
            case "companyName" -> "s.companyName";
            default -> DEFAULT_SORT_EXPRESSION;
        };
    }

    private String resolveDirection(String direction) {
        if ("desc".equalsIgnoreCase(direction)) {
            return "desc";
        }
        return "asc";
    }

    private String toKeywordPattern(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        return "%" + keyword.trim().toLowerCase() + "%";
    }
}
