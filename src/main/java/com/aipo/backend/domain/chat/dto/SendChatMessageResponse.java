package com.aipo.backend.domain.chat.dto;

public record SendChatMessageResponse(
        Long sessionId,
        String title,
        ChatMessageItem userMessage,
        ChatMessageItem assistantMessage
) {
}
