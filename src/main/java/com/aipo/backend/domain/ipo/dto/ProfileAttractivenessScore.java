package com.aipo.backend.domain.ipo.dto;

public record ProfileAttractivenessScore(
        Integer score,
        String grade,
        String reason
) {
}
