package com.aipo.backend.domain.chatbot.entity;

import com.aipo.backend.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "chatbot_log",
        indexes = {
                @Index(name = "idx_chatbot_log_user_id", columnList = "user_id"),
                @Index(name = "idx_chatbot_log_session_id", columnList = "session_id"),
                @Index(name = "idx_chatbot_log_created_at", columnList = "created_at DESC")
        })
@Getter
@NoArgsConstructor
public class ChatbotLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "log_id")
    private Long logId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "session_id", nullable = false, length = 100)
    private String sessionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "message_role", nullable = false, length = 20)
    private MessageRole messageRole;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "token_count")
    private Integer tokenCount;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public ChatbotLog(User user, String sessionId, MessageRole messageRole, String content, Integer tokenCount) {
        this.user = user;
        this.sessionId = sessionId;
        this.messageRole = messageRole;
        this.content = content;
        this.tokenCount = tokenCount;
        this.createdAt = LocalDateTime.now();
    }
}
