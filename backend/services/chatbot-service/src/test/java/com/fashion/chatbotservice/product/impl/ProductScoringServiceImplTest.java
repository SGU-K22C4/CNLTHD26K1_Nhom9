package com.fashion.chatbotservice.product.impl;

import com.fashion.chatbotservice.conversation.StylingSlots;
import com.fashion.chatbotservice.dto.ChatResponse;
import com.fashion.chatbotservice.model.ChatSession;
import com.fashion.chatbotservice.product.ProductMetadataProfile;
import com.fashion.chatbotservice.product.ProductScoringService;
import com.fashion.chatbotservice.styling.impl.BodyShapeAdvisorServiceImpl;
import com.fashion.chatbotservice.styling.impl.OccasionAdvisorServiceImpl;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductScoringServiceImplTest {

    private final ProductScoringServiceImpl service = new ProductScoringServiceImpl(
            new BodyShapeAdvisorServiceImpl(),
            new OccasionAdvisorServiceImpl());

    @Test
    void shouldRewardMetadataThatMatchesOfficeBudgetAndSizeNeed() {
        ChatSession.PreferenceProfile profile = ChatSession.PreferenceProfile.empty();
        profile.setBudget("duoi 1500000");
        profile.setStylingSlots(new StylingSlots());
        profile.getStylingSlots().setOccasion("work");
        profile.getStylingSlots().setStyleVibe("minimal");
        profile.getStylingSlots().setHeightCm(163);
        profile.getStylingSlots().setWeightKg(52);

        ChatResponse.ProductSuggestion suggestion = ChatResponse.ProductSuggestion.builder()
                .productId("P1")
                .name("Ao so mi regular linen")
                .category("Ao so mi")
                .price("1199000")
                .availableSizes(List.of("S", "M"))
                .availableColors(List.of("trang", "den"))
                .build();

        ProductMetadataProfile metadata = ProductMetadataProfile.builder()
                .styleTags(Set.of("minimal", "office", "smart_casual"))
                .occasionTags(Set.of("work", "daily"))
                .fitTags(Set.of("regular"))
                .whyBuyTags(Set.of("easy_to_match", "safe_choice"))
                .fashionRisk("safe")
                .versatilityScore(9)
                .build();

        ProductScoringService.ScoreResult result = service.score(
                suggestion,
                metadata,
                profile,
                "ao so mi di lam toi gian",
                null,
                1_500_000L,
                "trang",
                "S");

        assertTrue(result.score() > 5.0d);
        assertTrue(result.reasons().stream().anyMatch(reason -> reason.toLowerCase().contains("size")));
        assertTrue(result.reasons().size() >= 3);
    }
}
