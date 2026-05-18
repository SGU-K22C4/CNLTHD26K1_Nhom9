package com.fashion.chatbotservice.query;

import com.fashion.chatbotservice.model.ChatSession;

/**
 * Service phân tích và làm giàu query của user — Phase 2B.
 */
public interface QueryUnderstandingService {

    /**
     * Phân tích message và context session để tạo EnrichedQuery.
     * @param query   raw message từ user
     * @param session session hiện tại (có thể null nếu cold start)
     */
    EnrichedQuery understand(String query, ChatSession session);
}
