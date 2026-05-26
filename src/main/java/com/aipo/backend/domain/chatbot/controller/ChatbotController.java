package com.aipo.backend.domain.chatbot.controller;

import com.aipo.backend.domain.chat.dto.RecommendedQuestionResponse;
import com.aipo.backend.domain.chat.service.ChatRecommendedQuestionService;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/chatbot")
@SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME_NAME)
public class ChatbotController {

    private final ChatbotService chatbotService;
    private final ChatRecommendedQuestionService chatRecommendedQuestionService;

    @PostMapping
    @Operation(summary = "Python chatbot request")
    public ResponseEntity<ChatbotResponse> chat(
            @Valid @RequestBody ChatbotRequest request,
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        return ResponseEntity.ok(chatbotService.chat(principal.getUserId(), request));
    }

    @GetMapping("/recommended-questions")
    @Operation(summary = "Recommended chatbot questions")
    public ResponseEntity<RecommendedQuestionResponse> getRecommendedQuestions(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestParam(required = false) Long userId
    ) {
        Long effectiveUserId = principal != null ? principal.getUserId() : userId;
        return ResponseEntity.ok(chatRecommendedQuestionService.getRecommendedQuestions(effectiveUserId));
    }
}
