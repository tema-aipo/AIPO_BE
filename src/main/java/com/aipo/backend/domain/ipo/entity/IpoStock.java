package com.aipo.backend.domain.ipo.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "ipo_main")
public class IpoStock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "stock_id", columnDefinition = "INT UNSIGNED")
    private Long id;

    @Column(name = "stock_name", length = 100)
    private String stockName;

    @Column(name = "company_name", length = 100)
    private String companyName;

    @Column(name = "corp_name", length = 100)
    private String corpName;

    @Column(name = "stock_code", length = 20)
    private String stockCode;

    @Column(name = "dart_corp_code", length = 8)
    private String dartCorpCode;

    @Column(name = "market_type", length = 20)
    private String marketType;

    @Column(name = "one_line_description", length = 255)
    private String oneLineDescription;

    @Column(name = "confirmed_offer_price", precision = 15, scale = 2)
    private BigDecimal confirmedOfferPrice;

    @Column(name = "offering_price")
    private Integer offeringPrice;

    @Column(name = "subscription_start_date")
    private LocalDate subscriptionStartDate;

    @Column(name = "subscription_end_date")
    private LocalDate subscriptionEndDate;

    @Column(name = "listing_date")
    private LocalDate listingDate;

    @Column(name = "recent_growth_score")
    private Integer recentGrowthScore;

    @Column(name = "attract_score")
    private Float attractScore;

    @Column(name = "subscription_date", length = 50)
    private String subscriptionDate;

    @Column(name = "demand_forecast_date", length = 50)
    private String demandForecastDate;

    @Column(name = "refund_date", length = 50)
    private String refundDate;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public static IpoStock createFromExternal(
            String stockName,
            String companyName,
            String stockCode,
            String dartCorpCode,
            String marketType,
            BigDecimal confirmedOfferPrice,
            LocalDate subscriptionStartDate,
            LocalDate subscriptionEndDate
    ) {
        IpoStock stock = new IpoStock();
        stock.stockName = stockName;
        stock.companyName = companyName;
        stock.stockCode = stockCode;
        stock.dartCorpCode = dartCorpCode;
        stock.marketType = marketType;
        stock.oneLineDescription = "공모 정보 수집 중";
        stock.confirmedOfferPrice = confirmedOfferPrice;
        stock.subscriptionStartDate = subscriptionStartDate;
        stock.subscriptionEndDate = subscriptionEndDate;
        stock.recentGrowthScore = 0;
        stock.createdAt = LocalDateTime.now();
        stock.updatedAt = stock.createdAt;
        return stock;
    }

    public void updateFromExternal(
            String stockName,
            String companyName,
            String stockCode,
            String marketType,
            BigDecimal confirmedOfferPrice,
            LocalDate subscriptionStartDate,
            LocalDate subscriptionEndDate
    ) {
        if (stockName != null && !stockName.isBlank()) {
            this.stockName = stockName;
        }
        if (companyName != null && !companyName.isBlank()) {
            this.companyName = companyName;
        }
        if (stockCode != null && !stockCode.isBlank()) {
            this.stockCode = stockCode;
        }
        if (isBetterMarketType(marketType)) {
            this.marketType = marketType;
        }
        if (confirmedOfferPrice != null) {
            this.confirmedOfferPrice = confirmedOfferPrice;
        }
        if (subscriptionStartDate != null) {
            this.subscriptionStartDate = subscriptionStartDate;
        }
        if (subscriptionEndDate != null) {
            this.subscriptionEndDate = subscriptionEndDate;
        }
        this.updatedAt = LocalDateTime.now();
    }

    public void supplementFromKind(String stockCode, String marketType, LocalDate listingDate) {
        if (stockCode != null && !stockCode.isBlank()) {
            this.stockCode = stockCode;
        }
        if (marketType != null && !marketType.isBlank()) {
            this.marketType = marketType;
        }
        if (listingDate != null) {
            this.listingDate = listingDate;
        }
        this.updatedAt = LocalDateTime.now();
    }

    private boolean isBetterMarketType(String marketType) {
        if (marketType == null || marketType.isBlank()) {
            return false;
        }
        if (this.marketType == null || this.marketType.isBlank()) {
            return true;
        }
        return !"OTHER".equalsIgnoreCase(marketType) || "OTHER".equalsIgnoreCase(this.marketType);
    }
}
