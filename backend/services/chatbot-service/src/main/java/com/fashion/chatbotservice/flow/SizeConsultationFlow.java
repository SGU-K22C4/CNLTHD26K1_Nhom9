package com.fashion.chatbotservice.flow;

import com.fashion.chatbotservice.agent.ToolResultCollector;
import com.fashion.chatbotservice.dto.ChatResponse;
import com.fashion.chatbotservice.model.ChatSession;
import com.fashion.chatbotservice.service.IntentClassifierService;
import com.fashion.chatbotservice.service.SizeAdvisorService;
import com.fashion.chatbotservice.service.SizeFitAdvisoryService;
import com.fashion.chatbotservice.agent.FashionTools;
import com.fashion.chatbotservice.util.VietnameseNormalizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

/**
 * Flow tư vấn size (Size Consultation).
 * Tách ra từ ChatbotServiceImpl (buildSizeConsultationReply, buildSizeConsultationResponse).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SizeConsultationFlow implements ConversationFlowStrategy {

    private final SizeAdvisorService sizeAdvisorService;
    private final SizeFitAdvisoryService sizeFitAdvisoryService;
    private final FashionTools fashionTools;

    @Override
    public boolean canHandle(String intent, String message, ChatSession session) {
        return IntentClassifierService.CONSULT_SIZE.equals(intent)
                && isStrongDirectSizeIntent(message);
    }

    @Override
    public ChatResponse handle(String sessionId, String message, ChatSession session, ToolResultCollector collector) {
        return ChatResponse.builder()
                .sessionId(sessionId)
                .intent(IntentClassifierService.CONSULT_SIZE)
                .confidence(0.93d)
                .reply(buildSizeReply(message, session, collector))
                .missingFields(collector.getMissingFields())
                .suggestions(collector.getProducts())
                .promotions(collector.getPromotions())
                .profile(session.getPreferenceProfile())
                .createdAt(Instant.now())
                .build();
    }

    private String buildSizeReply(String message, ChatSession session, ToolResultCollector collector) {
        ChatSession.PreferenceProfile profile = session.getPreferenceProfile();
        SizeAdvisorService.Measurements extracted = sizeAdvisorService.extractMeasurements(message);

        Integer heightCm = extracted.heightCm() != null ? extracted.heightCm() : profile.getLastHeightCm();
        Integer weightKg = extracted.weightKg() != null ? extracted.weightKg() : profile.getLastWeightKg();
        Integer chestCm  = extracted.chestCm()  != null ? extracted.chestCm()  : profile.getLastChestCm();
        Integer waistCm  = extracted.waistCm()  != null ? extracted.waistCm()  : profile.getLastWaistCm();
        Integer hipCm    = extracted.hipCm()    != null ? extracted.hipCm()    : profile.getLastHipCm();

        SizeAdvisorService.Measurements merged =
                new SizeAdvisorService.Measurements(heightCm, weightKg, chestCm, waistCm, hipCm);

        if (!merged.hasMinimumData()) {
            collector.addMissingFields(merged.missingFields());
            return "Để tư vấn size chính xác, bạn cung cấp thêm: "
                    + String.join(", ", merged.missingFields());
        }

        // Lưu số đo vào profile cho câu tiếp theo
        profile.setLastHeightCm(heightCm);
        profile.setLastWeightKg(weightKg);
        profile.setLastChestCm(chestCm);
        profile.setLastWaistCm(waistCm);
        profile.setLastHipCm(hipCm);

        String garment = sizeAdvisorService.extractGarmentFromMessage(message);
        if ((garment == null || garment.isBlank()) && profile.getLastProductCategoryQueried() != null) {
            garment = profile.getLastProductCategoryQueried();
        }

        SizeAdvisorService.GarmentType type = sizeAdvisorService.detectGarmentType(message);
        SizeFitAdvisoryService.SizeFitAdvice advice =
                sizeFitAdvisoryService.advise(merged, type, garment == null ? message : garment, profile);
        collector.setSizeRecommendation(advice.recommendedSize());

        // Tự động tìm sản phẩm phù hợp sau khi tư vấn size
        if (garment != null && !garment.isBlank()) {
            try {
                fashionTools.setCollector(collector);
                fashionTools.setPreferenceProfile(profile);
                fashionTools.setCurrentUserId(session.getUserId());
                Double maxPrice = parseBudget(profile.getBudget());
                fashionTools.searchProducts(garment, null, maxPrice != null ? maxPrice.longValue() : null, null, null);
            } catch (Exception ex) {
                log.debug("Auto product search after size advice skipped: {}", ex.getMessage());
            } finally {
                fashionTools.clearCollector();
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Với chiều cao ").append(heightCm).append("cm và cân nặng ").append(weightKg)
          .append("kg, mình gợi ý bạn chọn **size ").append(advice.recommendedSize()).append("**. ")
          .append(advice.rationale());
        if (advice.followUpPrompt() != null && !advice.followUpPrompt().isBlank()) {
            sb.append("\n\n").append(advice.followUpPrompt());
        }
        if (!collector.getProducts().isEmpty()) {
            sb.append("\n\nMình cũng tìm thấy một số sản phẩm phù hợp cho bạn:");
        }
        return sb.toString();
    }

    private boolean isStrongDirectSizeIntent(String message) {
        if (message == null || message.isBlank()) return false;
        String normalized = VietnameseNormalizer.normalize(message);
        boolean hasMeasurement = containsMeasurementData(normalized);
        boolean hasSizeLanguage = normalized.contains("size") || normalized.contains("so do")
                || normalized.contains("nen chon") || normalized.contains("size nao");
        boolean hasGarment = !sizeAdvisorService.extractGarmentFromMessage(message).isBlank();
        return (hasMeasurement || hasSizeLanguage) && hasGarment;
    }

    private boolean containsMeasurementData(String normalized) {
        return normalized.matches(".*\\d+\\s*(cm|kg|met|m).*")
                || normalized.matches(".*\\d{2,3}\\s*kg.*")
                || normalized.matches(".*cao\\s*\\d.*")
                || normalized.matches(".*nang\\s*\\d.*");
    }

    private Double parseBudget(String budget) {
        if (budget == null || budget.isBlank()) return null;
        try {
            String clean = budget.replaceAll("[^\\d]", "");
            return clean.isBlank() ? null : Double.parseDouble(clean);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
