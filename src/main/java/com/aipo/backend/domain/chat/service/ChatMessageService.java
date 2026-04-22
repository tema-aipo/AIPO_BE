package com.aipo.backend.domain.chat.service;

import com.aipo.backend.domain.chat.dto.ChatFeedbackItem;
import com.aipo.backend.domain.chat.dto.ChatMessageItem;
import com.aipo.backend.domain.chat.dto.SendChatMessageResponse;
import com.aipo.backend.domain.chat.entity.ChatMessage;
import com.aipo.backend.domain.chat.entity.ChatSession;
import com.aipo.backend.domain.chat.entity.MessageRole;
import com.aipo.backend.domain.chat.entity.MessageType;
import com.aipo.backend.domain.chat.repository.ChatMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatMessageService {

    private final ChatSessionService chatSessionService;
    private final ChatMessageRepository chatMessageRepository;

    @Transactional
    public SendChatMessageResponse sendMessage(Long userId, Long sessionId, String question) {
        ChatSession chatSession = chatSessionService.getSession(userId, sessionId);
        long userMessageCount = chatMessageRepository.countByChatSession_IdAndMessageRole(sessionId, MessageRole.USER);

        int nextSequenceNo = chatMessageRepository.findTopByChatSession_IdOrderBySequenceNoDesc(sessionId)
                .map(ChatMessage::getSequenceNo)
                .orElse(0) + 1;

        String normalizedQuestion = question.trim();
        ChatMessage userMessage = chatMessageRepository.save(
                ChatMessage.create(chatSession, MessageRole.USER, MessageType.TEXT, normalizedQuestion, nextSequenceNo)
        );

        ChatMessage assistantMessage = chatMessageRepository.save(
                ChatMessage.create(
                        chatSession,
                        MessageRole.ASSISTANT,
                        MessageType.TEXT,
                        createStubAnswer(normalizedQuestion),
                        nextSequenceNo + 1
                )
        );

        chatSession.touchLastMessageAt(assistantMessage.getCreatedAt());
        if (userMessageCount == 0) {
            chatSession.updateTitle(generateTitle(normalizedQuestion));
        }

        return new SendChatMessageResponse(
                chatSession.getId(),
                chatSession.getTitle(),
                toMessageItem(userMessage),
                toAssistantMessageItem(assistantMessage)
        );
    }

    String generateTitle(String question) {
        String normalized = question == null ? "" : question
                .trim()
                .replaceAll("[\\r\\n]+", " ")
                .replaceAll("\\s+", " ");

        if (normalized.isBlank()) {
            return "새 채팅";
        }

        if (normalized.length() <= 30) {
            return normalized;
        }

        return normalized.substring(0, 27) + "...";
    }

    private String createStubAnswer(String question) {
        return "임시 답변입니다. 질문하신 내용은 \"" + question + "\" 입니다. 이후 AI 연동 시 실제 답변으로 대체됩니다.";
    }

    private ChatMessageItem toMessageItem(ChatMessage message) {
        return new ChatMessageItem(
                message.getId(),
                message.getMessageRole().name(),
                message.getMessageType().name(),
                message.getContent(),
                message.getCreatedAt(),
                null
        );
    }

    private ChatMessageItem toAssistantMessageItem(ChatMessage message) {
        return new ChatMessageItem(
                message.getId(),
                message.getMessageRole().name(),
                message.getMessageType().name(),
                message.getContent(),
                message.getCreatedAt(),
                new ChatFeedbackItem(null, null, null)
        );
    }
}
