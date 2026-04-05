package com.aipo.backend.domain.auth.repository;

import com.aipo.backend.domain.auth.entity.UserRefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRefreshTokenRepository extends JpaRepository<UserRefreshToken, Long> {
    Optional<UserRefreshToken> findByRefreshToken(String refreshToken);
    Optional<UserRefreshToken> findByUser_UserId(Long userId);
    void deleteByUser_UserId(Long userId);
}