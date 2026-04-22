package com.aipo.backend.domain.investmentprofile.dto;

public record InvestmentProfileOptionItem(
        Long optionId,
        Integer optionOrder,
        String optionText,
        Integer score
) {
}
