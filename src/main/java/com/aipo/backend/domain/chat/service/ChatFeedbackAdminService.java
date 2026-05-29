package com.aipo.backend.domain.chat.service;

import com.aipo.backend.domain.chat.entity.ChatFeedback;
import com.aipo.backend.domain.chat.entity.FeedbackReasonCode;
import com.aipo.backend.domain.chat.entity.FeedbackType;
import com.aipo.backend.domain.chat.repository.ChatFeedbackRepository;
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
@Transactional(readOnly = true)
public class ChatFeedbackAdminService {

    private final ChatFeedbackRepository chatFeedbackRepository;

    public Page<ChatFeedbackAdminResponse> getFeedbacks(
            FeedbackType feedbackType,
            FeedbackReasonCode reasonCode,
            LocalDateTime from,
            LocalDateTime to,
            Pageable pageable
    ) {
        return chatFeedbackRepository.findAllByAdminFilter(feedbackType, reasonCode, from, to, pageable)
                .map(ChatFeedbackAdminResponse::from);
    }

    @Getter
    @AllArgsConstructor
    public static class ChatFeedbackAdminResponse {
        private Long feedbackId;
        private Long messageId;
        private Long sessionId;
        private Long userId;
        private FeedbackType feedbackType;
        private FeedbackReasonCode reasonCode;
        private String reasonDetail;
        private String assistantAnswer;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public static ChatFeedbackAdminResponse from(ChatFeedback feedback) {
            return new ChatFeedbackAdminResponse(
                    feedback.getId(),
                    feedback.getChatMessage().getId(),
                    feedback.getChatMessage().getChatSession().getId(),
                    feedback.getUserId(),
                    feedback.getFeedbackType(),
                    feedback.getReasonCode(),
                    feedback.getReasonDetail(),
                    feedback.getChatMessage().getContent(),
                    feedback.getCreatedAt(),
                    feedback.getUpdatedAt()
            );
        }
    }
}
