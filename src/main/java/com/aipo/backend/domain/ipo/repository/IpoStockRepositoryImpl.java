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
import java.util.Map;
import java.util.stream.Collectors;

@Repository
public class IpoStockRepositoryImpl implements IpoStockRepositoryCustom {

    private static final String DEFAULT_SORT_EXPRESSION = "s.subscriptionStartDate";

    @PersistenceContext
    private EntityManager em;

    @Override
    public List<FeaturedIpoItem> findFeaturedIpos() {
        return em.createNativeQuery("""
                select
                    m.stock_id,
                    m.company_name,
                    m.stock_name,
                    m.corp_name,
                    count(v.view_log_id) as view_count
                from ipo_main m
                left join ipo_view_log v on v.stock_id = m.stock_id
                group by m.stock_id, m.company_name, m.stock_name, m.corp_name,
                    m.recent_growth_score, m.attract_score, m.created_at
                order by count(v.view_log_id) desc,
                    coalesce(m.recent_growth_score, 0) desc,
                    coalesce(m.attract_score, 0) desc,
                    m.created_at desc
                """)
                .setMaxResults(5)
                .getResultList()
                .stream()
                .map(row -> toFeaturedIpoItem((Object[]) row))
                .toList();
    }

    @Override
    public List<TrendingIpoItem> findTrendingIpos() {
        return em.createNativeQuery("""
                select
                    m.stock_id,
                    m.company_name,
                    m.stock_name,
                    m.corp_name,
                    m.recent_growth_score,
                    m.attract_score,
                    count(v.view_log_id) as view_count
                from ipo_main m
                left join ipo_view_log v on v.stock_id = m.stock_id
                group by m.stock_id, m.company_name, m.stock_name, m.corp_name,
                    m.recent_growth_score, m.attract_score
                order by coalesce(m.recent_growth_score, 0) desc,
                    coalesce(m.attract_score, 0) desc,
                    count(v.view_log_id) desc
                """)
                .setMaxResults(3)
                .getResultList()
                .stream()
                .map(row -> toTrendingIpoItem((Object[]) row))
                .toList();
    }

    @Override
    public List<AttractivenessItem> findAttractivenessByRecentGrowth() {
        return em.createNativeQuery("""
                select
                    m.stock_id,
                    m.company_name,
                    m.stock_name,
                    m.corp_name,
                    m.recent_growth_score,
                    m.attract_score,
                    date_format(nullif(m.subscription_start_date, '0000-00-00'), '%Y-%m-%d') as subscription_start_date,
                    date_format(nullif(m.subscription_end_date, '0000-00-00'), '%Y-%m-%d') as subscription_end_date,
                    m.subscription_date,
                    m.demand_forecast_date,
                    m.refund_date
                from ipo_main m
                order by coalesce(m.recent_growth_score, 0) desc,
                    coalesce(m.attract_score, 0) desc,
                    m.created_at desc
                """)
                .setMaxResults(10)
                .getResultList()
                .stream()
                .map(row -> toAttractivenessItem((Object[]) row))
                .toList();
    }

    @Override
    public List<AttractivenessItem> findAttractivenessBySubscriptionUpcoming() {
        List<Object[]> rows = em.createNativeQuery("""
                select
                    m.stock_id,
                    m.company_name,
                    m.stock_name,
                    m.corp_name,
                    m.recent_growth_score,
                    m.attract_score,
                    date_format(nullif(m.subscription_start_date, '0000-00-00'), '%Y-%m-%d') as subscription_start_date,
                    date_format(nullif(m.subscription_end_date, '0000-00-00'), '%Y-%m-%d') as subscription_end_date,
                    m.subscription_date,
                    m.demand_forecast_date,
                    m.refund_date
                from ipo_main m
                where nullif(m.subscription_start_date, '0000-00-00') >= :today
                    or nullif(m.subscription_end_date, '0000-00-00') >= :today
                order by coalesce(nullif(m.subscription_start_date, '0000-00-00'), nullif(m.subscription_end_date, '0000-00-00')) asc,
                    coalesce(m.recent_growth_score, 0) desc,
                    coalesce(m.attract_score, 0) desc
                """)
                .setParameter("today", LocalDate.now())
                .setMaxResults(10)
                .getResultList();

        if (rows.isEmpty()) {
            rows = em.createNativeQuery("""
                    select
                        m.stock_id,
                        m.company_name,
                        m.stock_name,
                        m.corp_name,
                        m.recent_growth_score,
                        m.attract_score,
                        date_format(nullif(m.subscription_start_date, '0000-00-00'), '%Y-%m-%d') as subscription_start_date,
                        date_format(nullif(m.subscription_end_date, '0000-00-00'), '%Y-%m-%d') as subscription_end_date,
                        m.subscription_date,
                        m.demand_forecast_date,
                        m.refund_date
                    from ipo_main m
                    order by coalesce(m.recent_growth_score, 0) desc,
                        coalesce(m.attract_score, 0) desc,
                        m.created_at desc
                    """)
                    .setMaxResults(5)
                    .getResultList();
        }

        return rows.stream()
                .map(row -> toAttractivenessItem((Object[]) row))
                .toList();
    }

