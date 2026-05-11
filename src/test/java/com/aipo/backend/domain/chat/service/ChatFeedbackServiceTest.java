package com.aipo.backend.domain.chat.service;

import com.aipo.backend.domain.chat.dto.SubmitChatFeedbackResponse;
import com.aipo.backend.domain.chat.entity.ChatFeedback;
import com.aipo.backend.domain.chat.entity.ChatMessage;
import com.aipo.backend.domain.chat.entity.ChatSession;
import com.aipo.backend.domain.chat.entity.FeedbackReasonCode;
import com.aipo.backend.domain.chat.entity.FeedbackType;
import com.aipo.backend.domain.chat.entity.MessageRole;
import com.aipo.backend.domain.chat.entity.MessageType;
import com.aipo.backend.domain.chat.repository.ChatFeedbackRepository;
import com.aipo.backend.domain.chat.repository.ChatMessageRepository;
import com.aipo.backend.global.exception.CustomException;
import com.aipo.backend.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatFeedbackServiceTest {

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @Mock
    private ChatFeedbackRepository chatFeedbackRepository;

    @InjectMocks
    private ChatFeedbackService chatFeedbackService;

    @Test
    @DisplayName("피드백이 없으면 새로 생성한다")
    void submitFeedback_create() {
        Long userId = 1L;
        ChatMessage assistantMessage = assistantMessage(userId, 10L);
        when(chatMessageRepository.findById(10L)).thenReturn(Optional.of(assistantMessage));
        when(chatFeedbackRepository.findByChatMessage_IdAndUserId(10L, userId)).thenReturn(Optional.empty());
        when(chatFeedbackRepository.save(any(ChatFeedback.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SubmitChatFeedbackResponse response = chatFeedbackService.submitFeedback(
                userId, 10L, "DISLIKE", "INACCURATE_INFO", "달라요"
        );

        assertThat(response.feedbackType()).isEqualTo("DISLIKE");
        assertThat(response.reasonCode()).isEqualTo("INACCURATE_INFO");
        verify(chatFeedbackRepository).save(any(ChatFeedback.class));
    }

    @Test
    @DisplayName("기존 피드백이 있으면 수정한다")
    void submitFeedback_update() {
        Long userId = 1L;
        ChatMessage assistantMessage = assistantMessage(userId, 10L);
        ChatFeedback feedback = ChatFeedback.create(
                assistantMessage,
                userId,
                FeedbackType.DISLIKE,
                FeedbackReasonCode.TOO_COMPLEX,
                "어려워요"
        );

        when(chatMessageRepository.findById(10L)).thenReturn(Optional.of(assistantMessage));
        when(chatFeedbackRepository.findByChatMessage_IdAndUserId(10L, userId)).thenReturn(Optional.of(feedback));
        when(chatFeedbackRepository.save(feedback)).thenReturn(feedback);

        SubmitChatFeedbackResponse response = chatFeedbackService.submitFeedback(
                userId, 10L, "LIKE", null, null
        );

        assertThat(response.feedbackType()).isEqualTo("LIKE");
        assertThat(response.reasonCode()).isNull();
    }

    @Test
    @DisplayName("USER 메시지에는 피드백을 남길 수 없다")
    void submitFeedback_whenUserMessage_throwException() {
        Long userId = 1L;
        ChatSession session = ChatSession.create(userId);
        ReflectionTestUtils.setField(session, "id", 1L);
        ChatMessage userMessage = ChatMessage.create(session, MessageRole.USER, MessageType.TEXT, "질문", 1);
        ReflectionTestUtils.setField(userMessage, "id", 11L);

        when(chatMessageRepository.findById(11L)).thenReturn(Optional.of(userMessage));

        assertThatThrownBy(() -> chatFeedbackService.submitFeedback(userId, 11L, "LIKE", null, null))
                .isInstanceOf(CustomException.class)
                .extracting(exception -> ((CustomException) exception).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_CHAT_FEEDBACK_TARGET);
    }

    @Test
    @DisplayName("싫어요 피드백에는 reasonCode가 필요하다")
    void submitFeedback_whenReasonMissing_throwIllegalArgumentException() {
        Long userId = 1L;
        ChatMessage assistantMessage = assistantMessage(userId, 10L);
        when(chatMessageRepository.findById(10L)).thenReturn(Optional.of(assistantMessage));

        assertThatThrownBy(() -> chatFeedbackService.submitFeedback(userId, 10L, "DISLIKE", null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("싫어요 피드백에는 reasonCode가 필요합니다.");
    }

    private ChatMessage assistantMessage(Long userId, Long messageId) {
        ChatSession session = ChatSession.create(userId);
        ReflectionTestUtils.setField(session, "id", 1L);
        ChatMessage assistantMessage = ChatMessage.create(session, MessageRole.ASSISTANT, MessageType.TEXT, "답변", 2);
        ReflectionTestUtils.setField(assistantMessage, "id", messageId);
        return assistantMessage;
    }
}
