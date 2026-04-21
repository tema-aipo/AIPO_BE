package com.aipo.backend.domain.ipo.dto;

import java.math.BigDecimal;
import java.util.List;

public record AttractionSection(
        BigDecimal totalScore,
        List<AttractionReasonItem> reasons
) {
}
