package com.aipo.backend.domain.chat.repository;

import com.aipo.backend.domain.chat.entity.ChatFeedback;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ChatFeedbackRepository extends JpaRepository<ChatFeedback, Long> {

    Optional<ChatFeedback> findByChatMessage_IdAndUserId(Long messageId, Long userId);
}
