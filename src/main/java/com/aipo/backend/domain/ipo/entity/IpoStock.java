package com.aipo.backend.domain.ipo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
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

    @Column(name = "corp_name", length = 100)
    private String corpName;

    @Column(name = "corp_code", length = 20)
    private String corpCode;

    @Column(name = "stock_code", length = 20)
    private String stockCode;

    @Column(name = "market_type", length = 20)
    private String marketType;

    @Column(name = "one_line_description", length = 255)
    private String oneLineDescription;

    @Column(name = "offering_price")
    private Integer offeringPrice;

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
        stock.corpName = firstText(companyName, stockName);
        stock.stockCode = stockCode;
        stock.corpCode = dartCorpCode;
        stock.marketType = marketType;
        stock.oneLineDescription = "공모 정보 수집 중";
        stock.offeringPrice = toIntegerPrice(confirmedOfferPrice);
        stock.subscriptionDate = formatDateRange(subscriptionStartDate, subscriptionEndDate);
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
        String resolvedCorpName = firstText(companyName, stockName);
        if (resolvedCorpName != null) {
            this.corpName = resolvedCorpName;
        }
        if (stockCode != null && !stockCode.isBlank()) {
            this.stockCode = stockCode;
        }
        if (isBetterMarketType(marketType)) {
            this.marketType = marketType;
        }
        Integer resolvedOfferingPrice = toIntegerPrice(confirmedOfferPrice);
        if (resolvedOfferingPrice != null) {
            this.offeringPrice = resolvedOfferingPrice;
        }
        String resolvedSubscriptionDate = formatDateRange(subscriptionStartDate, subscriptionEndDate);
        if (resolvedSubscriptionDate != null) {
            this.subscriptionDate = resolvedSubscriptionDate;
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

    public String getStockName() {
        return firstText(corpName, stockCode);
    }

    public String getCompanyName() {
        return corpName;
    }

    public String getDartCorpCode() {
        return corpCode;
    }

    public BigDecimal getConfirmedOfferPrice() {
        return offeringPrice == null ? null : BigDecimal.valueOf(offeringPrice);
    }

    public LocalDate getSubscriptionStartDate() {
        return parseSubscriptionDate(0);
    }

    public LocalDate getSubscriptionEndDate() {
        LocalDate endDate = parseSubscriptionDate(1);
        return endDate != null ? endDate : parseSubscriptionDate(0);
    }

    private LocalDate parseSubscriptionDate(int index) {
        return com.aipo.backend.domain.ipo.service.IpoStockViewMapper.parseSubscriptionDateText(subscriptionDate, index);
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

    private static Integer toIntegerPrice(BigDecimal price) {
        return price == null ? null : price.intValue();
    }

    private static String formatDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null && endDate == null) {
            return null;
        }
        if (startDate == null) {
            return endDate.toString();
        }
        if (endDate == null || startDate.equals(endDate)) {
            return startDate.toString();
        }
        return startDate + " ~ " + endDate;
    }

    private static String firstText(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }
}
