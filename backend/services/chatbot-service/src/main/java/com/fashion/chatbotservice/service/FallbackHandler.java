package com.fashion.chatbotservice.service;

import com.fashion.chatbotservice.agent.ToolResultCollector;
import com.fashion.chatbotservice.dto.ChatResponse;
import com.fashion.chatbotservice.model.ChatSession;

public interface FallbackHandler {

    ChatResponse handle(String sessionId,
                        String message,
                        ChatSession session,
                        ToolResultCollector collector);
}
