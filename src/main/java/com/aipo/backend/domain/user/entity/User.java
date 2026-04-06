package com.aipo.backend.domain.user.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "app_user")
@Getter
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "login_id", nullable = false, unique = true, length = 50)
    private String loginId;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "user_name", nullable = false, length = 30)
    private String userName;

    @Column(name = "email", length = 100)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private UserRole role;

    @Enumerated(EnumType.STRING)
    @Column(name = "user_status", nullable = false, length = 20)
    private UserStatus userStatus;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Column(name = "password_updated_at")
    private LocalDateTime passwordUpdatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public User(String loginId, String passwordHash, String userName, String email) {
        this.loginId = loginId;
        this.passwordHash = passwordHash;
        this.userName = userName;
        this.email = email;
        this.role = UserRole.USER;
        this.userStatus = UserStatus.ACTIVE;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public static User createAdmin(String loginId, String passwordHash, String userName, String email) {
        User user = new User(loginId, passwordHash, userName, email);
        user.role = UserRole.ADMIN;
        return user;
    }

    public void updateLastLoginAt() {
        this.lastLoginAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
}
// NOTE:
// 현재는 USER/ACTIVE 중심의 최소 인증 구조만 사용한다.
// 추후 관리자 웹, 회원탈퇴, 계정 정지 정책이 추가되면
// role 및 userStatus 값 범위를 확장하고 관련 검증 로직을 보완해야 한다.
// 비밀번호, 이메일 정책 추가 예정