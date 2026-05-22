package com.aipo.backend.domain.ipo.repository;

import com.aipo.backend.domain.ipo.entity.UserFavoriteStock;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface UserFavoriteStockRepository extends JpaRepository<UserFavoriteStock, Long> {

    // (기존 코드) 그대로 유지!
    boolean existsByUserIdAndStock_Id(Long userId, Long stockId);

    @EntityGraph(attributePaths = "stock")
    List<UserFavoriteStock> findAllByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<UserFavoriteStock> findByUserIdAndStock_Id(Long userId, Long stockId);

    // ✨ [신규 추가] 대시보드 통계용: 가장 많이 찜한 공모주 Top N 집계
    @Query("SELECT f.stock.id, f.stock.stockName, COUNT(f) " +
            "FROM UserFavoriteStock f " +
            "GROUP BY f.stock.id, f.stock.stockName " +
            "ORDER BY COUNT(f) DESC")
    List<Object[]> findTopFavoriteIpos(Pageable pageable);
}