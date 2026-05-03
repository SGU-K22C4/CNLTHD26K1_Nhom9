package com.fashion.chatbotservice.agent;

import com.fashion.chatbotservice.dto.ChatResponse;
import com.fashion.chatbotservice.model.ChatSession;

import java.time.Instant;

/**
 * Ghép LLM reply (text) + tool results (structured) thành ChatResponse DTO.
 * Đảm bảo contract frontend không thay đổi.
 */
public class ResponseAssembler {

    private ResponseAssembler() {}

    public static ChatResponse build(
            String sessionId,
            String llmReply,
            ToolResultCollector collector,
            ChatSession.PreferenceProfile profile) {

        return ChatResponse.builder()
                .sessionId(sessionId)
                .intent(deriveIntent(collector))
                .confidence(deriveConfidence(collector))
                .reply(llmReply != null ? llmReply : "Mình chưa thể xử lý yêu cầu này, bạn thử hỏi lại nhé!")
                .suggestions(collector.getProducts())
                .promotions(collector.getPromotions())
                .missingFields(collector.getMissingFields())
                .profile(profile)
                .createdAt(Instant.now())
                .build();
    }

    private static String deriveIntent(ToolResultCollector collector) {
        if (collector.getSizeRecommendation() != null) return "CONSULT_SIZE";
        if (!collector.getPromotions().isEmpty()) return "ASK_PROMOTION";
        if (!collector.getKnowledgeSources().isEmpty()) return "KNOWLEDGE";
        if (!collector.getProducts().isEmpty()) return "SEARCH_PRODUCT";
        return "GENERAL";
    }

    private static double deriveConfidence(ToolResultCollector collector) {
        if (!collector.getKnowledgeSources().isEmpty()) {
            return collector.getKnowledgeSources().stream()
                    .mapToDouble(ToolResultCollector.KnowledgeSource::score)
                    .max().orElse(0.5);
        }
        if (!collector.getProducts().isEmpty() || !collector.getPromotions().isEmpty()
                || collector.getSizeRecommendation() != null) {
            return 0.9;
        }
        return 0.5;
    }
}
