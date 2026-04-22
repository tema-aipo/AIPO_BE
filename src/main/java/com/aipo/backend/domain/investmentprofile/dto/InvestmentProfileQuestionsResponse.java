package com.aipo.backend.domain.investmentprofile.dto;

import java.util.List;

public record InvestmentProfileQuestionsResponse(
        Integer version,
        List<InvestmentProfileQuestionItem> questions
) {
}
