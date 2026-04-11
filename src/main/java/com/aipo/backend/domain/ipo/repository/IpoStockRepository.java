package com.aipo.backend.domain.ipo.repository;

import com.aipo.backend.domain.ipo.entity.IpoStock;
import org.springframework.data.jpa.repository.JpaRepository;

// 기본 CRUD + 커스텀 조회 메서드를 함께 사용하기 위해
// IpoStockRepositoryCustom을 같이 상속한다.
public interface IpoStockRepository extends JpaRepository<IpoStock, Long>, IpoStockRepositoryCustom {
}