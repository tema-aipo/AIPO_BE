package com.aipo.backend.domain.ipo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "user_favorite_stock",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_user_favorite_stock", columnNames = {"user_id", "stock_id"})
        }
)
public class UserFavoriteStock {

    private static final String ALERT_ENABLED = "Y";
    private static final String ALERT_DISABLED = "N";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "favorite_id")
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stock_id", nullable = false, columnDefinition = "INT UNSIGNED")
    private IpoStock stock;

    @Column(name = "display_order")
    private Integer displayOrder;

    @Column(name = "alert_priority")
    private Integer alertPriority;

    @Column(name = "alert_yn", length = 1)
    private String alertYn;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public static UserFavoriteStock create(Long userId, IpoStock stock) {
        UserFavoriteStock favoriteStock = new UserFavoriteStock();
        favoriteStock.userId = userId;
        favoriteStock.stock = stock;
        favoriteStock.alertYn = ALERT_ENABLED;
        favoriteStock.createdAt = LocalDateTime.now();
        return favoriteStock;
    }

    public boolean isNotificationEnabled() {
        return !ALERT_DISABLED.equalsIgnoreCase(alertYn);
    }

    public void updateNotificationEnabled(boolean enabled) {
        this.alertYn = enabled ? ALERT_ENABLED : ALERT_DISABLED;
    }
}
