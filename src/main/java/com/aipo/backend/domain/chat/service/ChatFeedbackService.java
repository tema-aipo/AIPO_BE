package com.aipo.backend.domain.chat.service;

import com.aipo.backend.domain.chat.dto.SubmitChatFeedbackResponse;
import com.aipo.backend.domain.chat.entity.ChatFeedback;
import com.aipo.backend.domain.chat.entity.ChatMessage;
import com.aipo.backend.domain.chat.entity.FeedbackReasonCode;
import com.aipo.backend.domain.chat.entity.FeedbackType;
import com.aipo.backend.domain.chat.entity.MessageRole;
import com.aipo.backend.domain.chat.repository.ChatFeedbackRepository;
import com.aipo.backend.domain.chat.repository.ChatMessageRepository;
import com.aipo.backend.global.exception.CustomException;
import com.aipo.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatFeedbackService {

    private final ChatMessageRepository chatMessageRepository;
    private final ChatFeedbackRepository chatFeedbackRepository;

    @Transactional
    public SubmitChatFeedbackResponse submitFeedback(
            Long userId,
            Long messageId,
            String feedbackTypeValue,
            String reasonCodeValue,
            String reasonDetail
    ) {
        ChatMessage chatMessage = chatMessageRepository.findById(messageId)
                .orElseThrow(() -> new CustomException(ErrorCode.CHAT_MESSAGE_NOT_FOUND));

        if (chatMessage.getChatSession().isDeleted() || !chatMessage.getChatSession().getUserId().equals(userId)) {
            throw new CustomException(ErrorCode.CHAT_MESSAGE_NOT_FOUND);
        }
        if (chatMessage.getMessageRole() != MessageRole.ASSISTANT) {
            throw new CustomException(ErrorCode.INVALID_CHAT_FEEDBACK_TARGET);
        }

        FeedbackType feedbackType = parseFeedbackType(feedbackTypeValue);
        FeedbackReasonCode reasonCode = parseReasonCode(reasonCodeValue);
        validateFeedback(feedbackType, reasonCode);

        String resolvedReasonDetail = feedbackType == FeedbackType.LIKE ? null : reasonDetail;
        FeedbackReasonCode resolvedReasonCode = feedbackType == FeedbackType.LIKE ? null : reasonCode;

        ChatFeedback chatFeedback = chatFeedbackRepository.findByChatMessage_IdAndUserId(messageId, userId)
                .map(existing -> {
                    existing.update(feedbackType, resolvedReasonCode, resolvedReasonDetail);
                    return existing;
                })
                .orElseGet(() -> ChatFeedback.create(
                        chatMessage,
                        userId,
                        feedbackType,
                        resolvedReasonCode,
                        resolvedReasonDetail
                ));

        ChatFeedback savedFeedback = chatFeedbackRepository.save(chatFeedback);

        return new SubmitChatFeedbackResponse(
                messageId,
                savedFeedback.getFeedbackType().name(),
                savedFeedback.getReasonCode() != null ? savedFeedback.getReasonCode().name() : null,
                savedFeedback.getReasonDetail(),
                savedFeedback.getUpdatedAt()
        );
    }

    private FeedbackType parseFeedbackType(String feedbackTypeValue) {
        try {
            return FeedbackType.valueOf(feedbackTypeValue);
        } catch (Exception exception) {
            throw new IllegalArgumentException("feedbackType이 올바르지 않습니다.");
        }
    }

    private FeedbackReasonCode parseReasonCode(String reasonCodeValue) {
        if (reasonCodeValue == null || reasonCodeValue.isBlank()) {
            return null;
        }
        try {
            return FeedbackReasonCode.valueOf(reasonCodeValue);
        } catch (Exception exception) {
            throw new IllegalArgumentException("reasonCode가 올바르지 않습니다.");
        }
    }

    private void validateFeedback(FeedbackType feedbackType, FeedbackReasonCode reasonCode) {
        if (feedbackType == FeedbackType.DISLIKE && reasonCode == null) {
            throw new IllegalArgumentException("싫어요 피드백에는 reasonCode가 필요합니다.");
        }
    }
}
