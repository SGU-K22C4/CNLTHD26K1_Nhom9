package com.fashion.chatbotservice.service;

import com.fashion.chatbotservice.dto.ChatRequest;
import com.fashion.chatbotservice.dto.ChatResponse;
import com.fashion.chatbotservice.dto.SessionResponse;

/**
 * Service chính điều phối chatbot: nhận request → phân tích → trả lời.
 */
public interface ChatbotService {

    /**
     * Xử lý tin nhắn chat từ user.
     */
    ChatResponse chat(ChatRequest request, String userIdHeader, String traceId);

    /**
     * Lấy lịch sử chat session.
     */
    SessionResponse getSession(String sessionId);
}
