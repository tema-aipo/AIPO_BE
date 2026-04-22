package com.aipo.backend.domain.chat.dto;

import java.time.LocalDateTime;
import java.util.List;

public record CreateChatSessionResponse(
        Long sessionId,
        String title,
        boolean pinned,
        LocalDateTime createdAt,
        String welcomeMessage,
        List<RecommendedQuestionItem> recommendedQuestions
) {
}
