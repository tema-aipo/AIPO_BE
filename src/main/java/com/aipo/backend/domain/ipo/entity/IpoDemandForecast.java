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
        name = "ipo_demand_forecast",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_ipo_demand_forecast_stock", columnNames = {"stock_id"})
        }
)
public class IpoDemandForecast {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "forecast_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stock_id", nullable = false, unique = true, columnDefinition = "INT UNSIGNED")
    private IpoStock stock;

    @Column(name = "institutional_competition_rate", precision = 10, scale = 2)
    private BigDecimal institutionalCompetitionRate;

    @Column(name = "participating_institution_count")
    private Integer participatingInstitutionCount;

    @Column(name = "above_upper_price_competition_rate", precision = 10, scale = 2)
    private BigDecimal aboveUpperPriceCompetitionRate;

    @Column(name = "above_upper_price_institution_count")
    private Integer aboveUpperPriceInstitutionCount;

    @Column(name = "lockup_competition_rate", precision = 10, scale = 2)
    private BigDecimal lockupCompetitionRate;

    @Column(name = "lockup_institution_count")
    private Integer lockupInstitutionCount;

    @Column(name = "lockup_rate", precision = 5, scale = 2)
    private BigDecimal lockupRate;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
