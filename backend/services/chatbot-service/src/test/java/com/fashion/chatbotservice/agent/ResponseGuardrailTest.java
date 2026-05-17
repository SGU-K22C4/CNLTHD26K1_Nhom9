package com.fashion.chatbotservice.agent;

import com.fashion.chatbotservice.dto.ChatResponse;
import com.fashion.chatbotservice.util.VietnameseNormalizer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResponseGuardrailTest {

    private ResponseGuardrail guardrail;

    @BeforeEach
    void setUp() {
        guardrail = new ResponseGuardrail();
    }

    @Test
    void shouldReplaceHallucinatedPrice() {
        ToolResultCollector collector = new ToolResultCollector();
        collector.addProducts(List.of(ChatResponse.ProductSuggestion.builder()
                .productId("P1")
                .name("Ao so mi Oxford")
                .price("599000")
                .availableSizes(List.of("S", "M"))
                .build()));

        String sanitized = guardrail.validateAndSanitize("Mau nay gia 899000d va rat de mac.", collector);

        assertTrue(VietnameseNormalizer.normalize(sanitized).contains("gia tren card san pham"));
        assertTrue(collector.getGuardrailViolations().contains("hallucinated_price"));
    }

    @Test
    void shouldSoftenPolicyAnswerWithoutKnowledgeSource() {
        ToolResultCollector collector = new ToolResultCollector();

        String sanitized = guardrail.validateAndSanitize("Chinh sach doi tra trong 30 ngay neu con tem mac.", collector);

        assertTrue(VietnameseNormalizer.normalize(sanitized).contains("nguon chinh sach chinh xac"));
        assertTrue(collector.getGuardrailViolations().contains("policy_without_source"));
    }

    @Test
    void shouldNeutralizeInvalidStockClaim() {
        ToolResultCollector collector = new ToolResultCollector();
        collector.addProducts(List.of(ChatResponse.ProductSuggestion.builder()
                .productId("P2")
                .name("Quan tay den")
                .price("699000")
                .availableSizes(List.of())
                .build()));

        String sanitized = guardrail.validateAndSanitize("Mau nay con hang va co san nhe.", collector);

        assertFalse(VietnameseNormalizer.normalize(sanitized).contains("con hang"));
        assertTrue(collector.getGuardrailViolations().contains("invalid_stock_claim"));
    }
}
