package com.fashion.chatbotservice.service;

import com.fashion.chatbotservice.agent.ToolResultCollector;
import com.fashion.chatbotservice.dto.ChatResponse;
import com.fashion.chatbotservice.model.ChatSession;

public interface ProductQueryHandler {

    boolean shouldHandleDirectSearch(String message, ChatSession.PreferenceProfile profile);

    ChatResponse handleExplicitLookup(String sessionId,
                                      String message,
                                      ChatSession session,
                                      ToolResultCollector collector);

    ChatResponse searchWithContext(String sessionId,
                                   String message,
                                   ChatSession session,
                                   ToolResultCollector collector);

    ChatResponse refreshForGenderContext(String sessionId,
                                         String searchKeyword,
                                         ChatSession session);
}
