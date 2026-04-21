package com.aipo.backend.domain.ipo.dto;

public record SubscriptionCompetitionSection(
        CompetitionTab defaultTab,
        CompetitionInfo equalAllocation,
        CompetitionInfo proportionalAllocation
) {
}
