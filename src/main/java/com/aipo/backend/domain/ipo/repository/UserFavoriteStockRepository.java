package com.aipo.backend.domain.ipo.repository;

import com.aipo.backend.domain.ipo.entity.UserFavoriteStock;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface UserFavoriteStockRepository extends JpaRepository<UserFavoriteStock, Long> {

    boolean existsByUserIdAndStock_Id(Long userId, Long stockId);

    @EntityGraph(attributePaths = "stock")
    List<UserFavoriteStock> findAllByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<UserFavoriteStock> findByUserIdAndStock_Id(Long userId, Long stockId);

    @Query("""
            SELECT f.stock.id AS ipoId,
                   COALESCE(f.stock.corpName, f.stock.stockCode) AS stockName,
                   COUNT(f) AS favoriteCount
            FROM UserFavoriteStock f
            GROUP BY f.stock.id, f.stock.corpName, f.stock.stockCode
            ORDER BY COUNT(f) DESC
            """)
    List<TopFavoriteIpoProjection> findTopFavoriteIpos(Pageable pageable);

    interface TopFavoriteIpoProjection {
        Long getIpoId();

        String getStockName();

        Long getFavoriteCount();
    }
}
