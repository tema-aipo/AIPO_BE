package com.aipo.backend.domain.chat.dto;

public record RecommendedQuestionItem(
        Long recommendedQuestionId,
        String questionText,
        Integer displayOrder
) {
}
