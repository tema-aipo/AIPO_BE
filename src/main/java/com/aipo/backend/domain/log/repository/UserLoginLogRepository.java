package com.aipo.backend.domain.log.repository;

import com.aipo.backend.domain.log.entity.UserLoginLog;
import com.aipo.backend.domain.user.entity.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface UserLoginLogRepository extends JpaRepository<UserLoginLog, Long> {

    @Query("SELECT l FROM UserLoginLog l WHERE " +
           "(:loginId IS NULL OR l.loginId LIKE %:loginId%) AND " +
           "(:role IS NULL OR l.role = :role) AND " +
           "(:from IS NULL OR l.loggedInAt >= :from) AND " +
           "(:to IS NULL OR l.loggedInAt <= :to)")
    Page<UserLoginLog> findAllByFilter(
            @Param("loginId") String loginId,
            @Param("role") UserRole role,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            Pageable pageable);
}
