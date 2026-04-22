package com.aipo.backend.domain.investmentprofile.repository;

import com.aipo.backend.domain.investmentprofile.entity.InvestmentProfileQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface InvestmentProfileQuestionRepository extends JpaRepository<InvestmentProfileQuestion, Long> {

    List<InvestmentProfileQuestion> findAllByVersionAndActiveTrueOrderByQuestionOrderAsc(Integer version);

    @Query("""
            select max(question.version)
              from InvestmentProfileQuestion question
             where question.active = true
            """)
    Integer findCurrentVersion();
}
