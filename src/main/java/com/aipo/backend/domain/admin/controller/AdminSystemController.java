package com.aipo.backend.domain.admin.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.lang.management.ManagementFactory;
import java.sql.Connection;
import java.time.LocalDateTime;

@Tag(name = "관리자 - 시스템", description = "시스템 상태 조회")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/admin/system")
public class AdminSystemController {

    private final DataSource dataSource;

    public AdminSystemController(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Operation(summary = "시스템 상태 조회")
    @GetMapping("/status")
    public SystemStatusResponse status() {
        boolean dbOk = checkDatabase();
        Runtime runtime = Runtime.getRuntime();
        long uptimeMs = ManagementFactory.getRuntimeMXBean().getUptime();

        MemoryInfo memory = new MemoryInfo(
                runtime.totalMemory() / 1024 / 1024,
                runtime.freeMemory() / 1024 / 1024,
                (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024,
                runtime.maxMemory() / 1024 / 1024
        );

        return new SystemStatusResponse(
                dbOk ? "UP" : "DOWN",
                dbOk,
                uptimeMs,
                memory,
                LocalDateTime.now()
        );
    }

    private boolean checkDatabase() {
        try (Connection conn = dataSource.getConnection()) {
            return conn.isValid(2);
        } catch (Exception e) {
            return false;
        }
    }

    @Getter
    @AllArgsConstructor
    static class SystemStatusResponse {
        private String status;
        private boolean databaseOk;
        private long uptimeMs;
        private MemoryInfo memory;
        private LocalDateTime checkedAt;
    }

    @Getter
    @AllArgsConstructor
    static class MemoryInfo {
        private long totalMb;
        private long freeMb;
        private long usedMb;
        private long maxMb;
    }
}
