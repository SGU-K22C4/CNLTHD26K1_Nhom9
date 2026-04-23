package com.fashion.chatbotservice.service;

import com.fashion.chatbotservice.agent.ToolResultCollector;
import com.fashion.chatbotservice.dto.ChatResponse;

/**
 * Lưu structured log entry cho mỗi request chat để analytics.
 */
public interface ChatAnalyticsService {

    /**
     * Ghi nhận analytics cho một request chat.
     */
    void record(String traceId, String sessionId, String userId,
                String userMessage, ChatResponse response,
                ToolResultCollector collector, long totalLatencyMs);
}
