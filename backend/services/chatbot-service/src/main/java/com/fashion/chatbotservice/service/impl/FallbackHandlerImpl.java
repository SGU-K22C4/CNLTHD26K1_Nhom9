package com.fashion.chatbotservice.service.impl;

import com.fashion.chatbotservice.agent.FashionTools;
import com.fashion.chatbotservice.agent.ResponseAssembler;
import com.fashion.chatbotservice.agent.ToolResultCollector;
import com.fashion.chatbotservice.dto.ChatResponse;
import com.fashion.chatbotservice.model.ChatSession;
import com.fashion.chatbotservice.service.FallbackHandler;
import com.fashion.chatbotservice.service.IntentClassifierService;
import com.fashion.chatbotservice.service.ProductQueryHandler;
import com.fashion.chatbotservice.service.SizeAdvisorService;
import com.fashion.chatbotservice.service.SizeFitAdvisoryService;
import com.fashion.chatbotservice.util.VietnameseNormalizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Owns heuristic fallback behavior so ChatbotServiceImpl can stay focused on
 * orchestration instead of intent-specific fallback branching.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FallbackHandlerImpl implements FallbackHandler {

    private final IntentClassifierService intentClassifierService;
    private final FashionTools fashionTools;
    private final SizeAdvisorService sizeAdvisorService;
    private final SizeFitAdvisoryService sizeFitAdvisoryService;
    private final ProductQueryHandler productQueryHandler;

    @Override
    public ChatResponse handle(String sessionId,
                               String message,
                               ChatSession session,
                               ToolResultCollector collector) {
        IntentClassifierService.IntentScore intentScore = intentClassifierService.classify(message);
        ChatSession.PreferenceProfile profile = session.getPreferenceProfile();

        String reply = switch (intentScore.intent()) {
            case IntentClassifierService.CONSULT_SIZE -> handleSizeConsultation(message, session, collector);
            case IntentClassifierService.ASK_POLICY -> executeKnowledgeFallback(message, profile, session.getUserId(), collector);
            case IntentClassifierService.ASK_PROMOTION -> executePromotionFallback(profile, session.getUserId(), collector);
            case IntentClassifierService.SEARCH_PRODUCT -> {
                ChatResponse response = productQueryHandler.searchWithContext(sessionId, message, session, collector);
                yield response.getReply();
            }
            case IntentClassifierService.CONSULT_SEASON -> executeSeasonFallback(message, profile, session.getUserId(), collector);
            case IntentClassifierService.CHECK_ORDER -> handleOrderFallback(message, session.getUserId(), profile, collector);
            case IntentClassifierService.GREETING -> greetingReply(message);
            default -> handleGeneralFallback(sessionId, message, session, collector);
        };

        return ChatResponse.builder()
                .sessionId(sessionId)
                .intent(intentScore.intent())
                .confidence(intentScore.confidence())
                .reply(reply)
                .missingFields(collector.getMissingFields())
                .suggestions(collector.getProducts())
                .promotions(collector.getPromotions())
                .profile(profile)
                .createdAt(Instant.now())
                .build();
    }

    private String handleSizeConsultation(String message,
                                          ChatSession session,
                                          ToolResultCollector collector) {
        ChatSession.PreferenceProfile profile = session.getPreferenceProfile();
        SizeAdvisorService.Measurements extracted = sizeAdvisorService.extractMeasurements(message);

        Integer heightCm = extracted.heightCm() != null ? extracted.heightCm() : profile.getLastHeightCm();
        Integer weightKg = extracted.weightKg() != null ? extracted.weightKg() : profile.getLastWeightKg();
        Integer chestCm = extracted.chestCm() != null ? extracted.chestCm() : profile.getLastChestCm();
        Integer waistCm = extracted.waistCm() != null ? extracted.waistCm() : profile.getLastWaistCm();
        Integer hipCm = extracted.hipCm() != null ? extracted.hipCm() : profile.getLastHipCm();

        SizeAdvisorService.Measurements merged = new SizeAdvisorService.Measurements(
                heightCm, weightKg, chestCm, waistCm, hipCm);
        if (!merged.hasMinimumData()) {
            collector.addMissingFields(merged.missingFields());
            return "Để tư vấn size chính xác, bạn cung cấp thêm: " + String.join(", ", merged.missingFields());
        }

        profile.setLastHeightCm(heightCm);
        profile.setLastWeightKg(weightKg);
        profile.setLastChestCm(chestCm);
        profile.setLastWaistCm(waistCm);
        profile.setLastHipCm(hipCm);

        String garmentKeyword = extractGarmentKeyword(message);
        if ((garmentKeyword == null || garmentKeyword.isBlank())
                && profile.getLastProductCategoryQueried() != null
                && !profile.getLastProductCategoryQueried().isBlank()) {
            garmentKeyword = profile.getLastProductCategoryQueried();
        }

        SizeAdvisorService.GarmentType type = sizeAdvisorService.detectGarmentType(message);
        SizeFitAdvisoryService.SizeFitAdvice advice =
                sizeFitAdvisoryService.advise(merged, type, garmentKeyword == null ? message : garmentKeyword, profile);
        collector.setSizeRecommendation(advice.recommendedSize());

        if (garmentKeyword != null && !garmentKeyword.isBlank()) {
            try {
                fashionTools.setCollector(collector);
                fashionTools.setPreferenceProfile(profile);
                fashionTools.setCurrentUserId(session.getUserId());
                fashionTools.searchProducts(garmentKeyword, null, null, null, null);
            } catch (Exception ex) {
                log.debug("Auto product search after size advice skipped: {}", ex.getMessage());
            } finally {
                fashionTools.clearCollector();
            }
        }

        StringBuilder sizeReply = new StringBuilder()
                .append("Với chiều cao ").append(heightCm).append("cm và cân nặng ").append(weightKg)
                .append("kg, mình gợi ý bạn chọn **size ").append(advice.recommendedSize()).append("**. ")
                .append(advice.rationale());
        if (advice.followUpPrompt() != null && !advice.followUpPrompt().isBlank()) {
            sizeReply.append("\n\n").append(advice.followUpPrompt());
        }
        if (!collector.getProducts().isEmpty()) {
            sizeReply.append("\n\nMình cũng tìm thấy một số sản phẩm phù hợp cho bạn:");
        }
        return sizeReply.toString();
    }

    private String executeKnowledgeFallback(String message,
                                            ChatSession.PreferenceProfile profile,
                                            String userId,
                                            ToolResultCollector collector) {
        try {
            fashionTools.setCollector(collector);
            fashionTools.setPreferenceProfile(profile);
            fashionTools.setCurrentUserId(userId);
            return fashionTools.searchKnowledge(message);
        } catch (Exception ex) {
            log.warn("Heuristic knowledge search failed: {}", ex.getMessage());
            return "Mình chưa tìm thấy thông tin về chính sách này. Bạn có thể liên hệ CSKH qua hotline hoặc email để được hỗ trợ trực tiếp nhé!";
        } finally {
            fashionTools.clearCollector();
        }
    }

    private String executePromotionFallback(ChatSession.PreferenceProfile profile,
                                            String userId,
                                            ToolResultCollector collector) {
        try {
            fashionTools.setCollector(collector);
            fashionTools.setPreferenceProfile(profile);
            fashionTools.setCurrentUserId(userId);
            return fashionTools.getActivePromotions();
        } catch (Exception ex) {
            log.warn("Heuristic promotion lookup failed: {}", ex.getMessage());
            return "Mình chưa thể kiểm tra khuyến mãi lúc này. Bạn thử lại sau nhé!";
        } finally {
            fashionTools.clearCollector();
        }
    }

    private String executeSeasonFallback(String message,
                                         ChatSession.PreferenceProfile profile,
                                         String userId,
                                         ToolResultCollector collector) {
        try {
            fashionTools.setCollector(collector);
            fashionTools.setPreferenceProfile(profile);
            fashionTools.setCurrentUserId(userId);
            if (shouldUseSalesGuidance(message)) {
                fashionTools.searchSalesGuidance(message);
            }
            return fashionTools.suggestOutfit(extractOccasion(message), extractStyle(message));
        } catch (Exception ex) {
            return "Mình sẵn sàng gợi ý outfit! Bạn cho mình biết dịp cụ thể (đi làm, đi tiệc, du lịch...) nhé!";
        } finally {
            fashionTools.clearCollector();
        }
    }

    private String handleOrderFallback(String message,
                                       String userId,
                                       ChatSession.PreferenceProfile profile,
                                       ToolResultCollector collector) {
        if (isGuestUser(userId)) {
            return "Mình cần bạn đăng nhập để kiểm tra đơn hàng chính xác theo tài khoản nhé.";
        }
        String orderNumber = extractOrderNumber(message);
        if (orderNumber == null) {
            return "Dạ, bạn vui lòng cung cấp mã đơn hàng để mình kiểm tra nhé! 📦\n"
                    + "VD: 'Kiểm tra đơn ORD-1713200000000'\n"
                    + "Bạn có thể tìm mã đơn trong email xác nhận hoặc trang Đơn hàng của tôi.";
        }
        try {
            fashionTools.setCollector(collector);
            fashionTools.setPreferenceProfile(profile);
            fashionTools.setCurrentUserId(userId);
            return fashionTools.checkOrderByNumber(orderNumber);
        } catch (Exception ex) {
            log.warn("Order check failed: {}", ex.getMessage());
            return "Mình chưa thể kiểm tra đơn hàng lúc này. Bạn thử lại sau nhé!";
        } finally {
            fashionTools.clearCollector();
        }
    }

    private String handleGeneralFallback(String sessionId,
                                         String message,
                                         ChatSession session,
                                         ToolResultCollector collector) {
        ChatSession.PreferenceProfile profile = session.getPreferenceProfile();
        if (shouldUseSalesGuidance(message)) {
            try {
                fashionTools.setCollector(collector);
                fashionTools.setPreferenceProfile(profile);
                fashionTools.setCurrentUserId(session.getUserId());
                String salesReply = fashionTools.searchSalesGuidance(message);
                if (salesReply != null && !salesReply.isBlank()) {
                    return salesReply;
                }
            } catch (Exception ex) {
                log.debug("Sales guidance fallback skipped: {}", ex.getMessage());
            } finally {
                fashionTools.clearCollector();
            }
        }

        String keyword = extractGarmentKeyword(message);
        if (keyword != null && keyword.length() >= 2) {
            ChatResponse searchResponse = productQueryHandler.searchWithContext(sessionId, message, session, collector);
            if (searchResponse.getSuggestions() != null && !searchResponse.getSuggestions().isEmpty()) {
                return searchResponse.getReply();
            }
        }

        return "Mình là trợ lý thời trang AI, có thể giúp bạn:\n"
                + "👕 Tìm sản phẩm (VD: 'Tìm áo sơ mi nam')\n"
                + "📏 Tư vấn size (VD: 'Mình cao 1m70, nặng 65kg mặc size gì?')\n"
                + "🎁 Kiểm tra khuyến mãi (VD: 'Có voucher nào không?')\n"
                + "📦 Theo dõi đơn hàng (VD: 'Kiểm tra đơn ORD-xxx')\n"
                + "👗 Gợi ý outfit (VD: 'Gợi ý đồ đi tiệc')";
    }

    private String greetingReply(String message) {
        String lower = VietnameseNormalizer.normalize(message == null ? "" : message);
        if (lower.contains("cam on") || lower.contains("thank")) {
            return "Không có gì ạ! Nếu cần gì thêm, cứ hỏi mình nhé! 😊";
        }
        if (lower.contains("tam biet") || lower.contains("bye")) {
            return "Hẹn gặp lại bạn nhé! Chúc bạn mua sắm vui vẻ! 👋";
        }
        return "Xin chào! 👋 Mình là trợ lý thời trang AI của Fashion Store.\n"
                + "Mình có thể giúp bạn tìm sản phẩm, tư vấn size, kiểm tra khuyến mãi và gợi ý outfit phù hợp.";
    }

    private boolean shouldUseSalesGuidance(String message) {
        String normalized = VietnameseNormalizer.normalize(message == null ? "" : message);
        return normalized.contains("de mac")
                || normalized.contains("an toan")
                || normalized.contains("de phoi")
                || normalized.contains("phan van")
                || normalized.contains("gia hoi cao")
                || normalized.contains("dat qua")
                || normalized.contains("khong biet chon mau nao");
    }

    private String extractOccasion(String message) {
        String normalized = VietnameseNormalizer.normalize(message == null ? "" : message);
        if (normalized.contains("di lam")) return "đi làm";
        if (normalized.contains("di tiec")) return "đi tiệc";
        if (normalized.contains("du lich")) return "du lịch";
        if (normalized.contains("cuoi tuan")) return "cuối tuần";
        return "hằng ngày";
    }

    private String extractStyle(String message) {
        String normalized = VietnameseNormalizer.normalize(message == null ? "" : message);
        if (normalized.contains("toi gian") || normalized.contains("minimal")) return "minimal";
        if (normalized.contains("lich su")) return "smart-casual";
        if (normalized.contains("ca tinh")) return "trend-forward";
        return "basic";
    }

    private String extractOrderNumber(String message) {
        Matcher matcher = Pattern.compile("\\bORD-[A-Za-z0-9-]+\\b", Pattern.CASE_INSENSITIVE).matcher(message == null ? "" : message);
        return matcher.find() ? matcher.group() : null;
    }

    private String extractGarmentKeyword(String message) {
        String normalized = VietnameseNormalizer.normalize(message == null ? "" : message);
        for (String garment : List.of(
                "ao so mi", "ao thun", "ao polo", "ao hoodie", "ao len", "ao khoac",
                "quan jean", "quan tay", "quan short", "quan chino", "chan vay", "vay", "dam", "blazer")) {
            if (normalized.contains(garment)) {
                return garment;
            }
        }
        return "";
    }

    private boolean isGuestUser(String userId) {
        return userId == null || userId.isBlank() || userId.startsWith("guest-");
    }
}
