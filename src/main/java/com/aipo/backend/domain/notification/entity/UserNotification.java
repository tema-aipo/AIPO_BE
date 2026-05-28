package com.aipo.backend.domain.notification.entity;

import com.aipo.backend.domain.ipo.entity.IpoStock;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "user_notification")
public class UserNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_id")
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stock_id", columnDefinition = "INT UNSIGNED")
    private IpoStock stock;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false, length = 40)
    private NotificationType notificationType;

    @Column(name = "title", nullable = false, length = 100)
    private String title;

    @Column(name = "content", nullable = false, length = 500)
    private String content;

    @Column(name = "target_date")
    private LocalDate targetDate;

    @Column(name = "is_read", nullable = false)
    private boolean read;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @Column(name = "is_deleted", nullable = false)
    private boolean deleted;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public static UserNotification create(
            Long userId,
            IpoStock stock,
            NotificationType notificationType,
            String title,
            String content,
            LocalDate targetDate
    ) {
        UserNotification notification = new UserNotification();
        notification.userId = userId;
        notification.stock = stock;
        notification.notificationType = notificationType;
        notification.title = title;
        notification.content = content;
        notification.targetDate = targetDate;
        notification.read = false;
        notification.deleted = false;
        notification.createdAt = LocalDateTime.now();
        return notification;
    }

    public void markRead() {
        if (read) {
            return;
        }
        this.read = true;
        this.readAt = LocalDateTime.now();
    }

    public void delete() {
        this.deleted = true;
    }
}
