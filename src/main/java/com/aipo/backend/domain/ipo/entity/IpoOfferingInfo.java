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
        name = "ipo_offering_info",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_ipo_offering_info_stock", columnNames = {"stock_id"})
        }
)
public class IpoOfferingInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "offering_info_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stock_id", nullable = false, unique = true)
    private IpoStock stock;

    @Column(name = "market_cap", precision = 20, scale = 2)
    private BigDecimal marketCap;

    @Column(name = "equal_allocation_ratio", precision = 5, scale = 2)
    private BigDecimal equalAllocationRatio;

    @Column(name = "circulating_ratio", precision = 5, scale = 2)
    private BigDecimal circulatingRatio;

    @Column(name = "old_share_sale_ratio", precision = 5, scale = 2)
    private BigDecimal oldShareSaleRatio;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
