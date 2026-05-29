package com.aipo.backend.domain.chat.repository;

import com.aipo.backend.domain.chat.entity.ChatFeedback;
import com.aipo.backend.domain.chat.entity.FeedbackType; // ✨ 이거 임포트 필수!
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ChatFeedbackRepository extends JpaRepository<ChatFeedback, Long> {

    Optional<ChatFeedback> findByChatMessage_IdAndUserId(Long messageId, Long userId);

    // ✨ 새로 추가: LIKE, DISLIKE 개수를 세기 위한 갓-메서드
    long countByFeedbackType(FeedbackType feedbackType);
}