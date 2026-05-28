package com.aipo.backend.domain.notification.dto;

import java.util.List;

public record NotificationListResponse(
        List<NotificationItemResponse> items,
        long unreadCount
) {
}
