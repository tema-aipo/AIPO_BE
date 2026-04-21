package com.aipo.backend.domain.ipo.dto;

import java.util.List;

public record IpoDetailResponse(
        Long ipoId,
        SummarySection summary,
        AttractionSection attraction,
        DemandForecastSection demandForecast,
        SubscriptionCompetitionSection subscriptionCompetition,
        ScheduleSection schedule,
        List<DepositInfoItem> depositInfos,
        OfferingInfoSection offeringInfo
) {
}
