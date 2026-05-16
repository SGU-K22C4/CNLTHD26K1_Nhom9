package com.fashion.chatbotservice.agent;

import com.fashion.chatbotservice.dto.ChatResponse;
import com.fashion.chatbotservice.model.ChatSession;

import java.time.Instant;
import java.util.List;

/**
 * Ghep LLM reply + structured tool results thanh ChatResponse on dinh hon cho FE.
 * Phase 1 uu tien giu response gon, co huong tu van tiep theo va khong dua qua nhieu item.
 */
public class ResponseAssembler {

    private ResponseAssembler() {}

    public static ChatResponse build(
            String sessionId,
            String llmReply,
            ToolResultCollector collector,
            ChatSession.PreferenceProfile profile) {

        String intent = deriveIntent(collector);
        List<ChatResponse.ProductSuggestion> suggestions = collector.getProducts().stream()
                .limit(3)
                .toList();
        List<ChatResponse.PromotionSuggestion> promotions = collector.getPromotions().stream()
                .limit(2)
                .toList();
        String reply = polishReply(
                llmReply != null ? llmReply.trim() : "",
                intent,
                suggestions,
                promotions,
                collector.getMissingFields());

        return ChatResponse.builder()
                .sessionId(sessionId)
                .intent(intent)
                .confidence(deriveConfidence(collector))
                .reply(reply)
                .suggestions(suggestions)
                .promotions(promotions)
                .missingFields(collector.getMissingFields())
                .profile(profile)
                .createdAt(Instant.now())
                .build();
    }

    private static String deriveIntent(ToolResultCollector collector) {
        if (collector.getSizeRecommendation() != null) return "CONSULT_SIZE";
        if (!collector.getPromotions().isEmpty()) return "ASK_PROMOTION";
        if (!collector.getKnowledgeSources().isEmpty()) return "ASK_POLICY";
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

    private static String polishReply(String reply,
                                      String intent,
                                      List<ChatResponse.ProductSuggestion> suggestions,
                                      List<ChatResponse.PromotionSuggestion> promotions,
                                      List<String> missingFields) {
        String normalized = (reply == null || reply.isBlank())
                ? "Mình chưa thể xử lý yêu cầu này, bạn thử hỏi lại nhé!"
                : reply.trim();

        if (!missingFields.isEmpty()) {
            return normalized;
        }

        if ("CONSULT_SIZE".equals(intent) && !suggestions.isEmpty()
                && !containsAny(normalized, "nếu bạn muốn", "neu ban muon", "mình có thể chọn tiếp", "minh co the chon tiep")) {
            return normalized + "\n\nNếu bạn muốn, mình có thể chọn tiếp 2-3 mẫu đang còn size phù hợp cho bạn.";
        }

        if ("SEARCH_PRODUCT".equals(intent) && !suggestions.isEmpty()
                && !containsAny(normalized, "size", "màu", "mau", "ngân sách", "ngan sach", "lọc tiếp", "loc tiep")) {
            return normalized + "\n\nNếu cần, mình có thể lọc tiếp theo size, màu hoặc ngân sách để bạn chọn nhanh hơn.";
        }

        if ("ASK_PROMOTION".equals(intent) && !promotions.isEmpty()
                && !containsAny(normalized, "giỏ hàng", "gio hang", "đơn", "don")) {
            return normalized + "\n\nNếu bạn đang nhắm mẫu cụ thể, mình có thể kiểm tra xem ưu đãi nào hợp nhất cho đơn hàng.";
        }

        return normalized;
    }

    private static boolean containsAny(String text, String... patterns) {
        String lower = text.toLowerCase();
        for (String pattern : patterns) {
            if (lower.contains(pattern.toLowerCase())) {
                return true;
            }
        }
        return false;
    }
}
