package com.aipo.backend.domain.chat.dto;

import java.time.LocalDateTime;

public record ChatSessionSummaryItem(
        Long sessionId,
        String title,
        boolean pinned,
        String lastMessagePreview,
        LocalDateTime lastMessageAt
) {
}
