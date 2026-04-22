package com.aipo.backend.domain.chat.service;

import com.aipo.backend.domain.chat.dto.ChatFeedbackItem;
import com.aipo.backend.domain.chat.dto.ChatMessageItem;
import com.aipo.backend.domain.chat.dto.ChatSessionDetailResponse;
import com.aipo.backend.domain.chat.dto.ChatSessionListResponse;
import com.aipo.backend.domain.chat.dto.ChatSessionPinResponse;
import com.aipo.backend.domain.chat.dto.ChatSessionSummaryItem;
import com.aipo.backend.domain.chat.dto.CreateChatSessionResponse;
import com.aipo.backend.domain.chat.dto.DeleteRecentChatSessionsResponse;
import com.aipo.backend.domain.chat.entity.ChatFeedback;
import com.aipo.backend.domain.chat.entity.ChatMessage;
import com.aipo.backend.domain.chat.entity.ChatSession;
import com.aipo.backend.domain.chat.entity.MessageRole;
import com.aipo.backend.domain.chat.repository.ChatFeedbackRepository;
import com.aipo.backend.domain.chat.repository.ChatMessageRepository;
import com.aipo.backend.domain.chat.repository.ChatSessionRepository;
import com.aipo.backend.global.exception.CustomException;
import com.aipo.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatSessionService {

    private static final String WELCOME_MESSAGE = "안녕하세요. AIPO입니다. 궁금한 공모주를 질문해보세요.";

    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatFeedbackRepository chatFeedbackRepository;
    private final ChatRecommendedQuestionService chatRecommendedQuestionService;

    @Transactional
    public CreateChatSessionResponse createSession(Long userId) {
        ChatSession chatSession = chatSessionRepository.save(ChatSession.create(userId));

        return new CreateChatSessionResponse(
                chatSession.getId(),
                chatSession.getTitle(),
                chatSession.isPinned(),
                chatSession.getCreatedAt(),
                WELCOME_MESSAGE,
                chatRecommendedQuestionService.getActiveCommonQuestions()
        );
    }

    public ChatSessionListResponse getSessions(Long userId) {
        List<ChatSessionSummaryItem> items = chatSessionRepository
                .findAllByUserIdAndDeletedFalseOrderByPinnedDescLastMessageAtDesc(userId).stream()
                .map(this::toSummaryItem)
                .toList();

        List<ChatSessionSummaryItem> pinnedSessions = items.stream()
                .filter(ChatSessionSummaryItem::pinned)
                .toList();
        List<ChatSessionSummaryItem> recentSessions = items.stream()
                .filter(item -> !item.pinned())
                .toList();

        return new ChatSessionListResponse(pinnedSessions, recentSessions);
    }

    public ChatSessionDetailResponse getSessionDetail(Long userId, Long sessionId) {
        ChatSession chatSession = getSession(userId, sessionId);

        List<ChatMessageItem> messages = chatMessageRepository.findAllByChatSession_IdOrderBySequenceNoAsc(sessionId).stream()
                .map(message -> toMessageItem(message, userId))
                .toList();

        return new ChatSessionDetailResponse(
                chatSession.getId(),
                chatSession.getTitle(),
                chatSession.isPinned(),
                messages
        );
    }

    @Transactional
    public void deleteSession(Long userId, Long sessionId) {
        ChatSession chatSession = getSession(userId, sessionId);
        chatSession.softDelete();
    }

    @Transactional
    public DeleteRecentChatSessionsResponse deleteRecentSessions(Long userId) {
        int deletedSessionCount = chatSessionRepository.softDeleteRecentSessions(userId, LocalDateTime.now());
        long keptPinnedSessionCount = chatSessionRepository.countByUserIdAndPinnedTrueAndDeletedFalse(userId);
        return new DeleteRecentChatSessionsResponse(deletedSessionCount, keptPinnedSessionCount);
    }

    @Transactional
    public ChatSessionPinResponse updatePinned(Long userId, Long sessionId, boolean pinned) {
        ChatSession chatSession = getSession(userId, sessionId);
        chatSession.updatePinned(pinned);
        return new ChatSessionPinResponse(chatSession.getId(), chatSession.isPinned());
    }

    public ChatSession getSession(Long userId, Long sessionId) {
        return chatSessionRepository.findByIdAndUserIdAndDeletedFalse(sessionId, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.CHAT_SESSION_NOT_FOUND));
    }

    private ChatSessionSummaryItem toSummaryItem(ChatSession chatSession) {
        String lastMessagePreview = chatMessageRepository.findTopByChatSession_IdOrderBySequenceNoDesc(chatSession.getId())
                .map(ChatMessage::getContent)
                .map(this::summarizePreview)
                .orElse(null);

        return new ChatSessionSummaryItem(
                chatSession.getId(),
                chatSession.getTitle(),
                chatSession.isPinned(),
                lastMessagePreview,
                chatSession.getLastMessageAt()
        );
    }

    private String summarizePreview(String content) {
        String normalized = content.replaceAll("\\s+", " ").trim();
        if (normalized.length() <= 50) {
            return normalized;
        }
        return normalized.substring(0, 47) + "...";
    }

    private ChatMessageItem toMessageItem(ChatMessage message, Long userId) {
        ChatFeedbackItem feedback = null;
        if (message.getMessageRole() == MessageRole.ASSISTANT) {
            feedback = chatFeedbackRepository.findByChatMessage_IdAndUserId(message.getId(), userId)
                    .map(this::toFeedbackItem)
                    .orElse(new ChatFeedbackItem(null, null, null));
        }

        return new ChatMessageItem(
                message.getId(),
                message.getMessageRole().name(),
                message.getMessageType().name(),
                message.getContent(),
                message.getCreatedAt(),
                feedback
        );
    }

    private ChatFeedbackItem toFeedbackItem(ChatFeedback chatFeedback) {
        return new ChatFeedbackItem(
                chatFeedback.getFeedbackType().name(),
                chatFeedback.getReasonCode() != null ? chatFeedback.getReasonCode().name() : null,
                chatFeedback.getReasonDetail()
        );
    }
}
