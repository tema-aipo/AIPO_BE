package com.aipo.backend.domain.user.repository;

import com.aipo.backend.domain.user.entity.User;
import com.aipo.backend.domain.user.entity.UserRole;
import com.aipo.backend.domain.user.entity.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByLoginId(String loginId);
    boolean existsByLoginId(String loginId);
    boolean existsByEmail(String email);
    boolean existsByEmailAndUserIdNot(String email, Long userId);

    // 👇 삭제할 코드 (관리자가 섞여버림)
    // long countByRole(UserRole role);
    // long countByUserStatus(UserStatus status);

    // ✨ 추가할 코드 1: [전체 회원 수용 - 방식 B] 특정 권한(USER) 중 특정 상태(WITHDRAWN)가 '아닌' 사람만!
    long countByRoleAndUserStatusNot(UserRole role, UserStatus status);

    // ✨ 추가할 코드 2: [상태별 회원 수용] 특정 권한(USER) 중 특정 상태(ACTIVE or WITHDRAWN)인 사람만!
    long countByRoleAndUserStatus(UserRole role, UserStatus status);

    long countByRoleAndCreatedAtAfter(UserRole role, LocalDateTime after);

    @Query("SELECT u FROM User u WHERE u.role = :role " +
            "AND (:keyword IS NULL OR u.loginId LIKE %:keyword% OR u.userName LIKE %:keyword% OR u.email LIKE %:keyword%) " +
            "AND (:status IS NULL OR u.userStatus = :status)")
    Page<User> findAllByRoleAndFilter(
            @Param("role") UserRole role,
            @Param("keyword") String keyword,
            @Param("status") UserStatus status,
            Pageable pageable);
}