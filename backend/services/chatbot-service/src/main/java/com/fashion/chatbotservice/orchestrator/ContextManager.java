package com.fashion.chatbotservice.orchestrator;

import com.fashion.chatbotservice.agent.ToolResultCollector;
import com.fashion.chatbotservice.dto.ChatResponse;
import com.fashion.chatbotservice.model.ChatSession;
import com.fashion.chatbotservice.service.ProfileEnrichmentService;
import com.fashion.chatbotservice.conversation.ConversationStateService;
import com.fashion.chatbotservice.util.VietnameseNormalizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

/**
 * Quản lý context & profile enrichment từ message và các nguồn bên ngoài.
 * Tách ra từ ChatbotServiceImpl — phần enrich profile.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ContextManager {

    private final ProfileEnrichmentService profileEnrichmentService;
    private final ConversationStateService conversationStateService;

    /**
     * Làm giàu PreferenceProfile từ message hiện tại + lịch sử user.
     */
    public void enrichContext(ChatSession session, String message, String userId) {
        ChatSession.PreferenceProfile profile = session.getPreferenceProfile();
        profileEnrichmentService.enrichFromMessage(profile, message);
        profileEnrichmentService.enrichFromPurchaseHistory(profile, userId);
        profileEnrichmentService.enrichFromWishlist(profile, userId);
        profileEnrichmentService.enrichFromUserProfile(profile, userId);
        conversationStateService.refreshState(profile, message);
        updateBudgetFromMessage(profile, message);
        refreshProductQueryContext(profile, message);
        log.debug("Context enriched for session: {}, userId: {}", session.getSessionId(), userId);
    }

    /**
     * Cập nhật ngân sách từ message nếu user đề cập đến.
     */
    private void updateBudgetFromMessage(ChatSession.PreferenceProfile profile, String message) {
        if (profile == null || message == null) return;
        String normalized = VietnameseNormalizer.normalize(message);
        // Regex: "dưới 500k", "khoảng 1 triệu", "từ 300k"
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(
                "(duoi|khoang|tu|tren|gia|budget)[\\s\\w]*?(\\d+)\\s*(k|tr|trieu|nghin|000)?",
                java.util.regex.Pattern.CASE_INSENSITIVE);
        java.util.regex.Matcher m = p.matcher(normalized);
        if (m.find()) {
            try {
                String numStr = m.group(2);
                String unit   = m.group(3);
                long amount   = Long.parseLong(numStr);
                if (unit != null && (unit.startsWith("tr") || unit.startsWith("tr"))) {
                    amount *= 1_000_000L;
                } else if (unit != null && unit.startsWith("k")) {
                    amount *= 1_000L;
                } else if (numStr.length() <= 3) {
                    amount *= 1_000L; // "500" → 500k
                }
                profile.setBudget(String.valueOf(amount));
            } catch (NumberFormatException ignored) {
            }
        }
    }

    /**
     * Cập nhật category được query gần nhất để dùng cho các turn tiếp theo.
     */
    private void refreshProductQueryContext(ChatSession.PreferenceProfile profile, String message) {
        if (profile == null || message == null) return;
        String normalized = VietnameseNormalizer.normalize(message).toLowerCase();
        List<String> categories = List.of("ao thun", "ao so mi", "ao khoac", "ao hoodie",
                "quan jean", "quan tay", "quan short", "vay", "dam");
        for (String cat : categories) {
            if (normalized.contains(cat)) {
                profile.setLastProductCategoryQueried(cat);
                profile.setLastProductQueryTime(Instant.now());
                break;
            }
        }
    }
}
