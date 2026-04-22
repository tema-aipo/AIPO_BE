package com.aipo.backend.domain.investmentprofile.dto;

import jakarta.validation.constraints.NotNull;

public record InvestmentProfileAnswerRequest(
        @NotNull Long questionId,
        @NotNull Long optionId
) {
}
