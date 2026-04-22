package com.aipo.backend.domain.chat.service;

import com.aipo.backend.domain.chat.dto.RecommendedQuestionItem;
import com.aipo.backend.domain.chat.entity.ChatRecommendedQuestion;
import com.aipo.backend.domain.chat.entity.RecommendedQuestionSourceType;
import com.aipo.backend.domain.chat.repository.ChatRecommendedQuestionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatRecommendedQuestionService {

    private final ChatRecommendedQuestionRepository chatRecommendedQuestionRepository;

    public List<RecommendedQuestionItem> getActiveCommonQuestions() {
        return chatRecommendedQuestionRepository.findActiveCommonQuestions(
                        RecommendedQuestionSourceType.DEFAULT,
                        LocalDateTime.now()
                ).stream()
                .map(this::toItem)
                .toList();
    }

    private RecommendedQuestionItem toItem(ChatRecommendedQuestion question) {
        return new RecommendedQuestionItem(
                question.getId(),
                question.getQuestionText(),
                question.getDisplayOrder()
        );
    }
}
