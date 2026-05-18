package com.fashion.chatbotservice.flow;

import com.fashion.chatbotservice.agent.ToolResultCollector;
import com.fashion.chatbotservice.dto.ChatResponse;
import com.fashion.chatbotservice.model.ChatSession;

/**
 * Strategy Pattern cho các luồng xử lý hội thoại khác nhau.
 * Mỗi impl chịu trách nhiệm một intent/flow cụ thể.
 */
public interface ConversationFlowStrategy {

    /**
     * Kiểm tra xem strategy này có phù hợp với intent và message không.
     */
    boolean canHandle(String intent, String message, ChatSession session);

    /**
     * Xử lý message và trả về ChatResponse, null nếu không xử lý được.
     */
    ChatResponse handle(String sessionId, String message, ChatSession session, ToolResultCollector collector);
}
