package com.aipo.backend.domain.chat.dto;

public record ChatSessionPinResponse(
        Long sessionId,
        boolean pinned
) {
}
