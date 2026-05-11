package com.aipo.backend.domain.home.dto;

import java.time.LocalDate;

public record AttractivenessItem(
        // 공모주 식별자
        Long ipoId,

        // 종목명
        String name,

        // 매력지수
        Integer score,

        // 청약 시작일
        LocalDate subscriptionStartDate,

        // 청약 종료일
        LocalDate subscriptionEndDate,

        // 주관사
        String leadManager
) {
    // JPQL 생성자 투영 호환성을 보장하기 위한 5개 파라미터 생성자 추가
    public AttractivenessItem(Long ipoId, String name, Integer score, LocalDate subscriptionStartDate, LocalDate subscriptionEndDate) {
        this(ipoId, name, score, subscriptionStartDate, subscriptionEndDate, null);
    }
}