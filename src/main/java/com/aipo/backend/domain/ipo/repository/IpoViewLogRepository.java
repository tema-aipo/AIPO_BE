package com.aipo.backend.domain.ipo.repository;

import com.aipo.backend.domain.ipo.entity.IpoViewLog;
import org.springframework.data.jpa.repository.JpaRepository;

// 조회 로그 기본 CRUD용 repository
public interface IpoViewLogRepository extends JpaRepository<IpoViewLog, Long> {
}