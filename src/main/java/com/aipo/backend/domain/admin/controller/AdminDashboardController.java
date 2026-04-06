package com.aipo.backend.domain.admin.controller;

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

    @GetMapping("/stats")
    public StatsResponse stats() {
        long totalUsers = userRepository.countByRole(UserRole.USER);
        long activeUsers = userRepository.countByUserStatus(UserStatus.ACTIVE);
        long suspendedUsers = userRepository.countByUserStatus(UserStatus.SUSPENDED);
        long withdrawnUsers = userRepository.countByUserStatus(UserStatus.WITHDRAWN);
        long newUsersLast7Days = userRepository.countByRoleAndCreatedAtAfter(
                UserRole.USER, LocalDateTime.now().minusDays(7));

        return new StatsResponse(totalUsers, activeUsers, suspendedUsers, withdrawnUsers, newUsersLast7Days);
    }

    @Getter
    @AllArgsConstructor
    static class StatsResponse {
        private long totalUsers;
        private long activeUsers;
        private long suspendedUsers;
        private long withdrawnUsers;
        private long newUsersLast7Days;
    }
}
