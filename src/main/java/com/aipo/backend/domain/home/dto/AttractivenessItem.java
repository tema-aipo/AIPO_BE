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
        LocalDate subscriptionEndDate
) {
}