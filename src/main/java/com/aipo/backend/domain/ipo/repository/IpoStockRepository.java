package com.aipo.backend.domain.ipo.repository;

import com.aipo.backend.domain.ipo.entity.IpoStock;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IpoStockRepository extends JpaRepository<IpoStock, Long>, IpoStockRepositoryCustom {
}
