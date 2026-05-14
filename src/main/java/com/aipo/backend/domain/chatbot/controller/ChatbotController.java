package com.aipo.backend.domain.chatbot.controller;

import com.aipo.backend.domain.chatbot.dto.ChatbotRequest;
import com.aipo.backend.domain.chatbot.dto.ChatbotResponse;
import com.aipo.backend.domain.chatbot.service.ChatbotService;
import com.aipo.backend.global.config.OpenApiConfig;
import com.aipo.backend.global.security.service.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/chatbot")
@SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME_NAME)
public class ChatbotController {

    private final ChatbotService chatbotService;

    @PostMapping
    @Operation(summary = "Python 챗봇 호출 테스트")
    public ResponseEntity<ChatbotResponse> chat(
            @Valid @RequestBody ChatbotRequest request,
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        return ResponseEntity.ok(chatbotService.chat(principal.getUserId(), request));
    }
}
