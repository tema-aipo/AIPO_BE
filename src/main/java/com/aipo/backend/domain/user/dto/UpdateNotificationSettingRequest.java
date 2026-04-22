package com.aipo.backend.domain.user.dto;

public record UpdateNotificationSettingRequest(
        boolean subscriptionScheduleNotificationEnabled,
        boolean listingDateNotificationEnabled
) {
}
