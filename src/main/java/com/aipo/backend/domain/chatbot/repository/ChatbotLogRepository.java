package com.aipo.backend.domain.chatbot.repository;

import com.aipo.backend.domain.chatbot.entity.ChatbotLog;
import com.aipo.backend.domain.chat.entity.MessageRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface ChatbotLogRepository extends JpaRepository<ChatbotLog, Long> {

    @Query("SELECT c FROM ChatbotLog c WHERE " +
            "(:sessionId IS NULL OR c.sessionId = :sessionId) AND " +
            "(:messageRole IS NULL OR c.messageRole = :messageRole) AND " +
            "(:from IS NULL OR c.createdAt >= :from) AND " +
            "(:to IS NULL OR c.createdAt <= :to)")
    Page<ChatbotLog> findAllByFilter(
            @Param("sessionId") String sessionId,
            @Param("messageRole") MessageRole messageRole,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            Pageable pageable);

    long countByCreatedAtAfter(LocalDateTime after);

    // ✨ 추가: 챗봇 답변 제외하고 순수 사용자 질문 수만 카운트하기 위한 메서드
    long countByMessageRole(MessageRole messageRole);

    // ✨ 추가: 기간별 순수 사용자 질문 수 카운트 메서드
    long countByMessageRoleAndCreatedAtAfter(MessageRole messageRole, LocalDateTime after);

    @Query("SELECT COUNT(DISTINCT c.sessionId) FROM ChatbotLog c")
    long countDistinctSessions();

    @Query("SELECT COUNT(DISTINCT c.sessionId) FROM ChatbotLog c WHERE c.createdAt >= :after")
    long countDistinctSessionsAfter(@Param("after") LocalDateTime after);

    @Query("SELECT COALESCE(SUM(c.tokenCount), 0) FROM ChatbotLog c WHERE c.createdAt >= :after")
    long sumTokenCountAfter(@Param("after") LocalDateTime after);
}