package com.aipo.backend.domain.chatbot.dto;

import jakarta.validation.constraints.NotBlank;

public record ChatbotRequest(
        @NotBlank(message = "message는 비어 있을 수 없습니다.")
        String message,
        String userType
) {
}
