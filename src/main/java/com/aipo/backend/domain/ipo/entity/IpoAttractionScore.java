package com.aipo.backend.domain.ipo.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "ipo_attraction_score")
public class IpoAttractionScore {

    // 매력지수 PK
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "score_id")
    private Long id;

    // 어느 공모주의 점수인지 연결
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stock_id", nullable = false)
    private IpoStock stock;

    // 총 매력지수
    @Column(name = "total_score", nullable = false)
    private Integer totalScore;

    // 재무 점수
    @Column(name = "financial_score")
    private Integer financialScore;

    // 수요예측 점수
    @Column(name = "demand_score")
    private Integer demandScore;

    // 시장 점수
    @Column(name = "market_score")
    private Integer marketScore;

    // 점수 계산 코멘트
    @Column(name = "score_comment", columnDefinition = "TEXT")
    private String scoreComment;

    // 계산 시각
    @Column(name = "calculated_at", nullable = false)
    private LocalDateTime calculatedAt;
}