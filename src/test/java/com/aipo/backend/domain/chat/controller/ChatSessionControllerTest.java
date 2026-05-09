package com.aipo.backend.domain.chat.controller;

import com.aipo.backend.domain.chat.dto.ChatSessionDetailResponse;
import com.aipo.backend.domain.chat.dto.ChatSessionListResponse;
import com.aipo.backend.domain.chat.dto.ChatSessionPinResponse;
import com.aipo.backend.domain.chat.dto.ChatSessionSummaryItem;
import com.aipo.backend.domain.chat.dto.CreateChatSessionResponse;
import com.aipo.backend.domain.chat.dto.DeleteRecentChatSessionsResponse;
import com.aipo.backend.domain.chat.dto.PinChatSessionRequest;
import com.aipo.backend.domain.chat.dto.RecommendedQuestionItem;
import com.aipo.backend.domain.chat.service.ChatSessionService;
import com.aipo.backend.global.config.SecurityConfig;
import com.aipo.backend.global.security.jwt.JwtTokenProvider;
import com.aipo.backend.global.security.service.CustomUserDetails;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ChatSessionController.class)
@Import(SecurityConfig.class)
class ChatSessionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ChatSessionService chatSessionService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @Test
    @DisplayName("채팅 세션을 생성한다")
    void createSession_success() throws Exception {
        Long userId = 1L;
        CustomUserDetails principal = new CustomUserDetails(userId, "chat-user", "USER");
        when(chatSessionService.createSession(userId)).thenReturn(new CreateChatSessionResponse(
                1L,
                "새 채팅",
                false,
                LocalDateTime.of(2026, 4, 22, 10, 0),
                "안녕하세요",
                List.of(new RecommendedQuestionItem(1L, "이번 주 청약 일정 알려줘", 1))
        ));

        mockMvc.perform(post("/api/v1/chat/sessions")
                        .with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId").value(1L))
                .andExpect(jsonPath("$.recommendedQuestions[0].questionText").value("이번 주 청약 일정 알려줘"));
    }

    @Test
    @DisplayName("채팅 세션 목록을 pinned와 recent로 나눠 반환한다")
    void getSessions_success() throws Exception {
        Long userId = 1L;
        CustomUserDetails principal = new CustomUserDetails(userId, "chat-user", "USER");
        when(chatSessionService.getSessions(userId)).thenReturn(new ChatSessionListResponse(
                List.of(new ChatSessionSummaryItem(1L, "고정", true, "미리보기", LocalDateTime.of(2026, 4, 22, 10, 0))),
                List.of(new ChatSessionSummaryItem(2L, "최근", false, "미리보기2", LocalDateTime.of(2026, 4, 22, 9, 0)))
        ));

        mockMvc.perform(get("/api/v1/chat/sessions")
                        .with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pinnedSessions[0].sessionId").value(1L))
                .andExpect(jsonPath("$.recentSessions[0].sessionId").value(2L));
    }

    @Test
    @DisplayName("채팅 세션 상세를 조회한다")
    void getSessionDetail_success() throws Exception {
        Long userId = 1L;
        CustomUserDetails principal = new CustomUserDetails(userId, "chat-user", "USER");
        when(chatSessionService.getSessionDetail(userId, 1L)).thenReturn(new ChatSessionDetailResponse(
                1L,
                "세션",
                false,
                List.of()
        ));

        mockMvc.perform(get("/api/v1/chat/sessions/{sessionId}", 1L)
                        .with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId").value(1L))
                .andExpect(jsonPath("$.title").value("세션"));
    }

    @Test
    @DisplayName("최근 기록 전체 삭제 응답을 반환한다")
    void deleteRecentSessions_success() throws Exception {
        Long userId = 1L;
        CustomUserDetails principal = new CustomUserDetails(userId, "chat-user", "USER");
        when(chatSessionService.deleteRecentSessions(userId)).thenReturn(new DeleteRecentChatSessionsResponse(3, 2L));

        mockMvc.perform(delete("/api/v1/chat/sessions/recent")
                        .with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deletedSessionCount").value(3))
                .andExpect(jsonPath("$.keptPinnedSessionCount").value(2));
    }

    @Test
    @DisplayName("세션 고정 상태를 변경한다")
    void updatePinned_success() throws Exception {
        Long userId = 1L;
        CustomUserDetails principal = new CustomUserDetails(userId, "chat-user", "USER");
        when(chatSessionService.updatePinned(userId, 1L, true)).thenReturn(new ChatSessionPinResponse(1L, true));

        mockMvc.perform(patch("/api/v1/chat/sessions/{sessionId}/pin", 1L)
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new PinChatSessionRequest(true))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId").value(1L))
                .andExpect(jsonPath("$.pinned").value(true));
    }
}
