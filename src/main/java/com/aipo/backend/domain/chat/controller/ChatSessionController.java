package com.aipo.backend.domain.chat.controller;

import com.aipo.backend.domain.chat.dto.ChatSessionDetailResponse;
import com.aipo.backend.domain.chat.dto.ChatSessionListResponse;
import com.aipo.backend.domain.chat.dto.ChatSessionPinResponse;
import com.aipo.backend.domain.chat.dto.CreateChatSessionResponse;
import com.aipo.backend.domain.chat.dto.DeleteRecentChatSessionsResponse;
import com.aipo.backend.domain.chat.dto.PinChatSessionRequest;
import com.aipo.backend.domain.chat.service.ChatSessionService;
import com.aipo.backend.global.config.OpenApiConfig;
import com.aipo.backend.global.security.service.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/chat/sessions")
@SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME_NAME)
public class ChatSessionController {

    private final ChatSessionService chatSessionService;

    @PostMapping
    @Operation(summary = "새 채팅 세션 생성")
    public ResponseEntity<CreateChatSessionResponse> createSession(
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        return ResponseEntity.ok(chatSessionService.createSession(principal.getUserId()));
    }

    @GetMapping
    @Operation(summary = "채팅 세션 목록 조회")
    public ResponseEntity<ChatSessionListResponse> getSessions(
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        return ResponseEntity.ok(chatSessionService.getSessions(principal.getUserId()));
    }

    @GetMapping("/{sessionId}")
    @Operation(summary = "채팅 세션 상세 조회")
    public ResponseEntity<ChatSessionDetailResponse> getSessionDetail(
            @PathVariable Long sessionId,
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        return ResponseEntity.ok(chatSessionService.getSessionDetail(principal.getUserId(), sessionId));
    }

    @DeleteMapping("/{sessionId}")
    @Operation(summary = "채팅 세션 삭제")
    public ResponseEntity<Void> deleteSession(
            @PathVariable Long sessionId,
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        chatSessionService.deleteSession(principal.getUserId(), sessionId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/recent")
    @Operation(summary = "최근 기록 전체 삭제", description = "고정되지 않은 채팅 세션만 일괄 삭제합니다.")
    public ResponseEntity<DeleteRecentChatSessionsResponse> deleteRecentSessions(
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        return ResponseEntity.ok(chatSessionService.deleteRecentSessions(principal.getUserId()));
    }

    @PatchMapping("/{sessionId}/pin")
    @Operation(summary = "채팅 세션 고정/해제")
    public ResponseEntity<ChatSessionPinResponse> updatePinned(
            @PathVariable Long sessionId,
            @RequestBody PinChatSessionRequest request,
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        return ResponseEntity.ok(chatSessionService.updatePinned(principal.getUserId(), sessionId, request.pinned()));
    }
}
