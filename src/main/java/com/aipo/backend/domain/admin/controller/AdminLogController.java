package com.aipo.backend.domain.admin.controller;

import com.aipo.backend.domain.chat.entity.FeedbackReasonCode;
import com.aipo.backend.domain.chat.entity.FeedbackType;
import com.aipo.backend.domain.chat.service.ChatFeedbackAdminService;
import com.aipo.backend.domain.chatbot.entity.MessageRole;
import com.aipo.backend.domain.chatbot.service.ChatbotLogService;
import com.aipo.backend.domain.log.service.LoginLogService;
import com.aipo.backend.domain.user.entity.UserRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@Tag(name = "관리자 - 로그", description = "로그인 로그, 챗봇 로그 조회")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/admin/logs")
@RequiredArgsConstructor
public class AdminLogController {

    private final LoginLogService loginLogService;
    private final ChatbotLogService chatbotLogService;
    private final ChatFeedbackAdminService chatFeedbackAdminService;

    @Operation(summary = "로그인 로그 조회")
    @GetMapping("/login")
    public Page<LoginLogService.LoginLogResponse> loginLogs(
            @RequestParam(required = false) String loginId,
            @RequestParam(required = false) UserRole role,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "loggedInAt"));
        return loginLogService.getLogs(loginId, role, from, to, pageable);
    }

    @Operation(summary = "챗봇 로그 조회")
    @GetMapping("/chatbot")
    public Page<ChatbotLogService.ChatbotLogResponse> chatbotLogs(
            @RequestParam(required = false) String sessionId,
            @RequestParam(required = false) MessageRole messageRole,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return chatbotLogService.getLogs(sessionId, messageRole, from, to, pageable);
    }

    @Operation(summary = "챗봇 피드백 목록 조회")
    @GetMapping("/chatbot/feedback")
    public Page<ChatFeedbackAdminService.ChatFeedbackAdminResponse> chatbotFeedbacks(
            @RequestParam(required = false) FeedbackType feedbackType,
            @RequestParam(required = false) FeedbackReasonCode reasonCode,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "updatedAt"));
        return chatFeedbackAdminService.getFeedbacks(feedbackType, reasonCode, from, to, pageable);
    }
}
