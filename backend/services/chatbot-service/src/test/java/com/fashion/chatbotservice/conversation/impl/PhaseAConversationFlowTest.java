package com.fashion.chatbotservice.conversation.impl;

import com.fashion.chatbotservice.agent.FashionAgent;
import com.fashion.chatbotservice.agent.FashionTools;
import com.fashion.chatbotservice.agent.ResponseGuardrail;
import com.fashion.chatbotservice.config.AgentConfig;
import com.fashion.chatbotservice.dto.ChatRequest;
import com.fashion.chatbotservice.dto.ChatResponse;
import com.fashion.chatbotservice.model.ChatSession;
import com.fashion.chatbotservice.repository.ChatSessionRepository;
import com.fashion.chatbotservice.response.FashionResponseComposer;
import com.fashion.chatbotservice.sales.impl.ClosingEngineImpl;
import com.fashion.chatbotservice.sales.impl.CompareEngineImpl;
import com.fashion.chatbotservice.service.ChatAnalyticsService;
import com.fashion.chatbotservice.service.IntentClassifierService;
import com.fashion.chatbotservice.service.KnowledgeBaseService;
import com.fashion.chatbotservice.service.MultiIntentResolver;
import com.fashion.chatbotservice.service.OutfitRuleEngine;
import com.fashion.chatbotservice.service.ProductQueryHandler;
import com.fashion.chatbotservice.service.ProductRecommendationService;
import com.fashion.chatbotservice.service.ProductTaxonomyService;
import com.fashion.chatbotservice.service.ProfileEnrichmentService;
import com.fashion.chatbotservice.service.SizeAdvisorService;
import com.fashion.chatbotservice.service.SizeFitAdvisoryService;
import com.fashion.chatbotservice.service.impl.ChatbotServiceImpl;
import com.fashion.chatbotservice.service.impl.FallbackHandlerImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

class PhaseAConversationFlowTest {

    private ChatbotServiceImpl service;
    private ChatSessionRepository sessionRepository;
    private ProfileEnrichmentService profileEnrichmentService;
    private IntentClassifierService intentClassifierService;
    private ProductQueryHandler productQueryHandler;

