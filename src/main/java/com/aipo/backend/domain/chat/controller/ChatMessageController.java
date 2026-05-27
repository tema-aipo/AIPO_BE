package com.aipo.backend.domain.chat.controller;

import com.aipo.backend.domain.chat.dto.SendChatMessageRequest;
import com.aipo.backend.domain.chat.dto.SendChatMessageResponse;
import com.aipo.backend.domain.chat.dto.SubmitChatFeedbackRequest;
import com.aipo.backend.domain.chat.dto.SubmitChatFeedbackResponse;
import com.aipo.backend.domain.chat.service.ChatFeedbackService;
import com.aipo.backend.domain.chat.service.ChatMessageService;
import com.aipo.backend.global.config.OpenApiConfig;
import com.aipo.backend.global.exception.CustomException;
import com.aipo.backend.global.exception.ErrorCode;
import com.aipo.backend.global.security.service.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME_NAME)
@RequestMapping("/api/v1/chat")
public class ChatMessageController {

    private final ChatMessageService chatMessageService;
    private final ChatFeedbackService chatFeedbackService;

    @PostMapping("/sessions/{sessionId}/messages")
    @Operation(summary = "채팅 질문 전송")
    public ResponseEntity<SendChatMessageResponse> sendMessage(
            @PathVariable Long sessionId,
            @Valid @RequestBody SendChatMessageRequest request,
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        if (principal == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }
        return ResponseEntity.ok(
                chatMessageService.sendMessage(principal.getUserId(), sessionId, request.question())
        );
    }

    @PostMapping("/messages/{messageId}/feedback")
    @Operation(summary = "채팅 답변 피드백 제출")
    public ResponseEntity<SubmitChatFeedbackResponse> submitFeedback(
            @PathVariable Long messageId,
            @RequestBody SubmitChatFeedbackRequest request,
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        if (principal == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }
        return ResponseEntity.ok(
                chatFeedbackService.submitFeedback(
                        principal.getUserId(),
                        messageId,
                        request.feedbackType(),
                        request.reasonCode(),
                        request.reasonDetail()
                )
        );
    }
}
