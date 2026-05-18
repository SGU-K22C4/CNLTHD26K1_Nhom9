package com.fashion.chatbotservice.flow;

import com.fashion.chatbotservice.agent.FashionTools;
import com.fashion.chatbotservice.agent.ResponseAssembler;
import com.fashion.chatbotservice.agent.ToolResultCollector;
import com.fashion.chatbotservice.dto.ChatResponse;
import com.fashion.chatbotservice.model.ChatSession;
import com.fashion.chatbotservice.service.IntentClassifierService;
import com.fashion.chatbotservice.service.ProductQueryHandler;
import com.fashion.chatbotservice.response.FashionResponseComposer;
import com.fashion.chatbotservice.conversation.ConversationStateService;
import com.fashion.chatbotservice.conversation.StageDecision;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Flow khám phá sản phẩm (Product Discovery).
 * Xử lý: SEARCH_PRODUCT intent + consultative recommendation.
 * Tách ra từ ChatbotServiceImpl (handleDirectIntent, maybeComposeConsultativeRecommendation).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ProductDiscoveryFlow implements ConversationFlowStrategy {

    private final ProductQueryHandler productQueryHandler;
    private final ConversationStateService conversationStateService;
    private final FashionResponseComposer fashionResponseComposer;

    @Override
    public boolean canHandle(String intent, String message, ChatSession session) {
        return IntentClassifierService.SEARCH_PRODUCT.equals(intent)
                && productQueryHandler.shouldHandleDirectSearch(message, session.getPreferenceProfile());
    }

    @Override
    public ChatResponse handle(String sessionId, String message, ChatSession session, ToolResultCollector collector) {
        ChatResponse response = productQueryHandler.searchWithContext(sessionId, message, session, collector);
        return maybeComposeConsultativeRecommendation(response, session.getPreferenceProfile(), message);
    }

    /**
     * Nếu đủ điều kiện consultative → tổng hợp reply mang phong cách tư vấn.
     */
    private ChatResponse maybeComposeConsultativeRecommendation(ChatResponse response,
                                                                ChatSession.PreferenceProfile profile,
                                                                String message) {
        if (response == null || profile == null
                || response.getSuggestions() == null || response.getSuggestions().isEmpty()) {
            return response;
        }
        if (!isConsultativeRecommendationTurn(message)) {
            return response;
        }
        StageDecision decision = conversationStateService.evaluateStageDecision(profile, message);
        if (!decision.shouldRecommend()) {
            return response;
        }
        List<ChatResponse.ProductSuggestion> limited = response.getSuggestions().stream()
                .limit(3)
                .toList();
        response.setSuggestions(new ArrayList<>(limited));
        response.setReply(fashionResponseComposer.composeRecommendationReply(profile, limited));
        return response;
    }

    private boolean isConsultativeRecommendationTurn(String message) {
        if (message == null || message.isBlank()) return false;
        String lower = message.toLowerCase();
        return lower.contains("gợi ý") || lower.contains("tư vấn")
                || lower.contains("mặc gì") || lower.contains("outfit")
                || lower.contains("set đồ") || lower.contains("áo")
                || lower.contains("quần") || lower.contains("váy");
    }
}
