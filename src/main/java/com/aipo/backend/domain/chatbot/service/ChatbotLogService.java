package com.aipo.backend.domain.chatbot.service;

import com.aipo.backend.domain.chatbot.entity.ChatbotLog;
import com.aipo.backend.domain.chat.entity.MessageRole; // 패키지 경로 주의!
import com.aipo.backend.domain.chatbot.repository.ChatbotLogRepository;
import com.aipo.backend.domain.user.entity.User;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class ChatbotLogService {

    private final ChatbotLogRepository chatbotLogRepository;

    public ChatbotLog record(User user, String sessionId, MessageRole messageRole,
                             String content, Integer tokenCount) {
        return chatbotLogRepository.save(new ChatbotLog(user, sessionId, messageRole, content, tokenCount));
    }

    @Transactional(readOnly = true)
    public Page<ChatbotLogResponse> getLogs(String sessionId, MessageRole messageRole,
                                            LocalDateTime from, LocalDateTime to,
                                            Pageable pageable) {
        return chatbotLogRepository.findAllByFilter(sessionId, messageRole, from, to, pageable)
                .map(ChatbotLogResponse::from);
    }

    @Transactional(readOnly = true)
    public ChatbotStats getStats() {
        LocalDateTime todayStart = LocalDateTime.now().toLocalDate().atStartOfDay();
        LocalDateTime weekAgo = LocalDateTime.now().minusDays(7);

        // ✨ 수정 완료: 이제 챗봇(ASSISTANT) 대답은 무시하고 사용자(USER) 질문만 셉니다!
        long totalMessages = chatbotLogRepository.countByMessageRole(MessageRole.USER);
        long todayMessages = chatbotLogRepository.countByMessageRoleAndCreatedAtAfter(MessageRole.USER, todayStart);
        long weeklyMessages = chatbotLogRepository.countByMessageRoleAndCreatedAtAfter(MessageRole.USER, weekAgo);

        long totalSessions = chatbotLogRepository.countDistinctSessions();
        long todaySessions = chatbotLogRepository.countDistinctSessionsAfter(todayStart);
        long weeklyTokens = chatbotLogRepository.sumTokenCountAfter(weekAgo);

        return new ChatbotStats(totalMessages, totalSessions, todayMessages, todaySessions,
                weeklyMessages, weeklyTokens);
    }

    @Getter
    @AllArgsConstructor
    public static class ChatbotLogResponse {
        private Long logId;
        private String sessionId;
        private MessageRole messageRole;
        private String content;
        private Integer tokenCount;
        private LocalDateTime createdAt;
        private Boolean isLiked; // ✨ 응답에도 좋아요 상태 추가!

        public static ChatbotLogResponse from(ChatbotLog log) {
            return new ChatbotLogResponse(
                    log.getLogId(),
                    log.getSessionId(),
                    log.getMessageRole(),
                    log.getContent(),
                    log.getTokenCount(),
                    log.getCreatedAt(),
                    log.getIsLiked()
            );
        }
    }

    @Getter
    @AllArgsConstructor
    public static class ChatbotStats {
        private long totalMessages;
        private long totalSessions;
        private long todayMessages;
        private long todaySessions;
        private long weeklyMessages;
        private long weeklyTokens;
    }
}