package com.fashion.chatbotservice.agent;

import com.fashion.chatbotservice.service.KnowledgeBaseService;
import com.fashion.chatbotservice.service.OutfitRuleEngine;
import com.fashion.chatbotservice.service.ProductRecommendationService;
import com.fashion.chatbotservice.service.ProductTaxonomyService;
import com.fashion.chatbotservice.service.SizeAdvisorService;
import com.fashion.chatbotservice.service.SizeFitAdvisoryService;
import com.fashion.chatbotservice.util.VietnameseNormalizer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.web.reactive.function.client.WebClient;

import static org.junit.jupiter.api.Assertions.assertTrue;

class FashionToolsTest {

    private FashionTools fashionTools;

    @BeforeEach
    void setUp() {
        fashionTools = new FashionTools(
                WebClient.builder().build(),
                Mockito.mock(SizeAdvisorService.class),
                Mockito.mock(OutfitRuleEngine.class),
                Mockito.mock(KnowledgeBaseService.class),
                Mockito.mock(ProductRecommendationService.class),
                Mockito.mock(ProductTaxonomyService.class),
                Mockito.mock(SizeFitAdvisoryService.class));
        fashionTools.setCollector(new ToolResultCollector());
    }

    @Test
    void shouldAskGuestToLoginForWishlist() {
        String reply = fashionTools.getWishlistRecommendations("guest-123");
        assertTrue(VietnameseNormalizer.normalize(reply).contains("dang nhap"));
    }

    @Test
    void shouldAskGuestToLoginForLoyalty() {
        String reply = fashionTools.getLoyaltyBenefits("guest-123");
        assertTrue(VietnameseNormalizer.normalize(reply).contains("dang nhap"));
    }

    @Test
    void shouldAskForOrderNumberWhenMissing() {
        String reply = fashionTools.checkOrderByNumber(null);
        assertTrue(VietnameseNormalizer.normalize(reply).contains("ma don hang"));
    }

    @Test
    void shouldAskForKnowledgeQuestionWhenMissing() {
        String reply = fashionTools.searchKnowledge(null);
        assertTrue(VietnameseNormalizer.normalize(reply).contains("cau hoi"));
    }
}