    @Override
    public List<AttractivenessItem> findAttractivenessByFavorite() {
        return em.createNativeQuery("""
                select
                    m.stock_id,
                    m.company_name,
                    m.stock_name,
                    m.corp_name,
                    m.recent_growth_score,
                    m.attract_score,
                    date_format(nullif(m.subscription_start_date, '0000-00-00'), '%Y-%m-%d') as subscription_start_date,
                    date_format(nullif(m.subscription_end_date, '0000-00-00'), '%Y-%m-%d') as subscription_end_date,
                    m.subscription_date,
                    m.demand_forecast_date,
                    m.refund_date
                from ipo_main m
                order by (
                    select count(f.favorite_id)
                    from user_favorite_stock f
                    where f.stock_id = m.stock_id
                ) desc,
                coalesce(m.recent_growth_score, 0) desc,
                coalesce(m.attract_score, 0) desc,
                m.created_at desc
                """)
                .setMaxResults(10)
                .getResultList()
                .stream()
                .map(row -> toAttractivenessItem((Object[]) row))
                .toList();
    }

    @Override
    public List<IpoListItem> findIpoList(int page, int size, String keyword, String sort, String direction) {
        String orderBy = resolveSortExpression(sort) + " " + resolveDirection(direction) + ", s.id asc";
        String keywordPattern = toKeywordPattern(keyword);

        var query = em.createQuery("""
                select s
                from IpoStock s
                where (:keyword is null
                    or lower(s.corpName) like :keyword
                    or lower(s.stockCode) like :keyword)
                order by %s
                """.formatted(orderBy), IpoStock.class);

        query.setParameter("keyword", keywordPattern);
        return query
                .setFirstResult(page * size)
                .setMaxResults(size)
                .getResultList()
                .stream()
                .map(this::toIpoListItem)
                .toList();
    }

    @Override
    public long countIpoList(String keyword) {
        String keywordPattern = toKeywordPattern(keyword);

        return em.createQuery("""
                select count(s.id)
                from IpoStock s
                where (:keyword is null
                    or lower(s.corpName) like :keyword
                    or lower(s.stockCode) like :keyword)
                """, Long.class)
                .setParameter("keyword", keywordPattern)
                .getSingleResult();
    }

    @Override
    public Map<Long, String> findUnderwritersByStockIds(List<Long> stockIds) {
        if (stockIds == null || stockIds.isEmpty()) {
            return Map.of();
        }

        List<?> rows = em.createNativeQuery("""
                select m.stock_id, m.underwriter
                from ipo_main m
                where m.stock_id in (:stockIds)
                """)
                .setParameter("stockIds", stockIds)
                .getResultList();

        return rows.stream()
                .map(row -> (Object[]) row)
                .filter(row -> row[1] != null && !row[1].toString().isBlank())
                .collect(Collectors.toMap(
                        row -> toLong(row[0]),
                        row -> row[1].toString(),
                        (existing, replacement) -> existing
                ));
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

    private IpoListItem toIpoListItem(IpoStock stock) {
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
                BigDecimal.valueOf(IpoStockViewMapper.displayScore(stock)),
                stock.getRecentGrowthScore(),
                stock.getDemandForecastDate(),
                stock.getRefundDate()
        );
    }

    private FeaturedIpoItem toFeaturedIpoItem(Object[] row) {
        return new FeaturedIpoItem(
                toLong(row[0]),
                0,
                IpoStockViewMapper.displayName(toString(row[1]), toString(row[2]), toString(row[3])),
                toLong(row[4])
        );
    }

    private TrendingIpoItem toTrendingIpoItem(Object[] row) {
        return new TrendingIpoItem(
                toLong(row[0]),
                0,
                IpoStockViewMapper.displayName(toString(row[1]), toString(row[2]), toString(row[3])),
                displayScore(row[4], row[5]),
                toLong(row[4])
        );
    }

    private AttractivenessItem toAttractivenessItem(Object[] row) {
        LocalDate startDate = IpoStockViewMapper.parseIsoDate(toString(row[6]));
        LocalDate endDate = IpoStockViewMapper.parseIsoDate(toString(row[7]));
        String subscriptionDate = toString(row[8]);

        return new AttractivenessItem(
                toLong(row[0]),
                IpoStockViewMapper.displayName(toString(row[1]), toString(row[2]), toString(row[3])),
                displayScore(row[4], row[5]),
                startDate != null ? startDate : IpoStockViewMapper.parseSubscriptionDateText(subscriptionDate, 0),
                endDate != null ? endDate : IpoStockViewMapper.parseSubscriptionDateText(subscriptionDate, 1),
                null,
                toString(row[9]),
                toString(row[10])
        );
    }

    private Integer displayScore(Object recentGrowthScore, Object attractScore) {
        Float attract = toFloat(attractScore);
        if (attract != null) {
            return Math.round(attract);
        }
        Integer recentGrowth = toInteger(recentGrowthScore);
        if (recentGrowth != null) {
            return recentGrowth;
        }
        return 0;
    }

    private Long toLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return null;
    }

    private Integer toInteger(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        return null;
    }

    private Float toFloat(Object value) {
        if (value instanceof Number number) {
            return number.floatValue();
        }
        return null;
    }

    private String toString(Object value) {
        return value == null ? null : value.toString();
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
            case "attractionScore" -> "coalesce(s.attractScore, 0)";
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
