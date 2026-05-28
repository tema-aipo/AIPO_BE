package com.aipo.backend.domain.admin.controller;

import com.aipo.backend.domain.admin.service.AdminChatbotService;
import com.aipo.backend.domain.chat.entity.MessageRole;
import com.aipo.backend.domain.chatbot.service.ChatbotLogService.ChatbotLogResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@Tag(name = "관리자 - 챗봇 관리", description = "챗봇 대화 내역 조회 및 관리")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/admin/chatbot")
@RequiredArgsConstructor
public class AdminChatbotController {

    private final AdminChatbotService adminChatbotService; // ✨ 변경된 서비스 이름 주입

    @Operation(summary = "챗봇 대화 로그 목록 조회", description = "관리자 페이지에서 챗봇 대화 로그를 페이징하여 조회합니다.")
    @GetMapping("/logs")
    public ResponseEntity<Page<ChatbotLogResponse>> getChatbotLogs(
            @RequestParam(required = false) String sessionId,
            @RequestParam(required = false) MessageRole messageRole,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(adminChatbotService.getLogs(sessionId, messageRole, from, to, pageable));
    }
}