package com.fashion.chatbotservice.product.impl;

import com.fashion.chatbotservice.dto.ChatResponse;
import com.fashion.chatbotservice.product.ProductMetadataProfile;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductMetadataEnrichmentServiceImplTest {

    private final ProductMetadataEnrichmentServiceImpl service = new ProductMetadataEnrichmentServiceImpl();

    @Test
    void shouldEnrichOfficeShirtWithUsefulMetadata() {
        ChatResponse.ProductSuggestion suggestion = ChatResponse.ProductSuggestion.builder()
                .productId("P-SHIRT-1")
                .name("Ao so mi linen dang ten")
                .category("Ao so mi")
                .build();

        ProductMetadataProfile metadata = service.enrich(suggestion);

        assertEquals("P-SHIRT-1", metadata.getProductId());
        assertTrue(metadata.getStyleTags().contains("office"));
        assertTrue(metadata.getOccasionTags().contains("work"));
        assertTrue(metadata.getSeasonTags().contains("summer"));
        assertTrue(metadata.getWhyBuyTags().contains("looks_premium"));
        assertTrue(metadata.getVersatilityScore() >= 8);
    }

    @Test
    void shouldFlagPatternedItemsAsMoreStatementDriven() {
        ChatResponse.ProductSuggestion suggestion = ChatResponse.ProductSuggestion.builder()
                .productId("P-PATTERN-1")
                .name("Ao so mi soc tay phong")
                .category("Ao so mi")
                .build();

        ProductMetadataProfile metadata = service.enrich(suggestion);

        assertTrue(metadata.getStyleTags().contains("statement"));
        assertEquals("medium", metadata.getFashionRisk());
        assertEquals("medium", metadata.getStylingDifficulty());
    }
}
