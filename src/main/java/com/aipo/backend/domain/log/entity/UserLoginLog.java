package com.aipo.backend.domain.log.entity;

import com.aipo.backend.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_login_log",
        indexes = @Index(name = "idx_login_log_user_id", columnList = "user_id"))
@Getter
@NoArgsConstructor
public class UserLoginLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "log_id")
    private Long logId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "login_id", nullable = false, length = 50)
    private String loginId;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private com.aipo.backend.domain.user.entity.UserRole role;

    @Column(name = "logged_in_at", nullable = false)
    private LocalDateTime loggedInAt;

    public UserLoginLog(User user) {
        this.user = user;
        this.loginId = user.getLoginId();
        this.role = user.getRole();
        this.loggedInAt = LocalDateTime.now();
    }
}
