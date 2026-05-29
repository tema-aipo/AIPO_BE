package com.aipo.backend.domain.admin.controller;

// ✨ 핵심 포인트: MessageRole 패키지가 반드시 'chat' 이어야 합니다! ('chatbot' 아님)
import com.aipo.backend.domain.chat.entity.MessageRole;
import com.aipo.backend.domain.chatbot.service.ChatbotLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/chatbot") // 프론트엔드 API 주소에 맞게 수정 가능
@Tag(name = "Admin Chatbot", description = "관리자용 챗봇 통계 및 로그 API")
public class AdminChatbotController {

    // 주의: 기존 프로젝트에서 사용하시던 서비스 이름(adminChatbotService 등)이 있다면 그 이름으로 맞춰주세요!
    private final ChatbotLogService adminChatbotService;

    @GetMapping("/stats")
    @Operation(summary = "챗봇 대시보드 통계 조회", description = "총 대화 수, 좋아요/싫어요 개수 등을 반환합니다.")
    public ResponseEntity<ChatbotLogService.ChatbotStats> getStats() {
        // ✨ 방금 만든 좋아요/싫어요가 포함된 Stats 객체를 프론트로 쏴줍니다!
        return ResponseEntity.ok(adminChatbotService.getStats());
    }

    @GetMapping("/logs") // 이 부분은 아까 에러 로그에 있던 형태를 그대로 살렸습니다!
    @Operation(summary = "챗봇 대화 목록 조회", description = "챗봇 대화 기록을 페이징하여 반환합니다.")
    public ResponseEntity<Page<ChatbotLogService.ChatbotLogResponse>> getLogs(
            @RequestParam(required = false) String sessionId,
            @RequestParam(required = false) MessageRole messageRole,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            Pageable pageable) {

        // ✨ JSON 응답에 "isLiked" 가 포함된 로그 목록을 반환합니다.
        return ResponseEntity.ok(adminChatbotService.getLogs(sessionId, messageRole, from, to, pageable));
    }
}