package com.aipo.backend.domain.chat.dto;

import jakarta.validation.constraints.NotBlank;

public record SendChatMessageRequest(
        @NotBlank(message = "question은 비어 있을 수 없습니다.")
        String question
) {
}
