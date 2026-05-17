package com.fashion.chatbotservice.response;

import com.fashion.chatbotservice.conversation.ClarifyingQuestion;
import com.fashion.chatbotservice.conversation.SalesStage;
import com.fashion.chatbotservice.model.ChatSession;
import com.fashion.chatbotservice.dto.ChatResponse;
import com.fashion.chatbotservice.sales.ClosingEngine;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class FashionResponseComposer {

    private final ClosingEngine closingEngine;

    public String buildClarifyingQuestion(ChatSession.PreferenceProfile profile, String nextSlot) {
        if (nextSlot == null || nextSlot.isBlank()) {
            return "Mình có thể gợi ý nhanh cho bạn, nhưng cho mình thêm 1 chi tiết ngắn để chọn đúng hơn nhé?";
        }
        return switch (nextSlot) {
            case "occasion" -> "Bạn đang cần mặc cho dịp nào là chính: đi làm, đi chơi, đi date hay hằng ngày?";
            case "productType" -> "Bạn đang nghiêng về áo, quần, váy hay set đồ để mình lọc đúng nhóm hơn?";
            case "styleVibe" -> "Bạn thích vibe nào hơn: tối giản, trẻ trung, lịch sự hay nổi bật?";
            default -> "Cho mình thêm 1 chi tiết về nhu cầu để mình gợi ý sát hơn nhé?";
        };
    }

    public String buildClarifyingQuestion(ClarifyingQuestion question) {
        if (question == null) {
            return "Mình có thể gợi ý nhanh cho bạn, nhưng cho mình thêm 1 chi tiết ngắn để chọn đúng hơn nhé?";
        }
        return question.question();
    }

    public String composeRecommendationReply(ChatSession.PreferenceProfile profile,
                                             List<ChatResponse.ProductSuggestion> suggestions) {
        if (suggestions == null || suggestions.isEmpty()) {
            return "";
        }

        List<ChatResponse.ProductSuggestion> topPicks = new ArrayList<>(suggestions.stream().limit(3).toList());
        StringBuilder reply = new StringBuilder();
        reply.append(buildRecommendationLead(profile));

        for (int i = 0; i < topPicks.size(); i++) {
            ChatResponse.ProductSuggestion suggestion = topPicks.get(i);
            reply.append("\n\nOption ").append(i + 1).append(": ").append(suggestion.getName()).append(". ");
            reply.append(buildReason(profile, suggestion)).append(" ");
            reply.append(buildStylingHint(profile, suggestion));
        }

        reply.append("\n\n").append(closingEngine.buildSoftClose(profile, topPicks.size()));
        return reply.toString().trim();
    }

    public String composeComparisonReply(ChatSession.PreferenceProfile profile,
                                         com.fashion.chatbotservice.sales.CompareEngine.CompareResult compareResult) {
        if (compareResult == null || compareResult.saferChoice() == null) {
            return "";
        }

        StringBuilder reply = new StringBuilder();
        reply.append("Nếu cần chốt nhanh, mình tách ngắn gọn thế này nhé:");
        reply.append("\n\n- An toàn nhất: ").append(compareResult.saferChoice().getName())
                .append(". ").append(compareResult.saferReason());
        if (compareResult.moreStylishChoice() != null) {
            reply.append("\n- Nổi bật hơn: ").append(compareResult.moreStylishChoice().getName())
                    .append(". ").append(compareResult.stylishReason());
        }
        if (compareResult.betterValueChoice() != null) {
            reply.append("\n- Đáng tiền hơn: ").append(compareResult.betterValueChoice().getName())
                    .append(". ").append(compareResult.valueReason());
        }
        if (compareResult.easierToStyleChoice() != null) {
            reply.append("\n- Dễ phối nhất: ").append(compareResult.easierToStyleChoice().getName())
                    .append(". ").append(compareResult.easyStyleReason());
        }

        reply.append("\n\n").append(closingEngine.buildDecisionClose(profile, compareResult));
        return reply.toString();
    }

    public String appendStageAwareClose(String baseReply,
                                        List<?> suggestions,
                                        ChatSession.PreferenceProfile profile) {
        String safeBase = baseReply == null ? "" : baseReply.trim();
        SalesStage stage = profile != null ? profile.getSalesStage() : null;
        if (stage == SalesStage.CLOSING || stage == SalesStage.COMPARING || stage == SalesStage.RECOMMENDING) {
            String close = closingEngine.buildSoftClose(profile, suggestions == null ? 0 : suggestions.size());
            if (!close.isBlank() && !safeBase.toLowerCase().contains(close.toLowerCase())) {
                return safeBase.isBlank() ? close : safeBase + "\n\n" + close;
            }
        }
        return safeBase;
    }

    private String buildRecommendationLead(ChatSession.PreferenceProfile profile) {
        if (profile == null || profile.getStylingSlots() == null) {
            return "Mình gợi ý nhanh 2-3 option phù hợp để bạn chọn dễ hơn nhé:";
        }

        List<String> context = new ArrayList<>();
        if (profile.getStylingSlots().getOccasion() != null) {
            context.add("dịp " + profile.getStylingSlots().getOccasion());
        }
        if (profile.getStylingSlots().getStyleVibe() != null) {
            context.add("vibe " + profile.getStylingSlots().getStyleVibe());
        }
        if (context.isEmpty()) {
            return "Mình gợi ý nhanh 2-3 option phù hợp để bạn chọn dễ hơn nhé:";
        }
        return "Mình gợi ý nhanh 2-3 option hợp với " + String.join(" và ", context) + " để bạn chọn dễ hơn nhé:";
    }

    private String buildReason(ChatSession.PreferenceProfile profile, ChatResponse.ProductSuggestion suggestion) {
        if (suggestion.getReason() != null && !suggestion.getReason().isBlank()) {
            return suggestion.getReason();
        }
        String occasion = profile != null && profile.getStylingSlots() != null
                ? profile.getStylingSlots().getOccasion()
                : null;
        String normalizedName = ((suggestion.getName() == null ? "" : suggestion.getName()) + " "
                + (suggestion.getCategory() == null ? "" : suggestion.getCategory())).toLowerCase();

        if ("office".equals(occasion)) {
            return "Mẫu này hợp đi làm vì nhìn gọn và giữ cảm giác chỉn chu.";
        }
        if ("date".equals(occasion)) {
            return "Mẫu này hợp đi date vì dễ tạo thiện cảm mà không bị quá gồng.";
        }
        if ("daily".equals(occasion)) {
            return "Mẫu này hợp mặc hằng ngày vì dễ phối và không quá kén người mặc.";
        }
        if (normalizedName.contains("linen") || normalizedName.contains("basic")) {
            return "Mẫu này là lựa chọn an toàn vì dễ mặc và dễ phối hơn.";
        }
        return "Mẫu này hợp vì form và tinh thần khá dễ ứng dụng trong nhiều tình huống.";
    }

    private String buildStylingHint(ChatSession.PreferenceProfile profile, ChatResponse.ProductSuggestion suggestion) {
        if (profile != null && profile.getStylingSlots() != null) {
            Integer height = profile.getStylingSlots().getHeightCm();
            Integer weight = profile.getStylingSlots().getWeightKg();
            String occasion = profile.getStylingSlots().getOccasion();
            if (height != null && height < 160) {
                return "Neu muon tong the cao rao hon, ban nen giu phan duoi gon va tranh item qua dai.";
            }
            if (weight != null && weight < 55) {
                return "Neu muon dang can doi hon, ban nen mix voi item regular hoac oversize nhe thay vi qua om.";
            }
            if ("date".equals(occasion)) {
                return "Neu can mac di hen ho, ban nen giu tong the mem va sach thay vi phoi qua nhieu chi tiet.";
            }
            if ("work".equals(occasion)) {
                return "Neu mac di lam, ban nen phoi cung quan hoac chan vay gon de tong the sach va de tao thien cam.";
            }
        }
        String color = suggestion.getAvailableColors() != null && !suggestion.getAvailableColors().isEmpty()
                ? suggestion.getAvailableColors().get(0)
                : null;
        if (color != null && !color.isBlank()) {
            return "Bạn có thể phối với tông " + color + " hoặc đồ trung tính để mặc dễ hơn.";
        }
        if (profile != null && profile.getStylingSlots() != null && "minimal".equals(profile.getStylingSlots().getStyleVibe())) {
            return "Nếu muốn giữ vibe tối giản, bạn nên phối cùng quần hoặc chân váy màu trung tính.";
        }
        return "Nếu muốn mặc an toàn, bạn chỉ cần phối với item cơ bản màu trung tính là đủ.";
    }
}
