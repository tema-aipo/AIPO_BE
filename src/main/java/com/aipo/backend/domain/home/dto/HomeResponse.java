package com.aipo.backend.domain.home.dto;

import java.util.List;

public record HomeResponse(
        // 홈 상단 대표 공모주 영역
        List<FeaturedIpoItem> featuredIpos,

        // 실시간 조회 급등 영역
        List<TrendingIpoItem> trendingIpos,

        // 공모주 매력지수 영역
        AttractivenessResponse attractiveness
) {
}

