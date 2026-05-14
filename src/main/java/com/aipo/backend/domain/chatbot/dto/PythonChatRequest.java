package com.aipo.backend.domain.chatbot.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record PythonChatRequest(
        @JsonProperty("user_id")
        String userId,
        String message,
        @JsonProperty("user_type")
        String userType,
        @JsonProperty("chat_history")
        List<List<String>> chatHistory
) {
}
