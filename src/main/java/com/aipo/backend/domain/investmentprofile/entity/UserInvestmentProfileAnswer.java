package com.aipo.backend.domain.investmentprofile.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "user_investment_profile_answer")
public class UserInvestmentProfileAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "answer_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "result_id", nullable = false)
    private UserInvestmentProfileResult result;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private InvestmentProfileQuestion question;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "option_id", nullable = false)
    private InvestmentProfileOption option;

    @Column(name = "selected_score", nullable = false)
    private Integer selectedScore;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public static UserInvestmentProfileAnswer create(
            UserInvestmentProfileResult result,
            InvestmentProfileQuestion question,
            InvestmentProfileOption option
    ) {
        UserInvestmentProfileAnswer answer = new UserInvestmentProfileAnswer();
        answer.result = result;
        answer.question = question;
        answer.option = option;
        answer.selectedScore = option.getScore();
        answer.createdAt = LocalDateTime.now();
        return answer;
    }
}
