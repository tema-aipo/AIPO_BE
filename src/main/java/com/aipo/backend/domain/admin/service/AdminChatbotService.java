package com.aipo.backend.domain.admin.service;

import com.aipo.backend.domain.chat.entity.MessageRole;
import com.aipo.backend.domain.chatbot.repository.ChatbotLogRepository;
import com.aipo.backend.domain.chatbot.service.ChatbotLogService.ChatbotLogResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminChatbotService {

    private final ChatbotLogRepository chatbotLogRepository;

    // 1. 챗봇 대화 로그 페이징 조회
    public Page<ChatbotLogResponse> getLogs(String sessionId, MessageRole messageRole,
                                            LocalDateTime from, LocalDateTime to,
                                            Pageable pageable) {
        return chatbotLogRepository.findAllByFilter(sessionId, messageRole, from, to, pageable)
                .map(ChatbotLogResponse::from);
    }
}