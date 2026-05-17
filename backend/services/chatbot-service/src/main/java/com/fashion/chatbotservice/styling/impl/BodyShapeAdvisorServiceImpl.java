package com.fashion.chatbotservice.styling.impl;

import com.fashion.chatbotservice.conversation.StylingSlots;
import com.fashion.chatbotservice.model.ChatSession;
import com.fashion.chatbotservice.product.ProductMetadataProfile;
import com.fashion.chatbotservice.styling.BodyShapeAdvice;
import com.fashion.chatbotservice.styling.BodyShapeAdvisorService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class BodyShapeAdvisorServiceImpl implements BodyShapeAdvisorService {

    @Override
    public BodyShapeAdvice advise(ChatSession.PreferenceProfile profile, ProductMetadataProfile metadata) {
        if (profile == null || profile.getStylingSlots() == null) {
            return BodyShapeAdvice.empty();
        }

        StylingSlots slots = profile.getStylingSlots();
        Integer height = slots.getHeightCm();
        Integer weight = slots.getWeightKg();
        String fitPreference = normalize(slots.getFitPreference() != null ? slots.getFitPreference() : profile.getFitPreference());

        if (height == null && weight == null && fitPreference.isBlank()) {
            return BodyShapeAdvice.empty();
        }

        List<String> tips = new ArrayList<>();
        List<String> avoid = new ArrayList<>();
        String recommendedFit = "regular";
        double confidence = 0.4d;

        // Keep these rules deterministic so product scoring remains stable even when the
        // agent path fails. This is the core stylist layer for body-shape guidance.
        if (height != null && height < 160) {
            recommendedFit = chooseFit(recommendedFit, "gọn");
            tips.add("Ưu tiên item có tỷ lệ gọn để tổng thể nhìn cao ráo hơn.");
            avoid.add("Tránh áo quá dài hoặc quần/váy kéo tỷ lệ cơ thể bị thấp xuống.");
            confidence += 0.25d;
        }

        if (weight != null && weight < 55) {
            recommendedFit = chooseFit(recommendedFit, "regular");
            tips.add("Form regular hoặc oversize nhẹ sẽ giúp dáng đầy đặn và cân đối hơn.");
            avoid.add("Tránh form quá ôm vì dễ làm lộ cảm giác gầy.");
            confidence += 0.25d;
        } else if (weight != null && weight >= 70) {
            recommendedFit = chooseFit(recommendedFit, "structured");
            tips.add("Form đứng, rũ vừa phải sẽ giúp tổng thể gọn hơn.");
            avoid.add("Tránh chất quá bó hoặc chi tiết ôm sát ở vùng thân.");
            confidence += 0.25d;
        }

        if (containsAny(fitPreference, "thoai mai", "oversize", "rong")) {
            recommendedFit = chooseFit(recommendedFit, "oversized");
            tips.add("Bạn nghiêng về cảm giác thoải mái nên ưu tiên form rộng vừa đủ.");
            confidence += 0.1d;
        } else if (containsAny(fitPreference, "gon", "vua van", "fit")) {
            recommendedFit = chooseFit(recommendedFit, "regular");
            tips.add("Bạn hợp hướng form vừa vặn để giữ tổng thể sạch và dễ mặc.");
            confidence += 0.1d;
        }

        if (metadata != null && metadata.getFitTags() != null && metadata.getFitTags().contains("structured")) {
            tips.add("Item này có form đứng nên khá dễ cân tỷ lệ cơ thể.");
        }

        return BodyShapeAdvice.builder()
                .recommendedFit(recommendedFit)
                .stylingTips(tips)
                .avoid(avoid)
                .confidence(Math.min(1.0d, confidence))
                .build();
    }

    private String chooseFit(String current, String candidate) {
        if (current == null || current.isBlank() || "regular".equals(current)) {
            return candidate;
        }
        return current;
    }

    private boolean containsAny(String haystack, String... needles) {
        for (String needle : needles) {
            if (haystack.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }
}
