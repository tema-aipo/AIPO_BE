package com.aipo.backend.domain.ipo.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "ipo_stock")
public class IpoStock {

    // 공모주 PK
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "stock_id")
    private Long id;

    // 종목명
    @Column(name = "stock_name", nullable = false, length = 100)
    private String stockName;

    // 기업명
    @Column(name = "company_name", nullable = false, length = 100)
    private String companyName;

    // 종목 코드
    @Column(name = "stock_code", length = 20)
    private String stockCode;

    // 시장 구분 (KOSPI, KOSDAQ 등)
    @Column(name = "market_type", length = 20)
    private String marketType;

    // 청약 시작일
    @Column(name = "subscription_start_date")
    private LocalDate subscriptionStartDate;

    // 청약 종료일
    @Column(name = "subscription_end_date")
    private LocalDate subscriptionEndDate;

    // 상장일
    @Column(name = "listing_date")
    private LocalDate listingDate;

    // 최근 성장 관련 점수
    // 홈의 recentGrowth 정렬이나 급등 정렬 보조값으로 사용할 수 있음
    @Column(name = "recent_growth_score")
    private Integer recentGrowthScore;

    // 생성일시
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    // 수정일시
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}