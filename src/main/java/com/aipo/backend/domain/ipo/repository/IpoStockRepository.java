package com.aipo.backend.domain.ipo.repository;

import com.aipo.backend.domain.ipo.entity.IpoStock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.List;

public interface IpoStockRepository extends JpaRepository<IpoStock, Long>, IpoStockRepositoryCustom {

    @Query("select s from IpoStock s where s.corpCode = :dartCorpCode")
    Optional<IpoStock> findByDartCorpCode(@Param("dartCorpCode") String dartCorpCode);

    @Query("select s from IpoStock s where s.corpCode = :dartCorpCode")
    List<IpoStock> findAllByDartCorpCode(@Param("dartCorpCode") String dartCorpCode);

    @Modifying
    @Query(value = """
            update ipo_main
            set recent_growth_score = coalesce(recent_growth_score, 0) + 1
            where stock_id = :stockId
            """, nativeQuery = true)
    int incrementRecentGrowthScore(@Param("stockId") Long stockId);

    @Query(value = """
            select
                m.stock_id as stockId,
                m.corp_name as companyName,
                coalesce(m.corp_name, m.stock_code) as stockName,
                m.corp_name as corpName,
                m.stock_code as stockCode,
                m.offering_price as offeringPrice,
                m.offering_price as confirmedOfferPrice,
                m.attract_score as attractScore,
                m.recent_growth_score as recentGrowthScore,
                m.market_type as marketType,
                m.one_line_description as oneLineDescription,
                m.underwriter as underwriter,
                m.subscription_date as subscriptionDate,
                m.demand_forecast_date as demandForecastDate,
                m.refund_date as refundDate,
                date_format(nullif(m.listing_date, '0000-00-00'), '%Y-%m-%d') as listingDate,
                null as subscriptionStartDate,
                null as subscriptionEndDate
            from ipo_main m
            where m.stock_id = :stockId
            """, nativeQuery = true)
    Optional<IpoDetailProjection> findDetailByStockId(@Param("stockId") Long stockId);

    @Query(value = """
            select
                m.stock_id as stockId,
                m.corp_name as corpName,
                m.stock_code as stockCode,
                m.attract_score as attractScore,
                m.demand_forecast_date as demandForecastDate,
                m.subscription_date as subscriptionDate,
                m.refund_date as refundDate,
                nullif(m.listing_date, '0000-00-00') as listingDate
            from ipo_main m
            """, nativeQuery = true)
    List<CalendarIpoProjection> findAllForCalendar();

    @Query(value = """
            select
                m.stock_id as stockId,
                m.corp_name as corpName,
                cast(m.competition_ratio as char) as competitionRatio,
                cast(m.inst_commitment_ratio as char) as instCommitmentRatio,
                cast(m.floating_stock_ratio as char) as floatingStockRatio,
                cast(m.lockup_total_ratio as char) as lockupTotalRatio
            from ipo_main m
            """, nativeQuery = true)
    List<AttractivenessIpoProjection> findAllForAttractiveness();
}
