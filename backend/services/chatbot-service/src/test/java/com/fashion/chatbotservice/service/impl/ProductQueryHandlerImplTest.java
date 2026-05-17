package com.fashion.chatbotservice.service.impl;

import com.fashion.chatbotservice.agent.FashionTools;
import com.fashion.chatbotservice.agent.ToolResultCollector;
import com.fashion.chatbotservice.dto.ChatResponse;
import com.fashion.chatbotservice.model.ChatSession;
import com.fashion.chatbotservice.service.KnowledgeBaseService;
import com.fashion.chatbotservice.service.OutfitRuleEngine;
import com.fashion.chatbotservice.service.ProductRecommendationService;
import com.fashion.chatbotservice.service.ProductTaxonomyService;
import com.fashion.chatbotservice.service.SizeAdvisorService;
import com.fashion.chatbotservice.service.SizeFitAdvisoryService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.web.reactive.function.client.WebClient;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductQueryHandlerImplTest {

    @Test
    void shouldIgnoreExplicitLookupForAttributeOnlyRefinement() {
        ProductQueryHandlerImpl handler = new ProductQueryHandlerImpl(new StubFashionTools());
        ChatSession.PreferenceProfile profile = ChatSession.PreferenceProfile.empty();
        profile.setLastProductCategoryQueried("ao so mi");
        ChatSession session = ChatSession.builder()
                .sessionId("s1")
                .userId("u1")
                .preferenceProfile(profile)
                .build();

        ChatResponse response = handler.handleExplicitLookup(
                "s1", "co mau den khong", session, new ToolResultCollector());

        assertNull(response);
    }

    @Test
    void shouldKeepLastCategoryDuringColorRefinement() {
        StubFashionTools fashionTools = new StubFashionTools();
        ProductQueryHandlerImpl handler = new ProductQueryHandlerImpl(fashionTools);
        ChatSession.PreferenceProfile profile = ChatSession.PreferenceProfile.empty();
        profile.setLastProductCategoryQueried("ao so mi");
        profile.setLastProductQueryTime(Instant.now());
        ChatSession session = ChatSession.builder()
                .sessionId("s2")
                .userId("u2")
                .preferenceProfile(profile)
                .build();
        ToolResultCollector collector = new ToolResultCollector();

        handler.searchWithContext("s2", "co mau den khong", session, collector);

        assertEquals("ao so mi", fashionTools.lastSearchKeyword);
        assertEquals("đen", fashionTools.lastColorFilter);
        assertFalse(fashionTools.browseCalled);
    }

    @Test
    void shouldBrowseForBudgetOnlyQueryInsteadOfReusingOldCategory() {
        StubFashionTools fashionTools = new StubFashionTools();
        ProductQueryHandlerImpl handler = new ProductQueryHandlerImpl(fashionTools);
        ChatSession.PreferenceProfile profile = ChatSession.PreferenceProfile.empty();
        profile.setLastProductCategoryQueried("ao so mi");
        profile.setBudget("1600000");
        ChatSession session = ChatSession.builder()
                .sessionId("s3")
                .userId("u3")
                .preferenceProfile(profile)
                .build();

        ChatResponse response = handler.searchWithContext(
                "s3",
                "goi y cho minh list san pham tu 1tr den 1tr6",
                session,
                new ToolResultCollector());

        assertTrue(fashionTools.browseCalled);
        assertEquals(Long.valueOf(1_000_000L), fashionTools.lastBrowseMinPrice);
        assertEquals(Long.valueOf(1_600_000L), fashionTools.lastBrowseMaxPrice);
        assertTrue(response.getReply().contains("ngân sách") || response.getReply().contains("gom sẵn"));
    }

    @Test
    void shouldTreatBestSellerAsBrowseIntent() {
        StubFashionTools fashionTools = new StubFashionTools();
        ProductQueryHandlerImpl handler = new ProductQueryHandlerImpl(fashionTools);
        ChatSession.PreferenceProfile profile = ChatSession.PreferenceProfile.empty();
        ChatSession session = ChatSession.builder()
                .sessionId("s4")
                .userId("u4")
                .preferenceProfile(profile)
                .build();

        assertTrue(handler.shouldHandleDirectSearch("sản phẩm nào nhiều lượt bán nhất", profile));

        ChatResponse response = handler.searchWithContext(
                "s4",
                "sản phẩm nào nhiều lượt bán nhất",
                session,
                new ToolResultCollector());

        assertTrue(fashionTools.browseCalled);
        assertTrue(response.getReply().contains("dễ chốt") || response.getReply().contains("nổi bật"));
    }

    private static class StubFashionTools extends FashionTools {

        private String lastSearchKeyword;
        private String lastColorFilter;
        private boolean browseCalled;
        private Long lastBrowseMinPrice;
        private Long lastBrowseMaxPrice;

        StubFashionTools() {
            super(
                    WebClient.builder().build(),
                    Mockito.mock(SizeAdvisorService.class),
                    Mockito.mock(OutfitRuleEngine.class),
                    Mockito.mock(KnowledgeBaseService.class),
                    Mockito.mock(ProductRecommendationService.class),
                    Mockito.mock(ProductTaxonomyService.class),
                    Mockito.mock(SizeFitAdvisoryService.class));
        }

        @Override
        public String searchProducts(String search, Long minPrice, Long maxPrice, String color, String size) {
            this.lastSearchKeyword = search;
            this.lastColorFilter = color;
            collectorFromCurrent().addProducts(List.of(ChatResponse.ProductSuggestion.builder()
                    .productId("P1")
                    .name("Ao so mi Oxford")
                    .category("Ao so mi")
                    .price("599000")
                    .availableSizes(List.of("S", "M"))
                    .build()));
            return "Da tim thay san pham phu hop";
        }

        @Override
        public String browseProducts(Long minPrice, Long maxPrice, String color, String size) {
            this.browseCalled = true;
            this.lastBrowseMinPrice = minPrice;
            this.lastBrowseMaxPrice = maxPrice;
            collectorFromCurrent().addProducts(List.of(ChatResponse.ProductSuggestion.builder()
                    .productId("P2")
                    .name("Dam midi de mac")
                    .category("Dam")
                    .price("1399000")
                    .availableSizes(List.of("S", "M"))
                    .build()));
            return "Mình đã gom sẵn vài mẫu phù hợp để bạn xem nhanh bên dưới.";
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
