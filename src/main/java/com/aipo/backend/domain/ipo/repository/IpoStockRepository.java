package com.aipo.backend.domain.ipo.repository;

import com.aipo.backend.domain.ipo.entity.IpoStock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.List;

public interface IpoStockRepository extends JpaRepository<IpoStock, Long>, IpoStockRepositoryCustom {

    Optional<IpoStock> findByDartCorpCode(String dartCorpCode);

    List<IpoStock> findAllByDartCorpCode(String dartCorpCode);

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
                m.company_name as companyName,
                m.stock_name as stockName,
                m.corp_name as corpName,
                m.stock_code as stockCode,
                m.offering_price as offeringPrice,
                m.confirmed_offer_price as confirmedOfferPrice,
                m.attract_score as attractScore,
                m.recent_growth_score as recentGrowthScore,
                m.market_type as marketType,
                m.one_line_description as oneLineDescription,
                m.underwriter as underwriter,
                m.subscription_date as subscriptionDate,
                m.demand_forecast_date as demandForecastDate,
                m.refund_date as refundDate,
                date_format(nullif(m.listing_date, '0000-00-00'), '%Y-%m-%d') as listingDate,
                date_format(nullif(m.subscription_start_date, '0000-00-00'), '%Y-%m-%d') as subscriptionStartDate,
                date_format(nullif(m.subscription_end_date, '0000-00-00'), '%Y-%m-%d') as subscriptionEndDate
            from ipo_main m
            where m.stock_id = :stockId
            """, nativeQuery = true)
    Optional<IpoDetailProjection> findDetailByStockId(@Param("stockId") Long stockId);

    @Query(value = """
            select
                m.stock_id as stockId,
                m.corp_name as corpName,
                cast(m.competition_ratio as char) as competitionRatio,
                cast(m.subscription_ratio as char) as subscriptionRatio,
                cast(m.inst_commitment_ratio as char) as instCommitmentRatio,
                cast(m.floating_stock_ratio as char) as floatingStockRatio,
                cast(m.lockup_total_ratio as char) as lockupTotalRatio
            from ipo_main m
            """, nativeQuery = true)
    List<AttractivenessIpoProjection> findAllForAttractiveness();
}
