package com.aipo.backend.domain.ipo.dto;

public record FactorScoresResponse(
        Integer competitionScore,
        Integer subscriptionScore,
        Integer instCommitmentScore,
        Integer floatingStockScore,
        Integer lockupScore
) {
}
