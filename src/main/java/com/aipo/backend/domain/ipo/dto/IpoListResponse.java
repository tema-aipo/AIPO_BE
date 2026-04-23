package com.aipo.backend.domain.ipo.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "공모주 목록 페이지 응답")
public record IpoListResponse(
        @Schema(description = "공모주 목록")
        List<IpoListItem> items,
        @Schema(description = "현재 페이지 번호. 0부터 시작", example = "0")
        Integer page,
        @Schema(description = "페이지 크기", example = "20")
        Integer size,
        @Schema(description = "전체 항목 수", example = "42")
        Long totalElements,
        @Schema(description = "전체 페이지 수", example = "3")
        Integer totalPages,
        @Schema(description = "다음 페이지 존재 여부", example = "true")
        Boolean hasNext
) {
    public static IpoListResponse of(List<IpoListItem> items, int page, int size, long totalElements) {
        int totalPages = size == 0 ? 0 : (int) Math.ceil((double) totalElements / size);
        boolean hasNext = page + 1 < totalPages;
        return new IpoListResponse(items, page, size, totalElements, totalPages, hasNext);
    }
}
