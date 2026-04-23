package com.aipo.backend.domain.ipo.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "공모주 목록 카드 항목")
public record IpoListItem(
        @Schema(description = "공모주 식별자", example = "1")
        Long ipoId,
        @Schema(description = "종목명", example = "AIPO")
        String stockName,
        @Schema(description = "기업명", example = "에이아이피오")
        String companyName,
        @Schema(description = "시장 구분", example = "KOSDAQ")
        String marketType,
        @Schema(description = "한 줄 소개", example = "공시문서 분석 기업")
        String oneLineDescription,
        @Schema(description = "확정 공모가", example = "15000.00")
        BigDecimal confirmedOfferPrice,
        @Schema(description = "청약 시작일", example = "2026-04-28")
        LocalDate subscriptionStartDate,
        @Schema(description = "청약 종료일", example = "2026-04-29")
        LocalDate subscriptionEndDate,
        @Schema(description = "상장일", example = "2026-05-08")
        LocalDate listingDate,
        @Schema(description = "최신 매력지수", example = "87.5")
        BigDecimal attractionScore,
        @Schema(description = "최근 조회/성장 점수", example = "91")
        Integer recentGrowthScore
) {
}
