package com.aipo.backend.domain.ipo.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "ipo_subscription_competition",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_ipo_subscription_competition_stock", columnNames = {"stock_id"})
        }
)
public class IpoSubscriptionCompetition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "competition_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stock_id", nullable = false, unique = true, columnDefinition = "INT UNSIGNED")
    private IpoStock stock;

    @Enumerated(EnumType.STRING)
    @Column(name = "default_tab", length = 20)
    private CompetitionTabType defaultTab;

    @Column(name = "equal_expected_allocation_quantity", precision = 10, scale = 2)
    private BigDecimal equalExpectedAllocationQuantity;

    @Column(name = "equal_competition_rate", precision = 10, scale = 2)
    private BigDecimal equalCompetitionRate;

    @Column(name = "proportional_expected_allocation_quantity", precision = 10, scale = 2)
    private BigDecimal proportionalExpectedAllocationQuantity;

    @Column(name = "proportional_competition_rate", precision = 10, scale = 2)
    private BigDecimal proportionalCompetitionRate;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
