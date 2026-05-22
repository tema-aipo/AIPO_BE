package com.aipo.backend.domain.ipo.repository;

import com.aipo.backend.domain.ipo.entity.IpoViewLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

// 조회 로그 기본 CRUD용 repository
public interface IpoViewLogRepository extends JpaRepository<IpoViewLog, Long> {

    // 특정 기간 사이의 조회수 집계
    long countByViewedAtBetween(LocalDateTime start, LocalDateTime end);

    // 최근 특정 기간 동안 가장 많이 조회된 공모주 집계 (급증 종목)
    @Query("SELECT v.stock.id, v.stock.stockName, COUNT(v) " +
            "FROM IpoViewLog v " +
            "WHERE v.viewedAt >= :since " +
            "GROUP BY v.stock.id, v.stock.stockName " +
            "ORDER BY COUNT(v) DESC")
    List<Object[]> findTrendingIpos(@Param("since") LocalDateTime since, Pageable pageable);
}