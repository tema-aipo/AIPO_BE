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
        name = "ipo_deposit_info",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_ipo_deposit_info_stock_company",
                        columnNames = {"stock_id", "securities_company_name"}
                ),
                @UniqueConstraint(
                        name = "uk_ipo_deposit_info_stock_order",
                        columnNames = {"stock_id", "display_order"}
                )
        }
)
public class IpoDepositInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "deposit_info_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stock_id", nullable = false)
    private IpoStock stock;

    @Column(name = "securities_company_name", nullable = false, length = 100)
    private String securitiesCompanyName;

    @Column(name = "amount_for_ten_shares", precision = 15, scale = 2)
    private BigDecimal amountForTenShares;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
