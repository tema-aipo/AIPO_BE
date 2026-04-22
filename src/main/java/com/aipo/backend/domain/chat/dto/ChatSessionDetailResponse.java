package com.aipo.backend.domain.chat.dto;

import java.util.List;

public record ChatSessionDetailResponse(
        Long sessionId,
        String title,
        boolean pinned,
        List<ChatMessageItem> messages
) {
}
