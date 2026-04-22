package com.aipo.backend.domain.investmentprofile.dto;

import java.util.List;

public record InvestmentProfileQuestionItem(
        Long questionId,
        Integer questionOrder,
        String questionText,
        List<InvestmentProfileOptionItem> options
) {
}
