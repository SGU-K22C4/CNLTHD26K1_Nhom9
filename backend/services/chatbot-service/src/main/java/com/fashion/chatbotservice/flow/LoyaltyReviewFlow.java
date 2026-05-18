package com.fashion.chatbotservice.flow;

import com.fashion.chatbotservice.agent.FashionTools;
import com.fashion.chatbotservice.agent.ResponseAssembler;
import com.fashion.chatbotservice.agent.ToolResultCollector;
import com.fashion.chatbotservice.dto.ChatResponse;
import com.fashion.chatbotservice.model.ChatSession;
import com.fashion.chatbotservice.service.IntentClassifierService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

/**
 * Flow xử lý Loyalty / Điểm thưởng / Quyền lợi thành viên.
 * Tách ra từ ChatbotServiceImpl (handleDirectIntent).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class LoyaltyReviewFlow implements ConversationFlowStrategy {

    private final FashionTools fashionTools;

    @Override
    public boolean canHandle(String intent, String message, ChatSession session) {
        return IntentClassifierService.LOYALTY_BENEFIT.equals(intent);
    }

    @Override
    public ChatResponse handle(String sessionId, String message, ChatSession session, ToolResultCollector collector) {
        String userId = session.getUserId();
        String intent = IntentClassifierService.LOYALTY_BENEFIT;

        if (isGuestUser(userId)) {
            return ChatResponse.builder()
                    .sessionId(sessionId)
                    .intent(intent)
                    .confidence(0.95d)
                    .reply("Mình cần bạn đăng nhập để kiểm tra điểm thưởng và quyền lợi thành viên chính xác nhé.")
                    .suggestions(List.of())
                    .promotions(List.of())
                    .profile(session.getPreferenceProfile())
                    .createdAt(Instant.now())
                    .build();
        }

        try {
            fashionTools.setCollector(collector);
            fashionTools.setPreferenceProfile(session.getPreferenceProfile());
            fashionTools.setCurrentUserId(userId);
            String reply = fashionTools.getLoyaltyBenefits(userId);
            return ResponseAssembler.build(sessionId, reply, collector, session.getPreferenceProfile());
        } catch (Exception ex) {
            log.warn("LoyaltyReviewFlow failed: {}", ex.getMessage());
            return ChatResponse.builder()
                    .sessionId(sessionId)
                    .intent(intent)
                    .confidence(0.8d)
                    .reply("Mình chưa thể kiểm tra điểm thưởng lúc này. Bạn thử lại sau ít phút nhé.")
                    .suggestions(collector.getProducts())
                    .promotions(collector.getPromotions())
                    .profile(session.getPreferenceProfile())
                    .createdAt(Instant.now())
                    .build();
        } finally {
            fashionTools.clearCollector();
        }
    }

    private boolean isGuestUser(String userId) {
        return userId == null || userId.isBlank()
                || userId.startsWith("guest-") || userId.startsWith("GUEST-");
    }
}
