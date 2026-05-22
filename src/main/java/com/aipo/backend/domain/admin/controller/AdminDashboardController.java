package com.aipo.backend.domain.admin.controller;

import com.aipo.backend.domain.admin.dto.AdminIpoStatsResponse;
import com.aipo.backend.domain.admin.service.AdminDashboardService;
import com.aipo.backend.domain.chatbot.entity.MessageRole;
import com.aipo.backend.domain.chatbot.service.ChatbotLogService;
import com.aipo.backend.domain.chatbot.service.ChatbotLogService.ChatbotLogResponse;
import com.aipo.backend.domain.document.entity.DocumentStatus;
import com.aipo.backend.domain.document.repository.DocumentRepository;
import com.aipo.backend.domain.pipeline.entity.PipelineJobStatus;
import com.aipo.backend.domain.pipeline.repository.PipelineJobRepository;
import com.aipo.backend.domain.user.entity.UserRole;
import com.aipo.backend.domain.user.entity.UserStatus;
import com.aipo.backend.domain.user.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.Getter;
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

@Tag(name = "관리자 - 대시보드", description = "대시보드 통계 조회")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/admin/dashboard")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final UserRepository userRepository;
    private final ChatbotLogService chatbotLogService;
    private final DocumentRepository documentRepository;
    private final PipelineJobRepository pipelineJobRepository;
    private final AdminDashboardService adminDashboardService;

    @Operation(summary = "대시보드 상단 기본 통계 조회")
    @GetMapping("/stats")
    public StatsResponse stats() {
        long totalUsers = userRepository.countByRole(UserRole.USER);
        long activeUsers = userRepository.countByUserStatus(UserStatus.ACTIVE);
        long suspendedUsers = userRepository.countByUserStatus(UserStatus.SUSPENDED);
        long withdrawnUsers = userRepository.countByUserStatus(UserStatus.WITHDRAWN);
        long newUsersLast7Days = userRepository.countByRoleAndCreatedAtAfter(
                UserRole.USER, LocalDateTime.now().minusDays(7));

        ChatbotLogService.ChatbotStats chatbotStats = chatbotLogService.getStats();

        long totalDocuments = documentRepository.count();
        long processingDocuments = documentRepository.countByDocStatus(DocumentStatus.PROCESSING);
        long failedDocuments = documentRepository.countByDocStatus(DocumentStatus.FAILED);

        long runningPipelineJobs = pipelineJobRepository.countByJobStatus(PipelineJobStatus.RUNNING);
        long failedPipelineJobs = pipelineJobRepository.countByJobStatus(PipelineJobStatus.FAILED);

        return new StatsResponse(
                new UserStats(totalUsers, activeUsers, suspendedUsers, withdrawnUsers, newUsersLast7Days),
                chatbotStats,
                new DocumentStats(totalDocuments, processingDocuments, failedDocuments),
                new PipelineStats(runningPipelineJobs, failedPipelineJobs)
        );
    }

    @Operation(summary = "공모주 조회 통계 조회", description = "대시보드 하단 영역에 필요한 공모주 통계 데이터(전체/일일/급증/관심종목 Top 10)를 조회합니다.")
    @GetMapping("/ipo-stats")
    public ResponseEntity<AdminIpoStatsResponse> getIpoStats() {
        return ResponseEntity.ok(adminDashboardService.getIpoStatistics());
    }

    // ✨ 우리가 새롭게 추가한 챗봇 로그 페이징 조회 API!
    @Operation(summary = "챗봇 대화 로그 목록 조회 (페이징)", description = "관리자 페이지에서 챗봇 대화 로그를 페이징하여 조회합니다.")
    @GetMapping("/logs/chatbot")
    public ResponseEntity<Page<ChatbotLogResponse>> getChatbotLogs(
            @RequestParam(required = false) String sessionId,
            @RequestParam(required = false) MessageRole messageRole,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(chatbotLogService.getLogs(sessionId, messageRole, from, to, pageable));
    }

    @Getter
    @AllArgsConstructor
    static class StatsResponse {
        private UserStats users;
        private ChatbotLogService.ChatbotStats chatbot;
        private DocumentStats documents;
        private PipelineStats pipeline;
    }

    @Getter
    @AllArgsConstructor
    static class UserStats {
        private long total;
        private long active;
        private long suspended;
        private long withdrawn;
        private long newLast7Days;
    }

    @Getter
    @AllArgsConstructor
    static class DocumentStats {
        private long total;
        private long processing;
        private long failed;
    }

    @Getter
    @AllArgsConstructor
    static class PipelineStats {
        private long running;
        private long failed;
    }
}