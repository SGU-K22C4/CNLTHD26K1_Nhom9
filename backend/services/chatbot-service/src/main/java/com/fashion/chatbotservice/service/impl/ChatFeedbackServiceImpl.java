package com.fashion.chatbotservice.service.impl;

import com.fashion.chatbotservice.dto.ChatFeedbackEventRequest;
import com.fashion.chatbotservice.model.ChatAnalyticsDocument;
import com.fashion.chatbotservice.repository.ChatAnalyticsRepository;
import com.fashion.chatbotservice.service.ChatFeedbackService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Luu feedback event tu FE de biet user da tuong tac voi goi y nao.
 * Chua co full attribution den order, nhung day la nen tang cho feedback loop.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChatFeedbackServiceImpl implements ChatFeedbackService {

    private final ChatAnalyticsRepository repository;

    @Override
    public void recordEvent(String userId, ChatFeedbackEventRequest request) {
        if (request == null || isBlank(request.getSessionId()) || isBlank(request.getEventType())) {
            return;
        }

        try {
            Map<String, Object> metadata = request.getMetadata() != null
                    ? new LinkedHashMap<>(request.getMetadata())
                    : new LinkedHashMap<>();

            ChatAnalyticsDocument document = ChatAnalyticsDocument.builder()
                    .sessionId(request.getSessionId())
                    .userId(userId)
                    .timestamp(Instant.now())
                    .eventType(request.getEventType())
                    .sourceMessageId(request.getSourceMessageId())
                    .productId(request.getProductId())
                    .productName(request.getProductName())
                    .metadata(metadata)
                    .build();

            repository.save(document);
        } catch (Exception ex) {
            log.warn("Failed to save chat feedback event: {}", ex.getMessage());
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
