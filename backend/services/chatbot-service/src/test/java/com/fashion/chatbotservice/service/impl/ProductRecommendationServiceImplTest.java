package com.fashion.chatbotservice.service.impl;

import com.fashion.chatbotservice.dto.ChatResponse;
import com.fashion.chatbotservice.model.ChatSession;
import com.fashion.chatbotservice.product.impl.ProductMetadataEnrichmentServiceImpl;
import com.fashion.chatbotservice.product.impl.ProductScoringServiceImpl;
import com.fashion.chatbotservice.styling.impl.BodyShapeAdvisorServiceImpl;
import com.fashion.chatbotservice.styling.impl.OccasionAdvisorServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ProductRecommendationServiceImplTest {

    private ProductRecommendationServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ProductRecommendationServiceImpl(
                new ProductTaxonomyServiceImpl(),
                new ProductMetadataEnrichmentServiceImpl(),
                new ProductScoringServiceImpl(
                        new BodyShapeAdvisorServiceImpl(),
                        new OccasionAdvisorServiceImpl()));
    }

    @Test
    void shouldRankSuggestionWithMatchingSizeColorAndBudgetHigher() {
        ChatSession.PreferenceProfile profile = ChatSession.PreferenceProfile.empty();
        profile.setBudget("duoi 1000000");
        profile.getPreferredSizes().add("S");
        profile.getPreferredColors().add("đen");
        profile.getPreferredCategories().add("áo");

        ChatResponse.ProductSuggestion matching = ChatResponse.ProductSuggestion.builder()
                .productId("P1")
                .name("Ao so mi den office")
                .category("Áo sơ mi")
                .price("799000")
                .availableSizes(List.of("S", "M"))
                .availableColors(List.of("đen", "trắng"))
                .build();

        ChatResponse.ProductSuggestion weak = ChatResponse.ProductSuggestion.builder()
                .productId("P2")
                .name("Dam midi statement")
                .category("Váy")
                .price("1899000")
                .availableSizes(List.of("L"))
                .availableColors(List.of("đỏ"))
                .build();

        List<ChatResponse.ProductSuggestion> ranked = service.rankSuggestions(
                List.of(weak, matching),
                profile,
                "ao di lam mau den size S",
                null,
                1_000_000L,
                "đen",
                "S");

        assertEquals("P1", ranked.getFirst().getProductId());
        assertNotNull(ranked.getFirst().getReason());
    }
}
