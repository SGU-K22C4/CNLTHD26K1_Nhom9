package com.fashion.chatbotservice.conversation.impl;

import com.fashion.chatbotservice.conversation.ClarifyingQuestion;
import com.fashion.chatbotservice.conversation.ConversationStateService;
import com.fashion.chatbotservice.conversation.RecommendationReadiness;
import com.fashion.chatbotservice.conversation.SalesStage;
import com.fashion.chatbotservice.conversation.SlotFillingService;
import com.fashion.chatbotservice.conversation.StageDecision;
import com.fashion.chatbotservice.model.ChatSession;
import com.fashion.chatbotservice.util.VietnameseNormalizer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ConversationStateServiceImpl implements ConversationStateService {

    private final SlotFillingService slotFillingService;

    @Override
    public void refreshState(ChatSession.PreferenceProfile profile, String message) {
        if (profile == null) {
            return;
        }

        slotFillingService.mergeSlots(profile, message);
        profile.setConversationStateUpdatedAt(Instant.now());

        StageDecision decision = evaluateStageDecision(profile, message);
        SalesStage nextStage = decision.stage();
        if (profile.getSalesStage() != nextStage) {
            profile.setSalesStage(nextStage);
            profile.setStageEntryAt(Instant.now());
        }

        profile.setSlotConfidence(decision.readiness().filledCoreSlots() / 3.0d);
    }

    @Override
    public StageDecision evaluateStageDecision(ChatSession.PreferenceProfile profile, String message) {
        RecommendationReadiness readiness = slotFillingService.evaluateReadiness(profile);
        SalesStage stage = determineStage(profile, message, readiness);
        ClarifyingQuestion question = buildClarifyingQuestion(profile, message, readiness, stage);
        return StageDecision.builder()
                .stage(stage)
                .readiness(readiness)
                .clarifyingQuestion(question)
                .shouldAskClarifyingQuestion(question != null)
                .shouldRecommend(readiness.ready() && stage == SalesStage.RECOMMENDING)
                .build();
    }

    @Override
    public boolean shouldAskClarifyingQuestion(ChatSession.PreferenceProfile profile, String message) {
        return evaluateStageDecision(profile, message).shouldAskClarifyingQuestion();
    }

    @Override
    public String pickNextQuestionSlot(ChatSession.PreferenceProfile profile) {
        RecommendationReadiness readiness = slotFillingService.evaluateReadiness(profile);
        List<String> missingSlots = readiness.missingPrioritySlots();
        if (missingSlots.isEmpty()) {
            return null;
        }
        return pickUnaskedSlot(profile, missingSlots);
    }

    private SalesStage determineStage(ChatSession.PreferenceProfile profile,
                                      String message,
                                      RecommendationReadiness readiness) {
        String normalized = VietnameseNormalizer.normalize(message == null ? "" : message);
        if (normalized.contains("so sanh") || normalized.contains("nen chon") || normalized.contains("phan van")) {
            return SalesStage.COMPARING;
        }
        if (normalized.contains("mua luon") || normalized.contains("chot") || normalized.contains("chon mau nao")) {
            return SalesStage.CLOSING;
        }

        if (readiness.ready()) {
            return SalesStage.RECOMMENDING;
        }
        if (readiness.filledCoreSlots() >= 1) {
            return SalesStage.FILTERING;
        }
        return SalesStage.DISCOVERY;
    }

    private ClarifyingQuestion buildClarifyingQuestion(ChatSession.PreferenceProfile profile,
                                                       String message,
                                                       RecommendationReadiness readiness,
                                                       SalesStage stage) {
        if (profile == null || message == null || message.isBlank()) {
            return null;
        }
        String normalized = VietnameseNormalizer.normalize(message);
        if (!isConsultativeTurn(normalized) || hasHardCommerceSignal(normalized) || readiness.ready()
                || profile.getSelectedProductContext() != null) {
            return null;
        }

        if (shouldAskMeasurements(profile, normalized)) {
            return ClarifyingQuestion.builder()
                    .slotName("measurements")
                    .question("Äá»ƒ mÃ¬nh chá»n form tÃ´n dÃ¡ng hÆ¡n, báº¡n cho mÃ¬nh chiá»u cao vÃ  cÃ¢n náº·ng trÆ°á»›c nhÃ©?")
                    .reason("Body-shape context giÃºp tÆ° váº¥n fit vÃ  giáº£i thÃ­ch vÃ¬ sao máº«u há»£p hÆ¡n.")
                    .priority(1)
                    .build();
        }

        List<String> missingSlots = readiness.missingPrioritySlots();
        if (missingSlots.isEmpty()) {
            return null;
        }

        String nextSlot = pickUnaskedSlot(profile, missingSlots);
        return switch (nextSlot) {
            case "occasion" -> ClarifyingQuestion.builder()
                    .slotName("occasion")
                    .question("Bạn đang cần mặc cho dịp nào là chính: đi làm, đi chơi, đi date hay hằng ngày?")
                    .reason("Occasion quyết định hướng outfit trước khi recommend.")
                    .priority(stage == SalesStage.DISCOVERY ? 1 : 2)
                    .build();
            case "productType" -> ClarifyingQuestion.builder()
                    .slotName("productType")
                    .question("Bạn đang nghiêng về áo, quần, váy hay set đồ để mình lọc đúng nhóm hơn?")
                    .reason("Product type giúp tránh recommend lan man.")
                    .priority(1)
                    .build();
            case "styleVibe" -> ClarifyingQuestion.builder()
                    .slotName("styleVibe")
                    .question("Bạn thích vibe nào hơn: tối giản, trẻ trung, lịch sự hay nổi bật?")
                    .reason("Style vibe giúp giải thích vì sao mẫu hợp hơn.")
                    .priority(3)
                    .build();
            default -> null;
        };
    }

    private String pickUnaskedSlot(ChatSession.PreferenceProfile profile, List<String> missingSlots) {
        for (String slot : missingSlots) {
            if (profile == null || profile.getAskedSlots() == null || !profile.getAskedSlots().contains(slot)) {
                return slot;
            }
        }
        return missingSlots.get(0);
    }

    private boolean isConsultativeTurn(String normalized) {
        return normalized.contains("goi y")
                || normalized.contains("tu van")
                || normalized.contains("mac gi")
                || normalized.contains("outfit")
                || normalized.contains("set do")
                || normalized.contains("chon gi");
    }

    private boolean hasHardCommerceSignal(String normalized) {
        return normalized.contains("review")
                || normalized.contains("khuyen mai")
                || normalized.contains("don hang")
                || normalized.contains("wishlist")
                || normalized.contains("diem thuong");
    }

    private boolean shouldAskMeasurements(ChatSession.PreferenceProfile profile, String normalized) {
        if (profile == null || profile.getStylingSlots() == null) {
            return false;
        }
        boolean hasBodyContext = profile.getStylingSlots().getHeightCm() != null
                && profile.getStylingSlots().getWeightKg() != null;
        if (hasBodyContext) {
            return false;
        }
        return normalized.contains("size")
                || normalized.contains("fit")
                || normalized.contains("form")
                || normalized.contains("ton dang")
                || normalized.contains("dang nguoi");
    }
}
