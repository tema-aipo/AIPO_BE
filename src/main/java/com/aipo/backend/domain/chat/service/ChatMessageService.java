package com.aipo.backend.domain.chat.service;

import com.aipo.backend.domain.chat.dto.ChatFeedbackItem;
import com.aipo.backend.domain.chat.dto.ChatMessageItem;
import com.aipo.backend.domain.chat.dto.SendChatMessageResponse;
import com.aipo.backend.domain.chat.entity.ChatMessage;
import com.aipo.backend.domain.chat.entity.ChatSession;
import com.aipo.backend.domain.chat.entity.MessageRole;
import com.aipo.backend.domain.chat.entity.MessageType;
import com.aipo.backend.domain.chat.repository.ChatMessageRepository;
import com.aipo.backend.domain.chatbot.client.PythonChatbotClient;
import com.aipo.backend.domain.chatbot.dto.PythonChatRequest;
import com.aipo.backend.domain.chatbot.dto.PythonChatResponse;
import com.aipo.backend.domain.investmentprofile.dto.InvestmentProfileResultResponse;
import com.aipo.backend.domain.investmentprofile.entity.InvestmentProfileTestStatus;
import com.aipo.backend.domain.investmentprofile.service.InvestmentProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatMessageService {

    private static final int CHAT_HISTORY_LIMIT = 10;
    private static final String DEFAULT_USER_TYPE = "NEUTRAL";

    private final ChatSessionService chatSessionService;
    private final ChatMessageRepository chatMessageRepository;
    private final PythonChatbotClient pythonChatbotClient;
    private final InvestmentProfileService investmentProfileService;

    @Transactional
    public SendChatMessageResponse sendMessage(Long userId, Long sessionId, String question) {
        ChatSession chatSession = chatSessionService.getSession(userId, sessionId);
        long userMessageCount = chatMessageRepository.countByChatSession_IdAndMessageRole(sessionId, MessageRole.USER);
        List<List<String>> chatHistory = createChatHistory(sessionId);

        int nextSequenceNo = chatMessageRepository.findTopByChatSession_IdOrderBySequenceNoDesc(sessionId)
                .map(ChatMessage::getSequenceNo)
                .orElse(0) + 1;

        String normalizedQuestion = question.trim();
        ChatMessage userMessage = chatMessageRepository.save(
                ChatMessage.create(chatSession, MessageRole.USER, MessageType.TEXT, normalizedQuestion, nextSequenceNo)
        );

        PythonChatResponse chatbotResponse = pythonChatbotClient.chat(new PythonChatRequest(
                String.valueOf(userId),
                normalizedQuestion,
                resolveCurrentUserType(userId),
                chatHistory
        ));

        ChatMessage assistantMessage = chatMessageRepository.save(
                ChatMessage.create(
                        chatSession,
                        MessageRole.ASSISTANT,
                        MessageType.TEXT,
                        chatbotResponse.answer(),
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

    private List<List<String>> createChatHistory(Long sessionId) {
        List<ChatMessage> messages = chatMessageRepository.findAllByChatSession_IdOrderBySequenceNoAsc(sessionId);
        int fromIndex = Math.max(0, messages.size() - CHAT_HISTORY_LIMIT);

        return messages.subList(fromIndex, messages.size()).stream()
                .map(message -> List.of(toPythonHistoryRole(message.getMessageRole()), message.getContent()))
                .toList();
    }

    private String toPythonHistoryRole(MessageRole messageRole) {
        if (messageRole == MessageRole.USER) {
            return "human";
        }
        return "ai";
    }

    private String resolveCurrentUserType(Long userId) {
        InvestmentProfileResultResponse result = investmentProfileService.getCurrentResult(userId);
        if (result.testStatus() == InvestmentProfileTestStatus.COMPLETED && result.profileType() != null) {
            return result.profileType().name();
        }
        return DEFAULT_USER_TYPE;
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
