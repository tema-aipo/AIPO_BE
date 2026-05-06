package com.aipo.backend.domain.external.opendart.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "ipo_external_disclosure",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_ipo_external_disclosure_rcept_no", columnNames = "rcept_no")
        }
)
public class IpoExternalDisclosure {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ipo_external_disclosure_id")
    private Long id;

    @Column(name = "provider", nullable = false, length = 30)
    private String provider;

    @Column(name = "rcept_no", nullable = false, length = 14)
    private String rceptNo;

    @Column(name = "corp_code", nullable = false, length = 8)
    private String corpCode;

    @Column(name = "corp_name", nullable = false, length = 100)
    private String corpName;

    @Column(name = "corp_cls", length = 1)
    private String corpCls;

    @Column(name = "stock_code", length = 20)
    private String stockCode;

    @Column(name = "report_name", length = 255)
    private String reportName;

    @Column(name = "received_date")
    private LocalDate receivedDate;

    @Column(name = "processed", nullable = false)
    private boolean processed;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public static IpoExternalDisclosure create(
            String rceptNo,
            String corpCode,
            String corpName,
            String corpCls,
            String stockCode,
            String reportName,
            LocalDate receivedDate
    ) {
        IpoExternalDisclosure disclosure = new IpoExternalDisclosure();
        disclosure.provider = "OPENDART";
        disclosure.rceptNo = rceptNo;
        disclosure.corpCode = corpCode;
        disclosure.corpName = corpName;
        disclosure.corpCls = corpCls;
        disclosure.stockCode = stockCode;
        disclosure.reportName = reportName;
        disclosure.receivedDate = receivedDate;
        disclosure.processed = false;
        disclosure.createdAt = LocalDateTime.now();
        disclosure.updatedAt = disclosure.createdAt;
        return disclosure;
    }

    public void markProcessed() {
        this.processed = true;
        this.processedAt = LocalDateTime.now();
        this.updatedAt = this.processedAt;
    }
}
