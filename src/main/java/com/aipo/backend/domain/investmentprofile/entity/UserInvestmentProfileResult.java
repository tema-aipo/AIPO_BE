package com.aipo.backend.domain.investmentprofile.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "user_investment_profile_result")
public class UserInvestmentProfileResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "result_id")
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "version", nullable = false)
    private Integer version;

    @Enumerated(EnumType.STRING)
    @Column(name = "test_status", nullable = false, length = 20)
    private InvestmentProfileTestStatus testStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "profile_type", length = 20)
    private InvestmentProfileType profileType;

    @Column(name = "total_score")
    private Integer totalScore;

    @Column(name = "is_current", nullable = false)
    private boolean current;

    @Column(name = "calculated_at")
    private LocalDateTime calculatedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public static UserInvestmentProfileResult createNotTested(Long userId, Integer version) {
        UserInvestmentProfileResult result = new UserInvestmentProfileResult();
        LocalDateTime now = LocalDateTime.now();
        result.userId = userId;
        result.version = version;
        result.testStatus = InvestmentProfileTestStatus.NOT_TESTED;
        result.current = true;
        result.createdAt = now;
        result.updatedAt = now;
        return result;
    }

    public static UserInvestmentProfileResult createSkipped(Long userId, Integer version) {
        UserInvestmentProfileResult result = new UserInvestmentProfileResult();
        LocalDateTime now = LocalDateTime.now();
        result.userId = userId;
        result.version = version;
        result.testStatus = InvestmentProfileTestStatus.SKIPPED;
        result.current = true;
        result.calculatedAt = now;
        result.createdAt = now;
        result.updatedAt = now;
        return result;
    }

    public static UserInvestmentProfileResult createCompleted(
            Long userId,
            Integer version,
            InvestmentProfileType profileType,
            Integer totalScore
    ) {
        UserInvestmentProfileResult result = new UserInvestmentProfileResult();
        LocalDateTime now = LocalDateTime.now();
        result.userId = userId;
        result.version = version;
        result.testStatus = InvestmentProfileTestStatus.COMPLETED;
        result.profileType = profileType;
        result.totalScore = totalScore;
        result.current = true;
        result.calculatedAt = now;
        result.createdAt = now;
        result.updatedAt = now;
        return result;
    }
}
