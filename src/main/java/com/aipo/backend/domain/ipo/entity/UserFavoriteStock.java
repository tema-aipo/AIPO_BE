package com.aipo.backend.domain.ipo.entity;

import jakarta.persistence.*;
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
                // 한 사용자가 같은 종목을 중복 관심종목으로 등록하지 못하도록 제한
                @UniqueConstraint(name = "uk_user_favorite_stock", columnNames = {"user_id", "stock_id"})
        }
)
public class UserFavoriteStock {

    // 관심종목 PK
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "favorite_id")
    private Long id;

    // 사용자 ID
    @Column(name = "user_id", nullable = false)
    private Long userId;

    // 관심 등록된 공모주
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stock_id", nullable = false)
    private IpoStock stock;

    // 관심종목 표시 순서
    @Column(name = "display_order")
    private Integer displayOrder;

    // 알림 우선순위
    @Column(name = "alert_priority")
    private Integer alertPriority;

    // 알림 여부 (Y/N)
    @Column(name = "alert_yn", length = 1)
    private String alertYn;

    // 생성 시각
    @Column(name = "created_at")
    private LocalDateTime createdAt;
}