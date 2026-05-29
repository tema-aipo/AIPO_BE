package com.aipo.backend.domain.chat.repository;

import com.aipo.backend.domain.chat.entity.ChatFeedback;
import com.aipo.backend.domain.chat.entity.FeedbackReasonCode;
import com.aipo.backend.domain.chat.entity.FeedbackType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.Optional;

public interface ChatFeedbackRepository extends JpaRepository<ChatFeedback, Long> {

    Optional<ChatFeedback> findByChatMessage_IdAndUserId(Long messageId, Long userId);

    @EntityGraph(attributePaths = {"chatMessage", "chatMessage.chatSession"})
    @Query("""
            SELECT f
            FROM ChatFeedback f
            WHERE (:feedbackType IS NULL OR f.feedbackType = :feedbackType)
              AND (:reasonCode IS NULL OR f.reasonCode = :reasonCode)
              AND (:from IS NULL OR f.updatedAt >= :from)
              AND (:to IS NULL OR f.updatedAt <= :to)
            """)
    Page<ChatFeedback> findAllByAdminFilter(
            FeedbackType feedbackType,
            FeedbackReasonCode reasonCode,
            LocalDateTime from,
            LocalDateTime to,
            Pageable pageable
    );
}
