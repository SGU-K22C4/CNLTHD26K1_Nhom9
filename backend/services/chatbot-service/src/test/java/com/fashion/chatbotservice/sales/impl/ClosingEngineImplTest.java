package com.fashion.chatbotservice.sales.impl;

import com.fashion.chatbotservice.conversation.SalesStage;
import com.fashion.chatbotservice.dto.ChatResponse;
import com.fashion.chatbotservice.model.ChatSession;
import com.fashion.chatbotservice.sales.CompareEngine;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ClosingEngineImplTest {

    private final ClosingEngineImpl service = new ClosingEngineImpl();

    @Test
    void shouldGenerateSoftCloseForRecommendationStage() {
        ChatSession.PreferenceProfile profile = ChatSession.PreferenceProfile.empty();
        profile.setSalesStage(SalesStage.RECOMMENDING);

        String reply = service.buildSoftClose(profile, 3);

        assertTrue(reply.contains("an") || reply.contains("noi bat"));
    }

    @Test
    void shouldGenerateDecisionCloseForComparingStage() {
        ChatSession.PreferenceProfile profile = ChatSession.PreferenceProfile.empty();
        profile.setSalesStage(SalesStage.COMPARING);

        CompareEngine.CompareResult compareResult = new CompareEngine.CompareResult(
                ChatResponse.ProductSuggestion.builder().name("Ao so mi lung phoi dang ten").build(),
                ChatResponse.ProductSuggestion.builder().name("Ao so mi soc tay phong").build(),
                ChatResponse.ProductSuggestion.builder().name("Ao so mi lung phoi dang ten").build(),
                ChatResponse.ProductSuggestion.builder().name("Ao so mi lung phoi dang ten").build(),
                "safe",
                "stylish",
                "value",
                "easy",
                "final");

        String reply = service.buildDecisionClose(profile, compareResult);

        assertTrue(reply.toLowerCase().contains("ao so mi lung phoi dang ten"));
        assertTrue(reply.toLowerCase().contains("ao so mi soc tay phong"));
    }
}
