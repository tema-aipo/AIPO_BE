package com.aipo.backend.domain.admin.controller;

import com.aipo.backend.domain.chatbot.service.ChatbotLogService;
import com.aipo.backend.domain.document.entity.DocumentStatus;
import com.aipo.backend.domain.document.repository.DocumentRepository;
import com.aipo.backend.domain.pipeline.entity.PipelineJobStatus;
import com.aipo.backend.domain.pipeline.repository.PipelineJobRepository;
import com.aipo.backend.domain.user.entity.UserRole;
import com.aipo.backend.domain.user.entity.UserStatus;
import com.aipo.backend.domain.user.repository.UserRepository;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/admin/dashboard")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final UserRepository userRepository;
    private final ChatbotLogService chatbotLogService;
    private final DocumentRepository documentRepository;
    private final PipelineJobRepository pipelineJobRepository;

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
