package com.aipo.backend.domain.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "user_notification_setting")
public class UserNotificationSetting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_setting_id")
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "subscription_schedule_notification_enabled", nullable = false)
    private boolean subscriptionScheduleNotificationEnabled;

    @Column(name = "listing_date_notification_enabled", nullable = false)
    private boolean listingDateNotificationEnabled;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public static UserNotificationSetting createDefault(Long userId) {
        UserNotificationSetting setting = new UserNotificationSetting();
        LocalDateTime now = LocalDateTime.now();
        setting.userId = userId;
        setting.subscriptionScheduleNotificationEnabled = true;
        setting.listingDateNotificationEnabled = true;
        setting.createdAt = now;
        setting.updatedAt = now;
        return setting;
    }

    public void update(boolean subscriptionScheduleNotificationEnabled, boolean listingDateNotificationEnabled) {
        this.subscriptionScheduleNotificationEnabled = subscriptionScheduleNotificationEnabled;
        this.listingDateNotificationEnabled = listingDateNotificationEnabled;
        this.updatedAt = LocalDateTime.now();
    }
}
