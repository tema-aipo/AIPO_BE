package com.aipo.backend.domain.external.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "ipo_external_source_data",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_ipo_external_source_data_key",
                        columnNames = {"provider", "source_type", "external_key"}
                )
        }
)
public class IpoExternalSourceData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ipo_external_source_data_id")
    private Long id;

    @Column(name = "provider", nullable = false, length = 30)
    private String provider;

    @Column(name = "source_type", nullable = false, length = 80)
    private String sourceType;

    @Column(name = "external_key", nullable = false, length = 120)
    private String externalKey;

    @Column(name = "corp_name", length = 100)
    private String corpName;

    @Column(name = "dart_corp_code", length = 8)
    private String dartCorpCode;

    @Column(name = "stock_code", length = 20)
    private String stockCode;

    @Column(name = "market_type", length = 20)
    private String marketType;

    @Column(name = "subscription_start_date")
    private LocalDate subscriptionStartDate;

    @Column(name = "subscription_end_date")
    private LocalDate subscriptionEndDate;

    @Column(name = "demand_forecast_start_date")
    private LocalDate demandForecastStartDate;

    @Column(name = "demand_forecast_end_date")
    private LocalDate demandForecastEndDate;

    @Column(name = "refund_date")
    private LocalDate refundDate;

    @Column(name = "listing_date")
    private LocalDate listingDate;

    @Column(name = "confirmed_offer_price", precision = 15, scale = 2)
    private BigDecimal confirmedOfferPrice;

    @Column(name = "lead_managers", columnDefinition = "TEXT")
    private String leadManagers;

    @Column(name = "raw_response_id")
    private Long rawResponseId;

    @Column(name = "confidence", nullable = false)
    private Integer confidence;

    @Column(name = "collected_at", nullable = false)
    private LocalDateTime collectedAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public static IpoExternalSourceData create(
            String provider,
            String sourceType,
            String externalKey
    ) {
        IpoExternalSourceData sourceData = new IpoExternalSourceData();
        sourceData.provider = provider;
        sourceData.sourceType = sourceType;
        sourceData.externalKey = externalKey;
        sourceData.confidence = 50;
        sourceData.collectedAt = LocalDateTime.now();
        sourceData.updatedAt = sourceData.collectedAt;
        return sourceData;
    }

    public void updateData(
            String corpName,
            String dartCorpCode,
            String stockCode,
            String marketType,
            LocalDate subscriptionStartDate,
            LocalDate subscriptionEndDate,
            LocalDate demandForecastStartDate,
            LocalDate demandForecastEndDate,
            LocalDate refundDate,
            LocalDate listingDate,
            BigDecimal confirmedOfferPrice,
            String leadManagers,
            Long rawResponseId,
            Integer confidence
    ) {
        this.corpName = corpName;
        this.dartCorpCode = dartCorpCode;
        this.stockCode = stockCode;
        this.marketType = marketType;
        this.subscriptionStartDate = subscriptionStartDate;
        this.subscriptionEndDate = subscriptionEndDate;
        this.demandForecastStartDate = demandForecastStartDate;
        this.demandForecastEndDate = demandForecastEndDate;
        this.refundDate = refundDate;
        this.listingDate = listingDate;
        this.confirmedOfferPrice = confirmedOfferPrice;
        this.leadManagers = leadManagers;
        this.rawResponseId = rawResponseId;
        this.confidence = confidence == null ? 50 : confidence;
        this.updatedAt = LocalDateTime.now();
    }
}
