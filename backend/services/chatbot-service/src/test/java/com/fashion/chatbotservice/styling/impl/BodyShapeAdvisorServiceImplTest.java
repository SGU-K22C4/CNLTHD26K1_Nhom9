package com.fashion.chatbotservice.styling.impl;

import com.fashion.chatbotservice.conversation.StylingSlots;
import com.fashion.chatbotservice.model.ChatSession;
import com.fashion.chatbotservice.product.ProductMetadataProfile;
import com.fashion.chatbotservice.styling.BodyShapeAdvice;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BodyShapeAdvisorServiceImplTest {

    private final BodyShapeAdvisorServiceImpl service = new BodyShapeAdvisorServiceImpl();

    @Test
    void shouldRecommendRegularOrLightOversizeForSlimProfile() {
        ChatSession.PreferenceProfile profile = ChatSession.PreferenceProfile.empty();
        profile.setStylingSlots(StylingSlots.builder()
                .heightCm(163)
                .weightKg(52)
                .fitPreference("thoai mai")
                .build());

        BodyShapeAdvice advice = service.advise(profile, ProductMetadataProfile.empty());

        assertEquals("oversized", advice.getRecommendedFit());
        assertTrue(advice.getStylingTips().stream().anyMatch(tip -> tip.toLowerCase().contains("đầy đặn")
                || tip.toLowerCase().contains("can doi")));
        assertTrue(advice.getConfidence() > 0.5d);
    }
}
