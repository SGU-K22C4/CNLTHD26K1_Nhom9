package com.fashion.chatbotservice.quality;

import com.fashion.chatbotservice.agent.ToolResultCollector;
import com.fashion.chatbotservice.dto.ChatResponse;

/**
 * Chấm điểm chất lượng response của chatbot — Phase 2C.
 */
public interface ResponseQualityScorer {

    /**
     * Chấm điểm response dựa trên grounding, relevance, safety và sales technique.
     *
     * @param response  response từ chatbot
     * @param collector kết quả tool calls (để kiểm tra grounding)
     * @param userMessage tin nhắn gốc của user
     * @return QualityScore (0-100)
     */
    QualityScore score(ChatResponse response, ToolResultCollector collector, String userMessage);
}
