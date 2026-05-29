package com.aipo.backend.domain.admin.service;

import com.aipo.backend.domain.admin.dto.AdminIpoStatsResponse;
import com.aipo.backend.domain.admin.dto.AdminIpoStatsResponse.*;
// ✨ (주의) UserStatsResponse DTO 경로가 다르다면 본인 프로젝트에 맞게 수정해 주세요!
import com.aipo.backend.domain.admin.dto.AdminUserStatsResponse;
import com.aipo.backend.domain.ipo.repository.IpoViewLogRepository;
import com.aipo.backend.domain.ipo.repository.UserFavoriteStockRepository;
import com.aipo.backend.domain.user.repository.UserRepository; // ✨ 추가
import com.aipo.backend.domain.user.entity.UserRole; // ✨ 추가
import com.aipo.backend.domain.user.entity.UserStatus; // ✨ 추가

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminDashboardService {

    private final IpoViewLogRepository ipoViewLogRepository;
    private final UserFavoriteStockRepository userFavoriteStockRepository;
    private final UserRepository userRepository; // ✨ 추가: 유저 레포지토리 주입!

    // =========================================================
    // 1. 기존 공모주 통계 로직 (수정 없이 그대로 둡니다)
    // =========================================================
    @Transactional(readOnly = true)
    public AdminIpoStatsResponse getIpoStatistics() {
        // ... (기존 코드와 동일하므로 생략하지 않고 그대로 두시면 됩니다!) ...
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();
        LocalDateTime startOfWeek = startOfToday.minusDays(7);

        ViewStats viewStats = ViewStats.builder()
                .totalViews(ipoViewLogRepository.count())
                .todayViews(ipoViewLogRepository.countByViewedAtBetween(startOfToday, now))
                .weeklyViews(ipoViewLogRepository.countByViewedAtBetween(startOfWeek, now))
                .build();

        LocalDateTime threeDaysAgo = startOfToday.minusDays(3);
        List<TrendingIpoDto> trendingIpos = ipoViewLogRepository.findTrendingIpos(threeDaysAgo, PageRequest.of(0, 10))
                .stream()
                .map(obj -> new TrendingIpoDto((Long) obj[0], (String) obj[1], (Long) obj[2]))
                .collect(Collectors.toList());

        List<FavoriteIpoDto> topFavoriteIpos = userFavoriteStockRepository.findTopFavoriteIpos(PageRequest.of(0, 10))
                .stream()
                .map(obj -> new FavoriteIpoDto((Long) obj[0], (String) obj[1], (Long) obj[2]))
                .collect(Collectors.toList());

        return AdminIpoStatsResponse.builder()
                .viewStats(viewStats)
                .trendingIpos(trendingIpos)
                .topFavoriteIpos(topFavoriteIpos)
                .build();
    }

    // =========================================================
    // 2. ✨ 신규 추가: 사용자 현황 통계 로직
    // =========================================================
    @Transactional(readOnly = true)
    public AdminUserStatsResponse getUserStatistics() {
        // 1. 전체 회원 수 (관리자 제외, 탈퇴자 제외)
        long totalUsers = userRepository.countByRoleAndUserStatusNot(UserRole.USER, UserStatus.WITHDRAWN);

        // 2. 활성 회원 수 (관리자 제외, 활성 상태만)
        long activeUsers = userRepository.countByRoleAndUserStatus(UserRole.USER, UserStatus.ACTIVE);

        // 3. 탈퇴 회원 수 (관리자 제외, 탈퇴 상태만) - 소프트 딜리트 덕분에 여기서 정확히 카운트됩니다!
        long withdrawnUsers = userRepository.countByRoleAndUserStatus(UserRole.USER, UserStatus.WITHDRAWN);

        // 4. 최근 7일 신규 가입자 (관리자 제외)
        long weeklyNewUsers = userRepository.countByRoleAndCreatedAtAfter(UserRole.USER, LocalDateTime.now().minusDays(7));

        // DTO로 예쁘게 포장해서 프론트로 반환!
        return AdminUserStatsResponse.builder()
                .totalUsers(totalUsers)
                .activeUsers(activeUsers)
                .withdrawnUsers(withdrawnUsers)
                .weeklyNewUsers(weeklyNewUsers)
                .build();
    }
}