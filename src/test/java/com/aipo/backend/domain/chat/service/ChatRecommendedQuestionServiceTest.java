package com.aipo.backend.domain.chat.service;

import com.aipo.backend.domain.chat.dto.RecommendedQuestionResponse;
import com.aipo.backend.domain.chat.entity.ChatRecommendedQuestion;
import com.aipo.backend.domain.chat.entity.RecommendedQuestionCategory;
import com.aipo.backend.domain.chat.repository.ChatRecommendedQuestionRepository;
import com.aipo.backend.domain.investmentprofile.repository.UserInvestmentProfileResultRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doReturn;

@ExtendWith(MockitoExtension.class)
class ChatRecommendedQuestionServiceTest {

    @Mock
    private ChatRecommendedQuestionRepository chatRecommendedQuestionRepository;

    @Mock
    private UserInvestmentProfileResultRepository userInvestmentProfileResultRepository;

    @InjectMocks
    private ChatRecommendedQuestionService chatRecommendedQuestionService;

    @Test
    @DisplayName("투자 성향이 있으면 성향별 질문 2개와 일반 질문 1개를 반환한다")
    void getRecommendedQuestions_withProfile() {
        Long userId = 21L;
        when(userInvestmentProfileResultRepository.findCurrentProfileTypeValueByUserId(userId))
                .thenReturn(Optional.of("AGGRESSIVE"));
        doReturn(List.of(
                        question(1L, "단기 수익을 기대할 만한 공모주는?", "AGGRESSIVE"),
                        question(2L, "상장일 상승 가능성이 높은 기업은?", "AGGRESSIVE")
                ))
                .when(chatRecommendedQuestionRepository)
                .findActiveCommonQuestionsByTargetInvestmentType(eq("AGGRESSIVE"), any(LocalDateTime.class));
        doReturn(List.of(question(10001L, "이번 주 청약 일정 알려줘", "GENERAL")))
                .when(chatRecommendedQuestionRepository)
                .findActiveCommonGeneralQuestions(any(LocalDateTime.class));

        RecommendedQuestionResponse response = chatRecommendedQuestionService.getRecommendedQuestions(userId);

        assertThat(response.profileType()).isEqualTo("AGGRESSIVE");
        assertThat(response.questions()).hasSize(3);
        assertThat(response.questions())
                .extracting("recommendedQuestionId")
                .containsExactlyInAnyOrder(1L, 2L, 10001L);
    }

    @Test
    @DisplayName("투자 성향이 없으면 일반 질문만 최대 3개 반환한다")
    void getRecommendedQuestions_withoutProfile() {
        Long userId = 21L;
        when(userInvestmentProfileResultRepository.findCurrentProfileTypeValueByUserId(userId))
                .thenReturn(Optional.empty());
        doReturn(List.of(
                        question(10001L, "이번 주 청약 일정 알려줘", "GENERAL"),
                        question(10002L, "오늘 주목할 만한 공모주는?", "GENERAL"),
                        question(10003L, "공모가가 적절한지 알려줘", "GENERAL")
                ))
                .when(chatRecommendedQuestionRepository)
                .findActiveCommonGeneralQuestions(any(LocalDateTime.class));

        RecommendedQuestionResponse response = chatRecommendedQuestionService.getRecommendedQuestions(userId);

        assertThat(response.profileType()).isEqualTo("DEFAULT");
        assertThat(response.questions()).hasSize(3);
        verify(chatRecommendedQuestionRepository, never())
                .findActiveCommonQuestionsByTargetInvestmentType(any(), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("예상 밖의 투자 성향 값은 DEFAULT로 처리한다")
    void getRecommendedQuestions_unknownProfile() {
        Long userId = 21L;
        when(userInvestmentProfileResultRepository.findCurrentProfileTypeValueByUserId(userId))
                .thenReturn(Optional.of("UNKNOWN"));
        doReturn(List.of(question(10001L, "이번 주 청약 일정 알려줘", "GENERAL")))
                .when(chatRecommendedQuestionRepository)
                .findActiveCommonGeneralQuestions(any(LocalDateTime.class));

        RecommendedQuestionResponse response = chatRecommendedQuestionService.getRecommendedQuestions(userId);

        assertThat(response.profileType()).isEqualTo("DEFAULT");
        assertThat(response.questions()).hasSize(1);
    }

    private ChatRecommendedQuestion question(Long id, String questionText, String targetInvestmentType) {
        ChatRecommendedQuestion question = mock(ChatRecommendedQuestion.class);
        when(question.getId()).thenReturn(id);
        when(question.getQuestionText()).thenReturn(questionText);
        when(question.getCategory()).thenReturn(RecommendedQuestionCategory.POPULAR);
        when(question.getTargetInvestmentType()).thenReturn(targetInvestmentType);
        return question;
    }
}
