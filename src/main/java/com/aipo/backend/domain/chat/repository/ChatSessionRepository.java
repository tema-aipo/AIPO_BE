package com.aipo.backend.domain.chat.repository;

import com.aipo.backend.domain.chat.entity.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ChatSessionRepository extends JpaRepository<ChatSession, Long> {

    List<ChatSession> findAllByUserIdAndDeletedFalseOrderByPinnedDescLastMessageAtDesc(Long userId);

    Optional<ChatSession> findByIdAndUserIdAndDeletedFalse(Long sessionId, Long userId);

    long countByUserIdAndPinnedTrueAndDeletedFalse(Long userId);

    @Modifying
    @Query("""
            update ChatSession session
               set session.deleted = true,
                   session.deletedAt = :deletedAt,
                   session.updatedAt = :deletedAt
             where session.userId = :userId
               and session.deleted = false
               and session.pinned = false
            """)
    int softDeleteRecentSessions(@Param("userId") Long userId, @Param("deletedAt") LocalDateTime deletedAt);
}
