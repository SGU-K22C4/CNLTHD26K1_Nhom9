package com.fashion.chatbotservice.sales.impl;

import com.fashion.chatbotservice.dto.ChatResponse;
import com.fashion.chatbotservice.sales.CompareEngine;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompareEngineImplTest {

    private final CompareEngine service = new CompareEngineImpl();

    @Test
    void shouldPreferSaferOptionForFinalRecommendation() {
        CompareEngine.CompareResult result = service.compare(List.of(
                ChatResponse.ProductSuggestion.builder()
                        .productId("P1")
                        .name("Ao so mi lung phoi dang ten")
                        .category("Ao so mi")
                        .price("1.199.000 d")
                        .availableSizes(List.of("S", "M"))
                        .build(),
                ChatResponse.ProductSuggestion.builder()
                        .productId("P2")
                        .name("Ao so mi soc tay phong")
                        .category("Ao so mi")
                        .price("1.399.000 d")
                        .availableSizes(List.of("S"))
                        .build()));

        assertEquals("Ao so mi lung phoi dang ten", result.saferChoice().getName());
        assertTrue(result.finalRecommendation().toLowerCase().contains("an"));
        assertTrue(result.saferReason().toLowerCase().contains("an"));
        assertTrue(result.valueReason().toLowerCase().contains("d"));
    }
}
