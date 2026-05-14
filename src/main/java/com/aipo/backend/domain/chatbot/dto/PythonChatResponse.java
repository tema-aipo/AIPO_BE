package com.aipo.backend.domain.chatbot.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record PythonChatResponse(
        String status,
        String answer,
        @JsonProperty("user_id")
        String userId
) {
}
