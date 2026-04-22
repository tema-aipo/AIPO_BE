package com.aipo.backend.domain.user.dto;

public record NotificationSettingResponse(
        boolean subscriptionScheduleNotificationEnabled,
        boolean listingDateNotificationEnabled
) {
}
