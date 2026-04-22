package com.aipo.backend.domain.chat.repository;

import com.aipo.backend.domain.chat.entity.ChatMessage;
import com.aipo.backend.domain.chat.entity.MessageRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    List<ChatMessage> findAllByChatSession_IdOrderBySequenceNoAsc(Long sessionId);

    Optional<ChatMessage> findTopByChatSession_IdOrderBySequenceNoDesc(Long sessionId);

    long countByChatSession_IdAndMessageRole(Long sessionId, MessageRole messageRole);
}
