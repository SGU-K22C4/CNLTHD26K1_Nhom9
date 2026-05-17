package com.fashion.chatbotservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Event feedback tu FE sau khi chatbot da goi y san pham.
 * Tach rieng event-level analytics de do duoc shown -> click -> intent.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatFeedbackEventRequest {

    private String sessionId;
    private String eventType;
    private String sourceMessageId;
    private String productId;
    private String productName;
    private Map<String, Object> metadata;
}
