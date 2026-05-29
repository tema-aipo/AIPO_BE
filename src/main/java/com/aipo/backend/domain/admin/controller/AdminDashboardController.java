package com.aipo.backend.domain.admin.controller;

import com.aipo.backend.domain.admin.dto.AdminIpoStatsResponse;
import com.aipo.backend.domain.admin.service.AdminDashboardService;
import com.aipo.backend.domain.chatbot.service.ChatbotLogService;
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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
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
        // 1. 전체 회원 (일반 유저 중 '탈퇴'가 아닌 사람)
        long totalUsers = userRepository.countByRoleAndUserStatusNot(UserRole.USER, UserStatus.WITHDRAWN);
// 2. 활성 회원 (일반 유저 중 '활성' 상태인 사람)
        long activeUsers = userRepository.countByRoleAndUserStatus(UserRole.USER, UserStatus.ACTIVE);
// 3. 정지 회원 (일반 유저 중 '정지' 상태인 사람)
        long suspendedUsers = userRepository.countByRoleAndUserStatus(UserRole.USER, UserStatus.SUSPENDED);
// 4. 탈퇴 회원 (일반 유저 중 '탈퇴' 상태인 사람)
        long withdrawnUsers = userRepository.countByRoleAndUserStatus(UserRole.USER, UserStatus.WITHDRAWN);
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