package com.aipo.backend.domain.chat.dto;

import java.util.List;

public record RecommendedQuestionResponse(
        String profileType,
        List<RecommendedQuestionItem> questions
) {
}
