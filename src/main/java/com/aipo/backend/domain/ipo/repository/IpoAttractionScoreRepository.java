package com.aipo.backend.domain.ipo.repository;

import com.aipo.backend.domain.ipo.entity.IpoAttractionScore;
import org.springframework.data.jpa.repository.JpaRepository;

// 매력지수 테이블 기본 CRUD용 repository
public interface IpoAttractionScoreRepository extends JpaRepository<IpoAttractionScore, Long> {
}