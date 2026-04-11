package com.aipo.backend.domain.home.dto;

public record TrendingIpoItem(
        // 공모주 식별자
        Long ipoId,

        // 급등 순위
        Integer rank,

        // 종목명
        String name,

        // 조회수 증가율 또는 급등 수치
        Integer changeRate,

        // 현재 누적 조회수
        Long viewCount
) {
}