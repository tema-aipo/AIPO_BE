package com.aipo.backend.domain.chat.dto;

import java.time.LocalDateTime;

public record ChatMessageItem(
        Long messageId,
        String role,
        String messageType,
        String content,
        LocalDateTime createdAt,
        ChatFeedbackItem feedback
) {
}
