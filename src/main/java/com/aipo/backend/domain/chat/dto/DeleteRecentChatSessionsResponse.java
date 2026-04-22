package com.aipo.backend.domain.chat.dto;

public record DeleteRecentChatSessionsResponse(
        int deletedSessionCount,
        long keptPinnedSessionCount
) {
}
