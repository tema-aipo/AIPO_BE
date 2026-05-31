package com.aipo.backend.domain.notification.dto;

import java.time.LocalDateTime;
import java.time.LocalDate;

public record NotificationItemResponse(
        Long notificationId,
        String type,
        String title,
        String content,
        Long ipoId,
        String stockCode,
        boolean read,
        LocalDate targetDate,
        LocalDateTime createdAt
) {
}
