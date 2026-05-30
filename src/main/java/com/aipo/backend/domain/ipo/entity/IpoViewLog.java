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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "ipo_view_log")
public class IpoViewLog {

    private static final ZoneId KOREA_ZONE = ZoneId.of("Asia/Seoul");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "view_log_id")
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stock_id", nullable = false, columnDefinition = "INT UNSIGNED")
    private IpoStock stock;

    @Column(name = "viewed_at", nullable = false)
    private LocalDateTime viewedAt;

    @Column(name = "source", length = 30)
    private String source;

    public static IpoViewLog create(Long userId, IpoStock stock, String source) {
        IpoViewLog viewLog = new IpoViewLog();
        viewLog.userId = userId;
        viewLog.stock = stock;
        viewLog.viewedAt = LocalDateTime.now(KOREA_ZONE);
        viewLog.source = source;
        return viewLog;
    }
}
