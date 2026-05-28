package com.aipo.backend.domain.ipo.repository;

import com.aipo.backend.domain.ipo.entity.IpoViewLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

// 조회 로그 기본 CRUD용 repository
public interface IpoViewLogRepository extends JpaRepository<IpoViewLog, Long> {

    long countByViewedAtAfter(LocalDateTime after);

    @Query("""
            SELECT v.stock.id AS ipoId,
                   COALESCE(v.stock.corpName, v.stock.stockCode) AS stockName,
                   COUNT(v) AS viewCount
            FROM IpoViewLog v
            GROUP BY v.stock.id, v.stock.corpName, v.stock.stockCode
            ORDER BY COUNT(v) DESC
            """)
    List<TrendingIpoProjection> findTrendingIpos(Pageable pageable);

    interface TrendingIpoProjection {
        Long getIpoId();

        String getStockName();

        Long getViewCount();
    }
}
