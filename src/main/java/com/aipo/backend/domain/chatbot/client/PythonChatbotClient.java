package com.aipo.backend.domain.chatbot.client;

import com.aipo.backend.domain.chatbot.config.ChatbotProperties;
import com.aipo.backend.domain.chatbot.dto.PythonChatRequest;
import com.aipo.backend.domain.chatbot.dto.PythonChatResponse;
import com.aipo.backend.global.exception.CustomException;
import com.aipo.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.SocketTimeoutException;
import java.time.Duration;

@Component
@RequiredArgsConstructor
public class PythonChatbotClient {

    private final ChatbotProperties properties;

    public PythonChatResponse chat(PythonChatRequest request) {
        try {
            PythonChatResponse response = restClient()
                    .post()
                    .uri("/chat")
                    .body(request)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (httpRequest, httpResponse) -> {
                        throw new CustomException(ErrorCode.CHATBOT_CALL_FAILED);
                    })
                    .body(PythonChatResponse.class);

            if (response == null || response.answer() == null || response.answer().isBlank()) {
                throw new CustomException(ErrorCode.CHATBOT_EMPTY_ANSWER);
            }

            return response;
        } catch (CustomException exception) {
            throw exception;
        } catch (ResourceAccessException exception) {
            if (isTimeout(exception)) {
                throw new CustomException(ErrorCode.CHATBOT_TIMEOUT);
            }
            throw new CustomException(ErrorCode.CHATBOT_SERVER_UNAVAILABLE);
        } catch (RestClientException exception) {
            throw new CustomException(ErrorCode.CHATBOT_CALL_FAILED);
        }
    }

    private RestClient restClient() {
        int timeoutSeconds = properties.resolvedTimeoutSeconds();
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(timeoutSeconds));
        requestFactory.setReadTimeout(Duration.ofSeconds(timeoutSeconds));

        return RestClient.builder()
                .baseUrl(properties.resolvedBaseUrl())
                .requestFactory(requestFactory)
                .build();
    }

    private boolean isTimeout(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof SocketTimeoutException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
