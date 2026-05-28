package com.aipo.backend.domain.user.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

public record UpdateNotificationSettingRequest(
        @JsonAlias("subscriptionAlarm")
        boolean subscriptionScheduleNotificationEnabled,
        @JsonAlias("listingAlarm")
        boolean listingDateNotificationEnabled
) {
}
