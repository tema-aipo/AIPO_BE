package com.aipo.backend.domain.admin.service;

import com.aipo.backend.domain.admin.dto.AdminIpoStatsResponse;
import com.aipo.backend.domain.admin.dto.AdminIpoStatsResponse.*;
import com.aipo.backend.domain.ipo.repository.IpoViewLogRepository;
import com.aipo.backend.domain.ipo.repository.UserFavoriteStockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service // ✨ 핵심! 스프링에게 이게 서비스라고 알려주는 역할
@RequiredArgsConstructor
public class AdminDashboardService {

    private final IpoViewLogRepository ipoViewLogRepository;
    private final UserFavoriteStockRepository userFavoriteStockRepository;

    @Transactional(readOnly = true)
    public AdminIpoStatsResponse getIpoStatistics() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();
        LocalDateTime startOfWeek = startOfToday.minusDays(7);

        // 1. 조회수 통계 세팅 (전체, 오늘, 최근 7일)
        ViewStats viewStats = ViewStats.builder()
                .totalViews(ipoViewLogRepository.count())
                .todayViews(ipoViewLogRepository.countByViewedAtBetween(startOfToday, now))
                .weeklyViews(ipoViewLogRepository.countByViewedAtBetween(startOfWeek, now))
                .build();

        // 2. 조회 급증 공모주 (최근 3일 기준 최대 10개)
        LocalDateTime threeDaysAgo = startOfToday.minusDays(3);
        List<TrendingIpoDto> trendingIpos = ipoViewLogRepository.findTrendingIpos(threeDaysAgo, PageRequest.of(0, 10))
                .stream()
                .map(obj -> new TrendingIpoDto((Long) obj[0], (String) obj[1], (Long) obj[2]))
                .collect(Collectors.toList());

        // 3. 관심 종목 상위 10개
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
}