package com.fashion.chatbotservice.service.impl;

import com.fashion.chatbotservice.agent.FashionTools;
import com.fashion.chatbotservice.agent.ToolResultCollector;
import com.fashion.chatbotservice.dto.ChatResponse;
import com.fashion.chatbotservice.model.ChatSession;
import com.fashion.chatbotservice.service.IntentClassifierService;
import com.fashion.chatbotservice.service.KnowledgeBaseService;
import com.fashion.chatbotservice.service.OutfitRuleEngine;
import com.fashion.chatbotservice.service.ProductQueryHandler;
import com.fashion.chatbotservice.service.ProductRecommendationService;
import com.fashion.chatbotservice.service.ProductTaxonomyService;
import com.fashion.chatbotservice.service.SizeAdvisorService;
import com.fashion.chatbotservice.service.SizeFitAdvisoryService;
import com.fashion.chatbotservice.util.VietnameseNormalizer;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.web.reactive.function.client.WebClient;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

class FallbackHandlerImplTest {

    @Test
    void shouldAskGuestToLoginForOrderFallback() {
        IntentClassifierService intentClassifierService = Mockito.mock(IntentClassifierService.class);
        when(intentClassifierService.classify("kiem tra don ORD-123"))
                .thenReturn(new IntentClassifierService.IntentScore(IntentClassifierService.CHECK_ORDER, 0.95d));

        FashionTools fashionTools = new FashionTools(
                WebClient.builder().build(),
                Mockito.mock(SizeAdvisorService.class),
                Mockito.mock(OutfitRuleEngine.class),
                Mockito.mock(KnowledgeBaseService.class),
                Mockito.mock(ProductRecommendationService.class),
                Mockito.mock(ProductTaxonomyService.class),
                Mockito.mock(SizeFitAdvisoryService.class));

        FallbackHandlerImpl handler = new FallbackHandlerImpl(
                intentClassifierService,
                fashionTools,
                Mockito.mock(SizeAdvisorService.class),
                Mockito.mock(SizeFitAdvisoryService.class),
                Mockito.mock(ProductQueryHandler.class));

        ChatSession session = ChatSession.builder()
                .sessionId("guest-session")
                .userId("guest-1")
                .preferenceProfile(ChatSession.PreferenceProfile.empty())
                .build();

        ChatResponse response = handler.handle("guest-session", "kiem tra don ORD-123", session, new ToolResultCollector());
        assertTrue(VietnameseNormalizer.normalize(response.getReply()).contains("dang nhap"));
    }
}
