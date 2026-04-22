package com.aipo.backend.domain.chat.repository;

import com.aipo.backend.domain.chat.entity.ChatRecommendedQuestion;
import com.aipo.backend.domain.chat.entity.RecommendedQuestionSourceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ChatRecommendedQuestionRepository extends JpaRepository<ChatRecommendedQuestion, Long> {

    @Query("""
            select question
              from ChatRecommendedQuestion question
             where question.active = true
               and question.sourceType = :sourceType
               and question.targetUserId is null
               and question.stockId is null
               and (question.validFrom is null or question.validFrom <= :now)
               and (question.validTo is null or question.validTo >= :now)
             order by question.displayOrder asc
            """)
    List<ChatRecommendedQuestion> findActiveCommonQuestions(
            @Param("sourceType") RecommendedQuestionSourceType sourceType,
            @Param("now") LocalDateTime now
    );
}
