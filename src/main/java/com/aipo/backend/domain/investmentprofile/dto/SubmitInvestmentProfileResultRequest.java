package com.aipo.backend.domain.investmentprofile.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record SubmitInvestmentProfileResultRequest(
        @NotNull Long userId,
        @NotNull Integer version,
        @NotEmpty List<@Valid InvestmentProfileAnswerRequest> answers
) {
}
