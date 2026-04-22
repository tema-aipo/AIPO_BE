package com.aipo.backend.domain.chat.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "chat_feedback",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_chat_feedback_message_user", columnNames = {"chat_message_id", "user_id"})
        }
)
public class ChatFeedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "chat_feedback_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_message_id", nullable = false)
    private ChatMessage chatMessage;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "feedback_type", nullable = false, length = 20)
    private FeedbackType feedbackType;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason_code", length = 50)
    private FeedbackReasonCode reasonCode;

    @Column(name = "reason_detail", length = 255)
    private String reasonDetail;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public static ChatFeedback create(
            ChatMessage chatMessage,
            Long userId,
            FeedbackType feedbackType,
            FeedbackReasonCode reasonCode,
            String reasonDetail
    ) {
        ChatFeedback chatFeedback = new ChatFeedback();
        LocalDateTime now = LocalDateTime.now();
        chatFeedback.chatMessage = chatMessage;
        chatFeedback.userId = userId;
        chatFeedback.feedbackType = feedbackType;
        chatFeedback.reasonCode = reasonCode;
        chatFeedback.reasonDetail = reasonDetail;
        chatFeedback.createdAt = now;
        chatFeedback.updatedAt = now;
        return chatFeedback;
    }

    public void update(FeedbackType feedbackType, FeedbackReasonCode reasonCode, String reasonDetail) {
        this.feedbackType = feedbackType;
        this.reasonCode = reasonCode;
        this.reasonDetail = reasonDetail;
        this.updatedAt = LocalDateTime.now();
    }
}
