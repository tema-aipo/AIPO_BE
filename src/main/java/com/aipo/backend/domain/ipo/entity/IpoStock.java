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
@Table(name = "ipo_stock")
public class IpoStock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "stock_id")
    private Long id;

    @Column(name = "stock_name", nullable = false, length = 100)
    private String stockName;

    @Column(name = "company_name", nullable = false, length = 100)
    private String companyName;

    @Column(name = "stock_code", length = 20)
    private String stockCode;

    @Column(name = "market_type", length = 20)
    private String marketType;

    @Column(name = "one_line_description", length = 255)
    private String oneLineDescription;

    @Column(name = "confirmed_offer_price", precision = 15, scale = 2)
    private BigDecimal confirmedOfferPrice;

    @Column(name = "subscription_start_date")
    private LocalDate subscriptionStartDate;

    @Column(name = "subscription_end_date")
    private LocalDate subscriptionEndDate;

    @Column(name = "listing_date")
    private LocalDate listingDate;

    @Column(name = "recent_growth_score")
    private Integer recentGrowthScore;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
