package com.aipo.backend.domain.chat.dto;

import java.time.LocalDateTime;

public record SubmitChatFeedbackResponse(
        Long messageId,
        String feedbackType,
        String reasonCode,
        String reasonDetail,
        LocalDateTime updatedAt
) {
}
