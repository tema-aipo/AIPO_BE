package com.aipo.backend.domain.ipo.dto;

import java.util.List;

public record IpoDetailResponse(
        Long ipoId,
        SummarySection summary,
        AttractionSection attraction,
        AttractivenessResponse attractiveness,
        DemandForecastSection demandForecast,
        SubscriptionCompetitionSection subscriptionCompetition,
        ScheduleSection schedule,
        List<DepositInfoItem> depositInfos,
        OfferingInfoSection offeringInfo
) {
    public IpoDetailResponse(
            Long ipoId,
            SummarySection summary,
            AttractionSection attraction,
            DemandForecastSection demandForecast,
            SubscriptionCompetitionSection subscriptionCompetition,
            ScheduleSection schedule,
            List<DepositInfoItem> depositInfos,
            OfferingInfoSection offeringInfo
    ) {
        this(
                ipoId,
                summary,
                attraction,
                null,
                demandForecast,
                subscriptionCompetition,
                schedule,
                depositInfos,
                offeringInfo
        );
    }
}
