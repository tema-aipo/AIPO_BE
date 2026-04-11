package com.aipo.backend.domain.home.dto;

import java.util.List;

public record AttractivenessResponse(
        // 현재 선택된 탭 값
        String selectedTab,

        // 탭 기준으로 정렬된 공모주 리스트
        List<AttractivenessItem> items
) {
}