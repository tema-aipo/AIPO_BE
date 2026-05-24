package com.aipo.backend.domain.ipo.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AttractivenessResponse(
        @JsonProperty("default")
        ProfileAttractivenessScore defaultScore,
        String selectedProfile,
        ProfileAttractivenessScore selected,
        ProfileAttractivenessScore aggressive,
        ProfileAttractivenessScore balanced,
        ProfileAttractivenessScore conservative,
        FactorScoresResponse factorScores,
        String notice
) {
}
