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
        name = "ipo_lead_manager",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_ipo_lead_manager_stock_order",
                        columnNames = {"stock_id", "display_order"}
                )
        }
)
public class IpoLeadManager {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "lead_manager_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stock_id", nullable = false)
    private IpoStock stock;

    @Column(name = "manager_name", nullable = false, length = 100)
    private String managerName;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public static IpoLeadManager create(IpoStock stock, String managerName, Integer displayOrder) {
        IpoLeadManager leadManager = new IpoLeadManager();
        leadManager.stock = stock;
        leadManager.managerName = managerName;
        leadManager.displayOrder = displayOrder;
        leadManager.createdAt = LocalDateTime.now();
        return leadManager;
    }
}