    @BeforeEach
    void setUp() {
        FashionAgent fashionAgent = Mockito.mock(FashionAgent.class);
        sessionRepository = Mockito.mock(ChatSessionRepository.class);
        AgentConfig agentConfig = new AgentConfig(sessionRepository);
        intentClassifierService = Mockito.mock(IntentClassifierService.class);
        profileEnrichmentService = Mockito.mock(ProfileEnrichmentService.class);
        ChatAnalyticsService analyticsService = Mockito.mock(ChatAnalyticsService.class);
        SizeAdvisorService sizeAdvisorService = Mockito.mock(SizeAdvisorService.class);
        SizeFitAdvisoryService sizeFitAdvisoryService = Mockito.mock(SizeFitAdvisoryService.class);
        productQueryHandler = Mockito.mock(ProductQueryHandler.class);
        MultiIntentResolver multiIntentResolver = Mockito.mock(MultiIntentResolver.class);
        SlotFillingServiceImpl slotFillingService = new SlotFillingServiceImpl();
        ConversationStateServiceImpl conversationStateService = new ConversationStateServiceImpl(slotFillingService);
        FashionResponseComposer fashionResponseComposer = new FashionResponseComposer(new ClosingEngineImpl());

        FashionTools fashionTools = new FashionTools(
                WebClient.builder().build(),
                sizeAdvisorService,
                Mockito.mock(OutfitRuleEngine.class),
                Mockito.mock(KnowledgeBaseService.class),
                Mockito.mock(ProductRecommendationService.class),
                Mockito.mock(ProductTaxonomyService.class),
                sizeFitAdvisoryService);
        FallbackHandlerImpl fallbackHandler = new FallbackHandlerImpl(
                intentClassifierService,
                fashionTools,
                sizeAdvisorService,
                sizeFitAdvisoryService,
                productQueryHandler);

        service = new ChatbotServiceImpl(
                fashionAgent,
                fashionTools,
                agentConfig,
                sessionRepository,
                intentClassifierService,
                profileEnrichmentService,
                analyticsService,
                sizeAdvisorService,
                sizeFitAdvisoryService,
                fallbackHandler,
                productQueryHandler,
                multiIntentResolver,
                new ResponseGuardrail(),
                conversationStateService,
                slotFillingService,
                fashionResponseComposer,
                new CompareEngineImpl(),
                WebClient.builder().build());

        when(sessionRepository.findBySessionId(anyString())).thenReturn(Optional.empty());
        when(sessionRepository.save(any(ChatSession.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(profileEnrichmentService.loadPersistedProfile(anyString())).thenReturn(ChatSession.PreferenceProfile.empty());
        doNothing().when(profileEnrichmentService).enrichFromMessage(any(), anyString());
        doNothing().when(profileEnrichmentService).enrichFromPurchaseHistory(any(), anyString());
        doNothing().when(profileEnrichmentService).enrichFromWishlist(any(), anyString());
        doNothing().when(profileEnrichmentService).enrichFromUserProfile(any(), anyString());
        doNothing().when(profileEnrichmentService).persistProfileAsync(anyString(), any());
        when(multiIntentResolver.resolve(anyString(), anyString(), any(), any())).thenReturn(null);
    }

    @Test
    void shouldAskOccasionFirstWhenConsultativeRequestIsTooBroad() {
        when(intentClassifierService.classify("Gợi ý đồ cho mình"))
                .thenReturn(new IntentClassifierService.IntentScore(IntentClassifierService.SEARCH_PRODUCT, 0.85d));

        ChatRequest request = new ChatRequest();
        request.setSessionId("phase-a-1");
        request.setMessage("Gợi ý đồ cho mình");

        ChatResponse response = service.chat(request, "user-1", "trace-a1");

        assertTrue(response.getReply().contains("dịp nào") || response.getReply().contains("dip nao"));
    }

    @Test
    void shouldComposeOptionBasedRecommendationWhenEnoughContextIsPresent() {
        when(intentClassifierService.classify("Mình cần áo sơ mi đi làm vibe tối giản"))
                .thenReturn(new IntentClassifierService.IntentScore(IntentClassifierService.SEARCH_PRODUCT, 0.9d));
        when(productQueryHandler.shouldHandleDirectSearch(anyString(), any())).thenReturn(true);
        when(productQueryHandler.searchWithContext(anyString(), anyString(), any(), any())).thenReturn(
                ChatResponse.builder()
                        .sessionId("phase-a-2")
                        .intent(IntentClassifierService.SEARCH_PRODUCT)
                        .confidence(0.9d)
                        .reply("Danh sách sản phẩm phù hợp")
                        .suggestions(new ArrayList<>(java.util.List.of(
                                ChatResponse.ProductSuggestion.builder()
                                        .productId("P1")
                                        .name("Áo sơ mi lửng phối đăng ten")
                                        .category("Áo sơ mi")
                                        .availableColors(java.util.List.of("trắng"))
                                        .build(),
                                ChatResponse.ProductSuggestion.builder()
                                        .productId("P2")
                                        .name("Áo sơ mi sọc tay phồng")
                                        .category("Áo sơ mi")
                                        .availableColors(java.util.List.of("đen"))
                                        .build())))
                        .promotions(new ArrayList<>())
                        .profile(ChatSession.PreferenceProfile.empty())
                        .createdAt(Instant.now())
                        .build());

        ChatRequest request = new ChatRequest();
        request.setSessionId("phase-a-2");
        request.setMessage("Mình cần áo sơ mi đi làm vibe tối giản");

        ChatResponse response = service.chat(request, "user-2", "trace-a2");

        assertTrue(response.getReply().contains("Option 1"));
        assertTrue(response.getReply().contains("đi làm") || response.getReply().contains("di lam"));
        assertTrue(response.getReply().contains("Bạn thích vibe") || response.getReply().contains("Nếu bạn muốn"));
        assertTrue(response.getSuggestions().size() <= 3);
    }
}
