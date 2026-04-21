package com.aipo.backend.domain.ipo.repository;

import com.aipo.backend.domain.ipo.entity.IpoAttractionScore;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IpoAttractionScoreRepository extends JpaRepository<IpoAttractionScore, Long> {

    Optional<IpoAttractionScore> findTopByStock_IdOrderByCalculatedAtDesc(Long stockId);
}
