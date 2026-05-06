package com.aipo.backend.domain.ipo.repository;

import com.aipo.backend.domain.ipo.entity.IpoStock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;

public interface IpoStockRepository extends JpaRepository<IpoStock, Long>, IpoStockRepositoryCustom {

    Optional<IpoStock> findByDartCorpCode(String dartCorpCode);

    List<IpoStock> findAllByDartCorpCode(String dartCorpCode);
}
