package com.aipo.backend.domain.chatbot.service;

import com.aipo.backend.domain.chatbot.client.PythonChatbotClient;
import com.aipo.backend.domain.chatbot.dto.ChatbotRequest;
import com.aipo.backend.domain.chatbot.dto.ChatbotResponse;
import com.aipo.backend.domain.chatbot.dto.PythonChatRequest;
import com.aipo.backend.domain.chatbot.dto.PythonChatResponse;
import com.aipo.backend.domain.investmentprofile.dto.InvestmentProfileResultResponse;
import com.aipo.backend.domain.investmentprofile.entity.InvestmentProfileTestStatus;
import com.aipo.backend.domain.investmentprofile.service.InvestmentProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatbotService {

    private static final String DEFAULT_USER_TYPE = "NEUTRAL";

    private final PythonChatbotClient pythonChatbotClient;
    private final InvestmentProfileService investmentProfileService;

    public ChatbotResponse chat(Long userId, ChatbotRequest request) {
        PythonChatResponse response = pythonChatbotClient.chat(new PythonChatRequest(
                String.valueOf(userId),
                request.message().trim(),
                resolveUserType(userId, request.userType()),
                List.of()
        ));

        return new ChatbotResponse(response.answer());
    }

    private String resolveUserType(Long userId, String userType) {
        InvestmentProfileResultResponse result = investmentProfileService.getCurrentResult(userId);
        if (result.testStatus() == InvestmentProfileTestStatus.COMPLETED && result.profileType() != null) {
            return result.profileType().name();
        }

        if (userType == null || userType.isBlank()) {
            return DEFAULT_USER_TYPE;
        }

        return userType.trim();
    }
}
