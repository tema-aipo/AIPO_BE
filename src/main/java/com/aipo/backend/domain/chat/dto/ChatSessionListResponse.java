package com.aipo.backend.domain.chat.dto;

import java.util.List;

public record ChatSessionListResponse(
        List<ChatSessionSummaryItem> pinnedSessions,
        List<ChatSessionSummaryItem> recentSessions
) {
}
