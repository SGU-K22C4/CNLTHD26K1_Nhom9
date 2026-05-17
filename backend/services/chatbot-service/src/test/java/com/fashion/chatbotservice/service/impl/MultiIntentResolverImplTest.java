package com.fashion.chatbotservice.service.impl;

import com.fashion.chatbotservice.agent.FashionTools;
import com.fashion.chatbotservice.agent.ToolResultCollector;
import com.fashion.chatbotservice.dto.ChatResponse;
import com.fashion.chatbotservice.model.ChatSession;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.web.reactive.function.client.WebClient;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MultiIntentResolverImplTest {

    @Test
    void shouldComposeDetailReviewAndPromotionForSelectedProduct() {
        StubFashionTools fashionTools = new StubFashionTools();
        MultiIntentResolverImpl resolver = new MultiIntentResolverImpl(fashionTools);
        ChatSession.PreferenceProfile profile = ChatSession.PreferenceProfile.empty();
        profile.setSelectedProductContext(ChatSession.SelectedProductContextSnapshot.builder()
                .productId("P1")
                .productName("Ao so mi selected")
                .category("Ao so mi")
                .categoryGender("female")
                .price("1199000")
                .link("/products/P1")
                .selectedAt(Instant.now())
                .build());
        ChatSession session = ChatSession.builder()
                .sessionId("s1")
                .userId("user-1")
                .preferenceProfile(profile)
                .build();
        ToolResultCollector collector = new ToolResultCollector();

        ChatResponse response = resolver.resolve(
                "s1",
                "review san pham nay the nao, thong tin chi tiet va khuyen mai hien tai?",
                session,
                collector);

        assertNotNull(response);
        assertEquals("PRODUCT_FOLLOW_UP", response.getIntent());
        assertTrue(response.getReply().contains("Thong tin chi tiet san pham")
                || response.getReply().contains("ThÃ´ng tin chi tiáº¿t sáº£n pháº©m"));
        assertTrue(response.getReply().contains("Danh gia san pham")
                || response.getReply().contains("ÄÃ¡nh giÃ¡ sáº£n pháº©m"));
        assertTrue(response.getReply().contains("PROMO10") || response.getReply().contains("MÃ£"));
        assertEquals("P1", response.getSuggestions().get(0).getProductId());
    }

    @Test
    void shouldResolveMultiIntentFromApproximateTitleWithoutSelectedCard() {
        StubFashionTools fashionTools = new StubFashionTools();
        MultiIntentResolverImpl resolver = new MultiIntentResolverImpl(fashionTools);
        ChatSession.PreferenceProfile profile = ChatSession.PreferenceProfile.empty();
        ChatSession session = ChatSession.builder()
                .sessionId("s2")
                .userId("user-2")
                .preferenceProfile(profile)
                .build();
        ToolResultCollector collector = new ToolResultCollector();

        ChatResponse response = resolver.resolve(
                "s2",
                "review san pham ao so mi lung phoi dang ten the nao thong tin chi tiet san pham va chuong trinh khuyen mai",
                session,
                collector);

        assertNotNull(response);
        assertEquals("PRODUCT_FOLLOW_UP", response.getIntent());
        assertEquals("P-title", response.getSuggestions().get(0).getProductId());
    }

    private static class StubFashionTools extends FashionTools {

        StubFashionTools() {
            super(
                    WebClient.builder().build(),
                    Mockito.mock(com.fashion.chatbotservice.service.SizeAdvisorService.class),
                    Mockito.mock(com.fashion.chatbotservice.service.OutfitRuleEngine.class),
                    Mockito.mock(com.fashion.chatbotservice.service.KnowledgeBaseService.class),
                    Mockito.mock(com.fashion.chatbotservice.service.ProductRecommendationService.class),
                    Mockito.mock(com.fashion.chatbotservice.service.ProductTaxonomyService.class),
                    Mockito.mock(com.fashion.chatbotservice.service.SizeFitAdvisoryService.class));
        }

        @Override
        public String getProductDetail(String productId) {
            collectorFromCurrent().addProducts(List.of(ChatResponse.ProductSuggestion.builder()
                    .productId(productId)
                    .name("Ao so mi selected")
                    .category("Ao so mi")
                    .price("1199000")
                    .availableSizes(List.of("S", "M"))
                    .availableColors(List.of("Den"))
                    .build()));
            return "Thong tin chi tiet san pham Ao so mi selected:\n- Gia: 1.199.000 d";
        }

        @Override
        public String getProductReviews(String productId) {
            return "Danh gia san pham:\n- Diem trung binh: 4.5/5";
        }

        @Override
        public String getActivePromotions() {
            collectorFromCurrent().addPromotions(List.of(ChatResponse.PromotionSuggestion.builder()
                    .code("PROMO10")
                    .discountType("PERCENT")
                    .discountValue("10%")
                    .minOrderAmount("500000")
                    .endDate("2026-12-31")
                    .build()));
            return "Ma PROMO10 dang ap dung giam 10%.";
        }

        @Override
        public String searchProductsStrict(String search, Long minPrice, Long maxPrice, String color, String size) {
            if (search.contains("ao so mi lung phoi dang ten")) {
                collectorFromCurrent().addProducts(List.of(ChatResponse.ProductSuggestion.builder()
                        .productId("P-title")
                        .name("Ao so mi lung phoi dang ten")
                        .category("Ao so mi")
                        .price("1199000")
                        .availableSizes(List.of("S", "M"))
                        .availableColors(List.of("Trang"))
                        .build()));
            }
            return "Da tim thay san pham";
        }

        private ToolResultCollector collectorFromCurrent() {
            try {
                Field field = FashionTools.class.getDeclaredField("collectorHolder");
                field.setAccessible(true);
                @SuppressWarnings("unchecked")
                ThreadLocal<ToolResultCollector> holder = (ThreadLocal<ToolResultCollector>) field.get(this);
                return holder.get();
            } catch (Exception ex) {
                throw new IllegalStateException(ex);
            }
        }
    }
}
