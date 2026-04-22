package com.aipo.backend.domain.chat.dto;

public record SubmitChatFeedbackRequest(
        String feedbackType,
        String reasonCode,
        String reasonDetail
) {
}
