package com.aipo.backend.domain.chatbot.service;

import com.aipo.backend.domain.chatbot.client.PythonChatbotClient;
import com.aipo.backend.domain.chatbot.dto.ChatbotRequest;
import com.aipo.backend.domain.chatbot.dto.ChatbotResponse;
import com.aipo.backend.domain.chatbot.dto.PythonChatRequest;
import com.aipo.backend.domain.chatbot.dto.PythonChatResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatbotService {

    private static final String DEFAULT_USER_TYPE = "balance";

    private final PythonChatbotClient pythonChatbotClient;

    public ChatbotResponse chat(Long userId, ChatbotRequest request) {
        PythonChatResponse response = pythonChatbotClient.chat(new PythonChatRequest(
                String.valueOf(userId),
                request.message().trim(),
                resolveUserType(request.userType()),
                List.of()
        ));

        return new ChatbotResponse(response.answer());
    }

    private String resolveUserType(String userType) {
        if (userType == null || userType.isBlank()) {
            return DEFAULT_USER_TYPE;
        }
        return userType.trim();
    }
}
