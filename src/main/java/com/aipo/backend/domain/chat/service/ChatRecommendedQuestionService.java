package com.aipo.backend.domain.chat.service;

import com.aipo.backend.domain.chat.dto.RecommendedQuestionItem;
import com.aipo.backend.domain.chat.dto.RecommendedQuestionResponse;
import com.aipo.backend.domain.chat.entity.ChatRecommendedQuestion;
import com.aipo.backend.domain.chat.repository.ChatRecommendedQuestionRepository;
import com.aipo.backend.domain.investmentprofile.repository.UserInvestmentProfileResultRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatRecommendedQuestionService {

    private static final String DEFAULT_PROFILE_TYPE = "DEFAULT";
    private static final String GENERAL_TARGET_TYPE = "GENERAL";
    private static final int MAX_QUESTION_COUNT = 3;
    private static final int PROFILE_QUESTION_COUNT = 2;

    private final ChatRecommendedQuestionRepository chatRecommendedQuestionRepository;
    private final UserInvestmentProfileResultRepository userInvestmentProfileResultRepository;

    public RecommendedQuestionResponse getRecommendedQuestions(Long userId) {
        String profileType = resolveProfileType(userId);
        LocalDateTime now = LocalDateTime.now();

        List<ChatRecommendedQuestion> generalQuestions =
                chatRecommendedQuestionRepository.findActiveCommonGeneralQuestions(now);

        List<ChatRecommendedQuestion> selected;
        if (DEFAULT_PROFILE_TYPE.equals(profileType)) {
            selected = pickRandom(generalQuestions, MAX_QUESTION_COUNT);
        } else {
            selected = new ArrayList<>();
            List<ChatRecommendedQuestion> profileQuestions =
                    chatRecommendedQuestionRepository.findActiveCommonQuestionsByTargetInvestmentType(profileType, now);
            selected.addAll(pickRandom(profileQuestions, PROFILE_QUESTION_COUNT));
            fillFromGeneralQuestions(selected, generalQuestions, MAX_QUESTION_COUNT);
        }

        return new RecommendedQuestionResponse(
                profileType,
                selected.stream()
                        .limit(MAX_QUESTION_COUNT)
                        .map(this::toItem)
                        .toList()
        );
    }

    public List<RecommendedQuestionItem> getRecommendedQuestionItems(Long userId) {
        return getRecommendedQuestions(userId).questions();
    }

    private String resolveProfileType(Long userId) {
        if (userId == null) {
            return DEFAULT_PROFILE_TYPE;
        }

        return userInvestmentProfileResultRepository.findCurrentProfileTypeValueByUserId(userId)
                .map(this::normalizeProfileType)
                .orElse(DEFAULT_PROFILE_TYPE);
    }

    private String normalizeProfileType(String profileType) {
        if (profileType == null || profileType.isBlank()) {
            return DEFAULT_PROFILE_TYPE;
        }

        return switch (profileType.trim().toUpperCase()) {
            case "AGGRESSIVE" -> "AGGRESSIVE";
            case "NEUTRAL" -> "NEUTRAL";
            case "STABLE" -> "STABLE";
            default -> DEFAULT_PROFILE_TYPE;
        };
    }

    private void fillFromGeneralQuestions(
            List<ChatRecommendedQuestion> selected,
            List<ChatRecommendedQuestion> generalQuestions,
            int maxCount
    ) {
        Map<Long, ChatRecommendedQuestion> selectedById = toQuestionMap(selected);
        List<ChatRecommendedQuestion> candidates = generalQuestions.stream()
                .filter(question -> !selectedById.containsKey(question.getId()))
                .toList();

        for (ChatRecommendedQuestion question : pickRandom(candidates, maxCount - selectedById.size())) {
            selectedById.putIfAbsent(question.getId(), question);
        }

        selected.clear();
        selected.addAll(selectedById.values());
    }

    private Map<Long, ChatRecommendedQuestion> toQuestionMap(List<ChatRecommendedQuestion> questions) {
        Map<Long, ChatRecommendedQuestion> questionsById = new LinkedHashMap<>();
        for (ChatRecommendedQuestion question : questions) {
            questionsById.putIfAbsent(question.getId(), question);
        }
        return questionsById;
    }

    private List<ChatRecommendedQuestion> pickRandom(List<ChatRecommendedQuestion> questions, int limit) {
        if (limit <= 0 || questions.isEmpty()) {
            return List.of();
        }

        List<ChatRecommendedQuestion> shuffled = new ArrayList<>(questions);
        Collections.shuffle(shuffled, ThreadLocalRandom.current());
        return shuffled.stream()
                .limit(limit)
                .toList();
    }

    private RecommendedQuestionItem toItem(ChatRecommendedQuestion question) {
        return new RecommendedQuestionItem(
                question.getId(),
                question.getQuestionText(),
                question.getCategory() != null ? question.getCategory().name() : null,
                question.getTargetInvestmentType() != null ? question.getTargetInvestmentType() : GENERAL_TARGET_TYPE
        );
    }
}
