package com.aipo.backend.domain.chat.controller;

import com.aipo.backend.domain.chat.dto.ChatFeedbackItem;
import com.aipo.backend.domain.chat.dto.ChatMessageItem;
import com.aipo.backend.domain.chat.dto.SendChatMessageRequest;
import com.aipo.backend.domain.chat.dto.SendChatMessageResponse;
import com.aipo.backend.domain.chat.dto.SubmitChatFeedbackRequest;
import com.aipo.backend.domain.chat.dto.SubmitChatFeedbackResponse;
import com.aipo.backend.domain.chat.service.ChatFeedbackService;
import com.aipo.backend.domain.chat.service.ChatMessageService;
import com.aipo.backend.global.config.SecurityConfig;
import com.aipo.backend.global.security.jwt.JwtTokenProvider;
import com.aipo.backend.global.security.service.CustomUserDetails;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ChatMessageController.class)
@Import(SecurityConfig.class)
class ChatMessageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ChatMessageService chatMessageService;

    @MockBean
    private ChatFeedbackService chatFeedbackService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @Test
    @DisplayName("질문 전송 시 userMessage와 assistantMessage를 반환한다")
    void sendMessage_success() throws Exception {
        Long userId = 1L;
        CustomUserDetails principal = new CustomUserDetails(userId, "chat-user", "USER");
        when(chatMessageService.sendMessage(userId, 1L, "질문")).thenReturn(new SendChatMessageResponse(
                1L,
                "질문",
                new ChatMessageItem(11L, "USER", "TEXT", "질문", LocalDateTime.of(2026, 4, 22, 10, 0), null),
                new ChatMessageItem(12L, "ASSISTANT", "TEXT", "답변", LocalDateTime.of(2026, 4, 22, 10, 0, 1), new ChatFeedbackItem(null, null, null))
        ));

        mockMvc.perform(post("/api/v1/chat/sessions/{sessionId}/messages", 1L)
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SendChatMessageRequest("질문"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId").value(1L))
                .andExpect(jsonPath("$.userMessage.role").value("USER"))
                .andExpect(jsonPath("$.assistantMessage.role").value("ASSISTANT"));
    }

    @Test
    @DisplayName("질문이 비어 있으면 validation error를 반환한다")
    void sendMessage_whenQuestionBlank_returnsBadRequest() throws Exception {
        Long userId = 1L;
        CustomUserDetails principal = new CustomUserDetails(userId, "chat-user", "USER");

        mockMvc.perform(post("/api/v1/chat/sessions/{sessionId}/messages", 1L)
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SendChatMessageRequest(" "))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("답변 메시지 피드백을 upsert 형태로 저장한다")
    void submitFeedback_success() throws Exception {
        Long userId = 1L;
        CustomUserDetails principal = new CustomUserDetails(userId, "chat-user", "USER");
        when(chatFeedbackService.submitFeedback(userId, 12L, "DISLIKE", "INACCURATE_INFO", "달라요"))
                .thenReturn(new SubmitChatFeedbackResponse(
                        12L,
                        "DISLIKE",
                        "INACCURATE_INFO",
                        "달라요",
                        LocalDateTime.of(2026, 4, 22, 10, 10)
                ));

        mockMvc.perform(post("/api/v1/chat/messages/{messageId}/feedback", 12L)
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new SubmitChatFeedbackRequest("DISLIKE", "INACCURATE_INFO", "달라요")
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.messageId").value(12L))
                .andExpect(jsonPath("$.feedbackType").value("DISLIKE"))
                .andExpect(jsonPath("$.reasonCode").value("INACCURATE_INFO"));
    }
}
