package com.aipo.backend.domain.user.dto;

import com.aipo.backend.domain.investmentprofile.entity.InvestmentProfileTestStatus;
import com.aipo.backend.domain.investmentprofile.entity.InvestmentProfileType;

public record InvestmentProfileSummaryResponse(
        InvestmentProfileTestStatus testStatus,
        InvestmentProfileType profileType,
        String profileLabel,
        String summaryText
) {
}
