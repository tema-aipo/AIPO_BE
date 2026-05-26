package com.aipo.backend.domain.ipo.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "ipo_subscription_company")
public class IpoSubscriptionCompany {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "subscription_company_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stock_id", nullable = false, columnDefinition = "INT UNSIGNED")
    private IpoStock stock;

    @Column(name = "securities_company_name", nullable = false, length = 100)
    private String securitiesCompanyName;

    @Column(name = "allocated_share_count")
    private Integer allocatedShareCount;

    @Column(name = "subscription_limit_share_count")
    private Integer subscriptionLimitShareCount;

    @Column(name = "note", length = 100)
    private String note;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
