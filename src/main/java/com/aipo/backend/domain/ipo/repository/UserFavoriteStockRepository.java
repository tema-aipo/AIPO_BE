package com.aipo.backend.domain.ipo.repository;

import com.aipo.backend.domain.ipo.entity.UserFavoriteStock;
import org.springframework.data.jpa.repository.JpaRepository;

// 관심종목 기본 CRUD용 repository
public interface UserFavoriteStockRepository extends JpaRepository<UserFavoriteStock, Long> {
}