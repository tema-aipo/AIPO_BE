package com.aipo.backend.domain.external.opendart.entity;

import com.aipo.backend.domain.ipo.entity.IpoStock;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
        name = "ipo_external_company_profile",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_ipo_external_company_profile_provider_corp",
                        columnNames = {"provider", "dart_corp_code"}
                )
        }
)
public class IpoExternalCompanyProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ipo_external_company_profile_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stock_id")
    private IpoStock stock;

    @Column(name = "provider", nullable = false, length = 30)
    private String provider;

    @Column(name = "dart_corp_code", nullable = false, length = 8)
    private String dartCorpCode;

    @Column(name = "corp_name", length = 100)
    private String corpName;

    @Column(name = "corp_name_eng", length = 200)
    private String corpNameEng;

    @Column(name = "stock_name", length = 100)
    private String stockName;

    @Column(name = "stock_code", length = 20)
    private String stockCode;

    @Column(name = "market_type", length = 20)
    private String marketType;

    @Column(name = "ceo_name", length = 100)
    private String ceoName;

    @Column(name = "corporation_registration_no", length = 30)
    private String corporationRegistrationNo;

    @Column(name = "business_registration_no", length = 30)
    private String businessRegistrationNo;

    @Column(name = "address", length = 500)
    private String address;

    @Column(name = "homepage_url", length = 500)
    private String homepageUrl;

    @Column(name = "ir_url", length = 500)
    private String irUrl;

    @Column(name = "phone_number", length = 50)
    private String phoneNumber;

    @Column(name = "fax_number", length = 50)
    private String faxNumber;

    @Column(name = "industry_code", length = 30)
    private String industryCode;

    @Column(name = "established_date")
    private LocalDate establishedDate;

    @Column(name = "fiscal_month", length = 10)
    private String fiscalMonth;

    @Column(name = "raw_response_id")
    private Long rawResponseId;

    @Column(name = "collected_at", nullable = false)
    private LocalDateTime collectedAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public static IpoExternalCompanyProfile create(String provider, String dartCorpCode) {
        IpoExternalCompanyProfile profile = new IpoExternalCompanyProfile();
        profile.provider = provider;
        profile.dartCorpCode = dartCorpCode;
        profile.collectedAt = LocalDateTime.now();
        profile.updatedAt = profile.collectedAt;
        return profile;
    }

    public void updateData(
            IpoStock stock,
            String corpName,
            String corpNameEng,
            String stockName,
            String stockCode,
            String marketType,
            String ceoName,
            String corporationRegistrationNo,
            String businessRegistrationNo,
            String address,
            String homepageUrl,
            String irUrl,
            String phoneNumber,
            String faxNumber,
            String industryCode,
            LocalDate establishedDate,
            String fiscalMonth,
            Long rawResponseId
    ) {
        this.stock = stock;
        this.corpName = corpName;
        this.corpNameEng = corpNameEng;
        this.stockName = stockName;
        this.stockCode = stockCode;
        this.marketType = marketType;
        this.ceoName = ceoName;
        this.corporationRegistrationNo = corporationRegistrationNo;
        this.businessRegistrationNo = businessRegistrationNo;
        this.address = address;
        this.homepageUrl = homepageUrl;
        this.irUrl = irUrl;
        this.phoneNumber = phoneNumber;
        this.faxNumber = faxNumber;
        this.industryCode = industryCode;
        this.establishedDate = establishedDate;
        this.fiscalMonth = fiscalMonth;
        this.rawResponseId = rawResponseId;
        this.updatedAt = LocalDateTime.now();
    }
}
