package com.fashion.chatbotservice.styling.impl;

import com.fashion.chatbotservice.product.ProductMetadataProfile;
import com.fashion.chatbotservice.styling.OccasionAdvice;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OccasionAdvisorServiceImplTest {

    private final OccasionAdvisorServiceImpl service = new OccasionAdvisorServiceImpl();

    @Test
    void shouldGiveDateSpecificDirection() {
        OccasionAdvice advice = service.advise(
                "minh can do di date toi nay",
                null,
                ProductMetadataProfile.builder()
                        .occasionTags(Set.of("date", "daily"))
                        .build());

        assertEquals("date", advice.getOccasion());
        assertTrue(advice.getRecommendedDirection().toLowerCase().contains("thiện cảm")
                || advice.getRecommendedDirection().toLowerCase().contains("thien cam"));
        assertTrue(advice.getAvoid().stream().anyMatch(item -> item.toLowerCase().contains("formal")));
    }
}
