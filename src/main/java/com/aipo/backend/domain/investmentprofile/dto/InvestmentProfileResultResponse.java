package com.aipo.backend.domain.investmentprofile.dto;

import com.aipo.backend.domain.investmentprofile.entity.InvestmentProfileTestStatus;
import com.aipo.backend.domain.investmentprofile.entity.InvestmentProfileType;

import java.time.LocalDateTime;
import java.util.List;

public record InvestmentProfileResultResponse(
        Long resultId,
        Integer version,
        InvestmentProfileTestStatus testStatus,
        InvestmentProfileType profileType,
        String profileLabel,
        String title,
        String description,
        List<String> tags,
        String startButtonLabel,
        String nextAction,
        Integer totalScore,
        LocalDateTime calculatedAt
) {
}
