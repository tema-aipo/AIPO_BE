package com.aipo.backend.domain.ipo.repository;

import com.aipo.backend.domain.ipo.entity.IpoAttractionScore;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;

public interface IpoAttractionScoreRepository extends JpaRepository<IpoAttractionScore, Long> {

    Optional<IpoAttractionScore> findTopByStock_IdOrderByCalculatedAtDesc(Long stockId);

    @org.springframework.data.jpa.repository.Query("select s from IpoAttractionScore s where s.stock.id in :stockIds order by s.calculatedAt desc")
    List<IpoAttractionScore> findAllByStock_IdIn(@org.springframework.data.repository.query.Param("stockIds") List<Long> stockIds);
}
