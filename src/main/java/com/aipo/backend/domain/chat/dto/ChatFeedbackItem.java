package com.aipo.backend.domain.chat.dto;

public record ChatFeedbackItem(
        String feedbackType,
        String reasonCode,
        String reasonDetail
) {
}
