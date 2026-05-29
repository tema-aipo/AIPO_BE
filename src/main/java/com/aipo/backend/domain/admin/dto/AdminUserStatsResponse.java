package com.aipo.backend.domain.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserStatsResponse {
    private long totalUsers;       // 전체 회원 수
    private long activeUsers;      // 활성 회원
    private long withdrawnUsers;   // 탈퇴 회원
    private long weeklyNewUsers;   // 최근 7일간 신규 가입
}