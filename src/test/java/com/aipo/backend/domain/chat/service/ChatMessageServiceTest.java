package com.aipo.backend.domain.chat.service;

import com.aipo.backend.domain.chat.dto.SendChatMessageResponse;
import com.aipo.backend.domain.chat.entity.ChatMessage;
import com.aipo.backend.domain.chat.entity.ChatSession;
import com.aipo.backend.domain.chat.entity.MessageRole;
import com.aipo.backend.domain.chat.entity.MessageType;
import com.aipo.backend.domain.chat.repository.ChatMessageRepository;
import com.aipo.backend.domain.chatbot.client.PythonChatbotClient;
import com.aipo.backend.domain.chatbot.dto.PythonChatResponse;
import com.aipo.backend.domain.investmentprofile.dto.InvestmentProfileResultResponse;
import com.aipo.backend.domain.investmentprofile.entity.InvestmentProfileTestStatus;
import com.aipo.backend.domain.investmentprofile.entity.InvestmentProfileType;
import com.aipo.backend.domain.investmentprofile.service.InvestmentProfileService;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatMessageServiceTest {

    @Mock
    private ChatSessionService chatSessionService;

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @Mock
    private PythonChatbotClient pythonChatbotClient;

    @Mock
    private InvestmentProfileService investmentProfileService;

    @InjectMocks
    private ChatMessageService chatMessageService;

    @Test
    @DisplayName("첫 USER 메시지 전송 시 제목을 자동 생성하고 USER/ASSISTANT 메시지를 함께 저장한다")
    void sendMessage_success() {
        Long userId = 1L;
        Long sessionId = 1L;
        ChatSession session = chatSession(userId, sessionId, "새 채팅");

        when(chatSessionService.getSession(userId, sessionId)).thenReturn(session);
        when(chatMessageRepository.countByChatSession_IdAndMessageRole(sessionId, MessageRole.USER)).thenReturn(0L);
        when(chatMessageRepository.findAllByChatSession_IdOrderBySequenceNoAsc(sessionId)).thenReturn(List.of());
        when(chatMessageRepository.findTopByChatSession_IdOrderBySequenceNoDesc(sessionId)).thenReturn(Optional.empty());
        when(investmentProfileService.getCurrentResult(userId)).thenReturn(completedProfile(InvestmentProfileType.AGGRESSIVE));
        when(pythonChatbotClient.chat(any())).thenReturn(new PythonChatResponse("success", "AI 답변입니다.", String.valueOf(userId)));
        when(chatMessageRepository.save(any(ChatMessage.class))).thenAnswer(invocation -> {
            ChatMessage message = invocation.getArgument(0);
            if (message.getMessageRole() == MessageRole.USER) {
                ReflectionTestUtils.setField(message, "id", 11L);
            } else {
                ReflectionTestUtils.setField(message, "id", 12L);
            }
            return message;
        });

        SendChatMessageResponse response = chatMessageService.sendMessage(userId, sessionId, "  첫 질문입니다  ");

        assertThat(response.sessionId()).isEqualTo(1L);
        assertThat(response.title()).isEqualTo("첫 질문입니다");
        assertThat(response.userMessage().role()).isEqualTo("USER");
        assertThat(response.assistantMessage().role()).isEqualTo("ASSISTANT");
        assertThat(session.getTitle()).isEqualTo("첫 질문입니다");
    }

    @Test
    @DisplayName("첫 USER 메시지가 아니면 기존 제목을 유지한다")
    void sendMessage_whenNotFirstUserMessage_keepsExistingTitle() {
        Long userId = 1L;
        Long sessionId = 1L;
        ChatSession session = chatSession(userId, sessionId, "기존 제목");

        when(chatSessionService.getSession(userId, sessionId)).thenReturn(session);
        when(chatMessageRepository.countByChatSession_IdAndMessageRole(sessionId, MessageRole.USER)).thenReturn(1L);
        when(chatMessageRepository.findAllByChatSession_IdOrderBySequenceNoAsc(sessionId)).thenReturn(List.of());
        when(chatMessageRepository.findTopByChatSession_IdOrderBySequenceNoDesc(sessionId))
                .thenReturn(Optional.of(message(session, 10L, MessageRole.ASSISTANT, "이전 답변", 2)));
        when(investmentProfileService.getCurrentResult(userId)).thenReturn(completedProfile(InvestmentProfileType.AGGRESSIVE));
        when(pythonChatbotClient.chat(any())).thenReturn(new PythonChatResponse("success", "AI 답변입니다.", String.valueOf(userId)));
        when(chatMessageRepository.save(any(ChatMessage.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SendChatMessageResponse response = chatMessageService.sendMessage(userId, sessionId, "두 번째 질문");

        assertThat(response.title()).isEqualTo("기존 제목");
        assertThat(session.getTitle()).isEqualTo("기존 제목");
    }

    @Test
    @DisplayName("제목 자동 생성 규칙을 적용한다")
    void generateTitle_success() {
        assertThat(chatMessageService.generateTitle("   ")).isEqualTo("새 채팅");
        assertThat(chatMessageService.generateTitle("  첫 줄\n둘째 줄  ")).isEqualTo("첫 줄 둘째 줄");
        assertThat(chatMessageService.generateTitle("12345678901234567890123456789012345"))
                .isEqualTo("123456789012345678901234567...");
    }

    private ChatSession chatSession(Long userId, Long id, String title) {
        ChatSession session = ChatSession.create(userId);
        ReflectionTestUtils.setField(session, "id", id);
        ReflectionTestUtils.setField(session, "title", title);
        ReflectionTestUtils.setField(session, "createdAt", LocalDateTime.of(2026, 4, 22, 10, 0));
        ReflectionTestUtils.setField(session, "updatedAt", LocalDateTime.of(2026, 4, 22, 10, 0));
        return session;
    }

    private ChatMessage message(ChatSession session, Long id, MessageRole role, String content, int sequenceNo) {
        ChatMessage message = ChatMessage.create(session, role, MessageType.TEXT, content, sequenceNo);
        ReflectionTestUtils.setField(message, "id", id);
        return message;
    }

    private InvestmentProfileResultResponse completedProfile(InvestmentProfileType profileType) {
        return new InvestmentProfileResultResponse(
                1L,
                1,
                InvestmentProfileTestStatus.COMPLETED,
                profileType,
                "profile",
                "title",
                "description",
                List.of(),
                "start",
                "next",
                10,
                LocalDateTime.now()
        );
    }
}
