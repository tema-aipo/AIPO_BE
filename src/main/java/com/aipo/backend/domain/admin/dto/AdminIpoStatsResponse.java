package com.aipo.backend.domain.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "관리자 대시보드 공모주 통계 응답")
public class AdminIpoStatsResponse {

    @Schema(description = "조회수 통계 (전체/오늘/최근7일)")
    private ViewStats viewStats;

    @Schema(description = "조회 급증 공모주 목록")
    private List<TrendingIpoDto> trendingIpos;

    @Schema(description = "관심 종목 Top 10 목록")
    private List<FavoriteIpoDto> topFavoriteIpos;

    @Getter
    @Builder
    public static class ViewStats {
        private long totalViews;
        private long todayViews;
        private long weeklyViews;
    }

    @Getter
    @AllArgsConstructor
    public static class TrendingIpoDto {
        private Long ipoId;
        private String stockName;
        private long viewCount;
    }

    @Getter
    @AllArgsConstructor
    public static class FavoriteIpoDto {
        private Long ipoId;
        private String stockName;
        private long favoriteCount;
    }
}