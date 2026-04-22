package com.aipo.backend.domain.investmentprofile.dto;

import jakarta.validation.constraints.NotNull;

public record SkipInvestmentProfileRequest(
        @NotNull Long userId,
        @NotNull Integer version
) {
}
