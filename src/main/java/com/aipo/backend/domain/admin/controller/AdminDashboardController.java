package com.aipo.backend.domain.admin.controller;

import com.aipo.backend.domain.chatbot.service.ChatbotLogService;
import com.aipo.backend.domain.document.entity.DocumentStatus;
import com.aipo.backend.domain.document.repository.DocumentRepository;
import com.aipo.backend.domain.ipo.repository.IpoViewLogRepository;
import com.aipo.backend.domain.ipo.repository.UserFavoriteStockRepository;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Supplier;

@Tag(name = "관리자 - 대시보드", description = "대시보드 통계 조회")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/admin/dashboard")
@RequiredArgsConstructor
@Slf4j
public class AdminDashboardController {

    private final UserRepository userRepository;
    private final ChatbotLogService chatbotLogService;
    private final DocumentRepository documentRepository;
    private final PipelineJobRepository pipelineJobRepository;
    private final IpoViewLogRepository ipoViewLogRepository;
    private final UserFavoriteStockRepository userFavoriteStockRepository;

    @Operation(summary = "대시보드 통계 조회")
    @GetMapping("/stats")
    public StatsResponse stats() {
        long totalUsers = safeCount(() -> userRepository.countByRole(UserRole.USER), "total users");
        long activeUsers = safeCount(() -> userRepository.countByUserStatus(UserStatus.ACTIVE), "active users");
        long suspendedUsers = safeCount(() -> userRepository.countByUserStatus(UserStatus.SUSPENDED), "suspended users");
        long withdrawnUsers = safeCount(() -> userRepository.countByUserStatus(UserStatus.WITHDRAWN), "withdrawn users");
        long newUsersLast7Days = safeCount(
                () -> userRepository.countByRoleAndCreatedAtAfter(UserRole.USER, LocalDateTime.now().minusDays(7)),
                "new users last 7 days");

        ChatbotLogService.ChatbotStats chatbotStats = safeChatbotStats();

        long totalDocuments = safeCount(documentRepository::count, "total documents");
        long processingDocuments = safeCount(
                () -> documentRepository.countByDocStatus(DocumentStatus.PROCESSING),
                "processing documents");
        long failedDocuments = safeCount(
                () -> documentRepository.countByDocStatus(DocumentStatus.FAILED),
                "failed documents");

        long runningPipelineJobs = safeCount(
                () -> pipelineJobRepository.countByJobStatus(PipelineJobStatus.RUNNING),
                "running pipeline jobs");
        long failedPipelineJobs = safeCount(
                () -> pipelineJobRepository.countByJobStatus(PipelineJobStatus.FAILED),
                "failed pipeline jobs");

        return new StatsResponse(
                new UserStats(totalUsers, activeUsers, suspendedUsers, withdrawnUsers, newUsersLast7Days),
                chatbotStats,
                new DocumentStats(totalDocuments, processingDocuments, failedDocuments),
                new PipelineStats(runningPipelineJobs, failedPipelineJobs)
        );
    }

    @Operation(summary = "공모주 조회 통계 조회")
    @GetMapping("/ipo-stats")
    public IpoStatsResponse ipoStats() {
        LocalDateTime todayStart = LocalDateTime.now().toLocalDate().atStartOfDay();
        LocalDateTime weekAgo = LocalDateTime.now().minusDays(7);

        IpoViewStats viewStats = new IpoViewStats(
                safeCount(ipoViewLogRepository::count, "total IPO views"),
                safeCount(() -> ipoViewLogRepository.countByViewedAtAfter(todayStart), "today IPO views"),
                safeCount(() -> ipoViewLogRepository.countByViewedAtAfter(weekAgo), "weekly IPO views")
        );

        List<TrendingIpoItem> trendingIpos = safeList(
                () -> ipoViewLogRepository.findTrendingIpos(PageRequest.of(0, 5)).stream()
                        .map(item -> new TrendingIpoItem(
                                item.getIpoId(),
                                fallbackStockName(item.getStockName()),
                                nullToZero(item.getViewCount())
                        ))
                        .toList(),
                "trending IPOs");

        List<TopFavoriteIpoItem> topFavoriteIpos = safeList(
                () -> userFavoriteStockRepository.findTopFavoriteIpos(PageRequest.of(0, 5)).stream()
                        .map(item -> new TopFavoriteIpoItem(
                                item.getIpoId(),
                                fallbackStockName(item.getStockName()),
                                nullToZero(item.getFavoriteCount())
                        ))
                        .toList(),
                "top favorite IPOs");

        return new IpoStatsResponse(viewStats, trendingIpos, topFavoriteIpos);
    }

    private ChatbotLogService.ChatbotStats safeChatbotStats() {
        try {
            return chatbotLogService.getStats();
        } catch (RuntimeException exception) {
            log.warn("Failed to load chatbot dashboard stats. Falling back to zeros.", exception);
            return new ChatbotLogService.ChatbotStats(0, 0, 0, 0, 0, 0);
        }
    }

    private long safeCount(Supplier<Long> supplier, String label) {
        try {
            Long value = supplier.get();
            return value == null ? 0 : value;
        } catch (RuntimeException exception) {
            log.warn("Failed to load dashboard count: {}. Falling back to 0.", label, exception);
            return 0;
        }
    }

    private <T> List<T> safeList(Supplier<List<T>> supplier, String label) {
        try {
            return supplier.get();
        } catch (RuntimeException exception) {
            log.warn("Failed to load dashboard list: {}. Falling back to empty list.", label, exception);
            return List.of();
        }
    }

    private long nullToZero(Long value) {
        return value == null ? 0 : value;
    }

    private String fallbackStockName(String stockName) {
        return stockName == null || stockName.isBlank() ? "-" : stockName;
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

    @Getter
    @AllArgsConstructor
    static class IpoStatsResponse {
        private IpoViewStats viewStats;
        private List<TrendingIpoItem> trendingIpos;
        private List<TopFavoriteIpoItem> topFavoriteIpos;
    }

    @Getter
    @AllArgsConstructor
    static class IpoViewStats {
        private long totalViews;
        private long todayViews;
        private long weeklyViews;
    }

    @Getter
    @AllArgsConstructor
    static class TrendingIpoItem {
        private Long ipoId;
        private String stockName;
        private long viewCount;
    }

    @Getter
    @AllArgsConstructor
    static class TopFavoriteIpoItem {
        private Long ipoId;
        private String stockName;
        private long favoriteCount;
    }
}

