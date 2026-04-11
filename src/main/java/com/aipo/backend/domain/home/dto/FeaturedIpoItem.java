package com.aipo.backend.domain.home.dto;

public record FeaturedIpoItem(
        // 공모주 식별자
        Long ipoId,

        // 홈 상단 노출 순위
        Integer rank,

        // 종목명
        String name,

        // 조회수
        Long viewCount
) {
}
//상단 카드 디자인에 따라 필드 추가 예정
//프론트와 상담 후 결정