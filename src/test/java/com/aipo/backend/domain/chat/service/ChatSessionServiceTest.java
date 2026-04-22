package com.aipo.backend.domain.chat.service;

import com.aipo.backend.domain.chat.dto.ChatSessionDetailResponse;
import com.aipo.backend.domain.chat.dto.ChatSessionListResponse;
import com.aipo.backend.domain.chat.dto.CreateChatSessionResponse;
import com.aipo.backend.domain.chat.dto.DeleteRecentChatSessionsResponse;
import com.aipo.backend.domain.chat.dto.RecommendedQuestionItem;
import com.aipo.backend.domain.chat.entity.ChatFeedback;
import com.aipo.backend.domain.chat.entity.ChatMessage;
import com.aipo.backend.domain.chat.entity.ChatSession;
import com.aipo.backend.domain.chat.entity.FeedbackReasonCode;
import com.aipo.backend.domain.chat.entity.FeedbackType;
import com.aipo.backend.domain.chat.entity.MessageRole;
import com.aipo.backend.domain.chat.entity.MessageType;
import com.aipo.backend.domain.chat.repository.ChatFeedbackRepository;
import com.aipo.backend.domain.chat.repository.ChatMessageRepository;
import com.aipo.backend.domain.chat.repository.ChatSessionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatSessionServiceTest {

    @Mock
    private ChatSessionRepository chatSessionRepository;

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @Mock
    private ChatFeedbackRepository chatFeedbackRepository;

    @Mock
    private ChatRecommendedQuestionService chatRecommendedQuestionService;

    @InjectMocks
    private ChatSessionService chatSessionService;

    @Test
    @DisplayName("새 채팅 세션 생성 시 추천 질문과 인사 메시지를 함께 반환한다")
    void createSession_success() {
        Long userId = 1L;
        ChatSession chatSession = ChatSession.create(userId);
        ReflectionTestUtils.setField(chatSession, "id", 10L);
        when(chatSessionRepository.save(any(ChatSession.class))).thenReturn(chatSession);
        when(chatRecommendedQuestionService.getActiveCommonQuestions()).thenReturn(List.of(
                new RecommendedQuestionItem(1L, "이번 주 청약 일정 알려줘", 1)
        ));

        CreateChatSessionResponse response = chatSessionService.createSession(userId);

        assertThat(response.sessionId()).isEqualTo(10L);
        assertThat(response.title()).isEqualTo("새 채팅");
        assertThat(response.welcomeMessage()).contains("AIPO");
        assertThat(response.recommendedQuestions()).hasSize(1);
    }

    @Test
    @DisplayName("세션 목록 조회 시 pinned와 recent로 분리한다")
    void getSessions_success() {
        Long userId = 1L;
        ChatSession pinned = chatSession(userId, 1L, "고정 세션", true, LocalDateTime.of(2026, 4, 22, 10, 0));
        ChatSession recent = chatSession(userId, 2L, "최근 세션", false, LocalDateTime.of(2026, 4, 22, 9, 0));

        when(chatSessionRepository.findAllByUserIdAndDeletedFalseOrderByPinnedDescLastMessageAtDesc(userId))
                .thenReturn(List.of(pinned, recent));
        when(chatMessageRepository.findTopByChatSession_IdOrderBySequenceNoDesc(1L))
                .thenReturn(Optional.of(message(pinned, 11L, MessageRole.ASSISTANT, "고정 미리보기", 1)));
        when(chatMessageRepository.findTopByChatSession_IdOrderBySequenceNoDesc(2L))
                .thenReturn(Optional.of(message(recent, 12L, MessageRole.ASSISTANT, "최근 미리보기", 1)));

        ChatSessionListResponse response = chatSessionService.getSessions(userId);

        assertThat(response.pinnedSessions()).hasSize(1);
        assertThat(response.pinnedSessions().get(0).sessionId()).isEqualTo(1L);
        assertThat(response.recentSessions()).hasSize(1);
        assertThat(response.recentSessions().get(0).sessionId()).isEqualTo(2L);
    }

    @Test
    @DisplayName("세션 상세 조회 시 assistant 메시지에는 feedback을 포함한다")
    void getSessionDetail_success() {
        Long userId = 1L;
        ChatSession session = chatSession(userId, 1L, "세션", false, LocalDateTime.of(2026, 4, 22, 10, 0));
        ChatMessage userMessage = message(session, 11L, MessageRole.USER, "질문", 1);
        ChatMessage assistantMessage = message(session, 12L, MessageRole.ASSISTANT, "답변", 2);
        ChatFeedback feedback = feedback(assistantMessage, userId, FeedbackType.DISLIKE, FeedbackReasonCode.TOO_COMPLEX, "어려워요");

        when(chatSessionRepository.findByIdAndUserIdAndDeletedFalse(1L, userId)).thenReturn(Optional.of(session));
        when(chatMessageRepository.findAllByChatSession_IdOrderBySequenceNoAsc(1L))
                .thenReturn(List.of(userMessage, assistantMessage));
        when(chatFeedbackRepository.findByChatMessage_IdAndUserId(12L, userId)).thenReturn(Optional.of(feedback));

        ChatSessionDetailResponse response = chatSessionService.getSessionDetail(userId, 1L);

        assertThat(response.messages()).hasSize(2);
        assertThat(response.messages().get(0).feedback()).isNull();
        assertThat(response.messages().get(1).feedback().feedbackType()).isEqualTo("DISLIKE");
        assertThat(response.messages().get(1).feedback().reasonCode()).isEqualTo("TOO_COMPLEX");
    }

    @Test
    @DisplayName("최근 기록 전체 삭제는 pinned를 제외하고 삭제 개수와 유지 개수를 반환한다")
    void deleteRecentSessions_success() {
        Long userId = 1L;
        when(chatSessionRepository.softDeleteRecentSessions(eq(userId), any(LocalDateTime.class))).thenReturn(3);
        when(chatSessionRepository.countByUserIdAndPinnedTrueAndDeletedFalse(userId)).thenReturn(2L);

        DeleteRecentChatSessionsResponse response = chatSessionService.deleteRecentSessions(userId);

        assertThat(response.deletedSessionCount()).isEqualTo(3);
        assertThat(response.keptPinnedSessionCount()).isEqualTo(2L);
    }

    @Test
    @DisplayName("세션 고정 상태를 변경한다")
    void updatePinned_success() {
        Long userId = 1L;
        ChatSession session = chatSession(userId, 1L, "세션", false, LocalDateTime.of(2026, 4, 22, 10, 0));
        when(chatSessionRepository.findByIdAndUserIdAndDeletedFalse(1L, userId)).thenReturn(Optional.of(session));

        var response = chatSessionService.updatePinned(userId, 1L, true);

        assertThat(response.sessionId()).isEqualTo(1L);
        assertThat(response.pinned()).isTrue();
        assertThat(session.isPinned()).isTrue();
    }

    private ChatSession chatSession(Long userId, Long id, String title, boolean pinned, LocalDateTime lastMessageAt) {
        ChatSession session = ChatSession.create(userId);
        ReflectionTestUtils.setField(session, "id", id);
        ReflectionTestUtils.setField(session, "title", title);
        ReflectionTestUtils.setField(session, "pinned", pinned);
        ReflectionTestUtils.setField(session, "lastMessageAt", lastMessageAt);
        ReflectionTestUtils.setField(session, "createdAt", lastMessageAt.minusMinutes(10));
        ReflectionTestUtils.setField(session, "updatedAt", lastMessageAt);
        return session;
    }

    private ChatMessage message(ChatSession session, Long id, MessageRole role, String content, int sequenceNo) {
        ChatMessage message = ChatMessage.create(session, role, MessageType.TEXT, content, sequenceNo);
        ReflectionTestUtils.setField(message, "id", id);
        ReflectionTestUtils.setField(message, "createdAt", LocalDateTime.of(2026, 4, 22, 10, 0).plusSeconds(sequenceNo));
        return message;
    }

    private ChatFeedback feedback(
            ChatMessage message,
            Long userId,
            FeedbackType feedbackType,
            FeedbackReasonCode reasonCode,
            String reasonDetail
    ) {
        ChatFeedback feedback = ChatFeedback.create(message, userId, feedbackType, reasonCode, reasonDetail);
        ReflectionTestUtils.setField(feedback, "id", 99L);
        return feedback;
    }
}
