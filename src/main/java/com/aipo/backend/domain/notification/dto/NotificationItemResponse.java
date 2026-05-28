package com.aipo.backend.domain.notification.dto;

import java.time.LocalDateTime;

public record NotificationItemResponse(
        Long notificationId,
        String type,
        String title,
        String content,
        Long ipoId,
        String stockCode,
        boolean read,
        LocalDateTime createdAt
) {
}
