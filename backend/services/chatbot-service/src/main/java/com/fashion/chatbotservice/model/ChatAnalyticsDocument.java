package com.fashion.chatbotservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Structured log entry cho mỗi request chat.
 * Lưu vào collection riêng cho analytics, tách khỏi chat_sessions.
 */
@Document(collection = "chatbot_analytics")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatAnalyticsDocument {

    @Id
    private String id;

    @Indexed
    private String traceId;

    @Indexed
    private String sessionId;

    @Indexed
    private String userId;

    private Instant timestamp;

    private String userMessage;
    private int messageLength;

    private String intent;
    private double confidence;

    private List<String> toolsCalled;
    private Map<String, Long> toolDurations;

    private List<String> knowledgeSources;
    private double topKnowledgeScore;

    private String llmModel;
    private int inputTokens;
    private int outputTokens;
    private long llmLatencyMs;

    private int replyLength;
    private int suggestionsCount;
    private int promotionsCount;
    private boolean hasCitation;

    private long totalLatencyMs;
}
