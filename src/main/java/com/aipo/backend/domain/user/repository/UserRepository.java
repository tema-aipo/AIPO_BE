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
    boolean existsByLoginId(String loginId);
    Optional<User> findByLoginId(String loginId);

    long countByRole(UserRole role);
    long countByUserStatus(UserStatus status);
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
