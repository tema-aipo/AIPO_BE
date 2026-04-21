package com.aipo.backend.domain.ipo.repository;

import com.aipo.backend.domain.ipo.entity.UserFavoriteStock;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserFavoriteStockRepository extends JpaRepository<UserFavoriteStock, Long> {

    boolean existsByUserIdAndStock_Id(Long userId, Long stockId);

    @EntityGraph(attributePaths = "stock")
    List<UserFavoriteStock> findAllByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<UserFavoriteStock> findByUserIdAndStock_Id(Long userId, Long stockId);
}
