package com.aipo.backend.domain.ipo.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "ipo_view_log")
public class IpoViewLog {

    // 조회 로그 PK
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "view_log_id")
    private Long id;

    // 조회한 사용자 ID
    // 비로그인 사용자는 null일 수도 있음
    @Column(name = "user_id")
    private Long userId;

    // 어떤 공모주를 조회했는지 연결
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stock_id", nullable = false)
    private IpoStock stock;

    // 조회 시각
    @Column(name = "viewed_at", nullable = false)
    private LocalDateTime viewedAt;

    // 조회 출처 (HOME, DETAIL, SEARCH 등)
    @Column(name = "source", length = 30)
    private String source;
}