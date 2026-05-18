package com.fashion.chatbotservice.service.impl;

import com.fashion.chatbotservice.agent.FashionAgent;
import com.fashion.chatbotservice.agent.FashionTools;
import com.fashion.chatbotservice.agent.ResponseGuardrail;
import com.fashion.chatbotservice.config.AgentConfig;
import com.fashion.chatbotservice.conversation.impl.ConversationStateServiceImpl;
import com.fashion.chatbotservice.conversation.impl.SlotFillingServiceImpl;
import com.fashion.chatbotservice.dto.ChatRequest;
import com.fashion.chatbotservice.dto.ChatResponse;
import com.fashion.chatbotservice.model.ChatSession;
import com.fashion.chatbotservice.response.FashionResponseComposer;
import com.fashion.chatbotservice.repository.ChatSessionRepository;
import com.fashion.chatbotservice.sales.impl.ClosingEngineImpl;
import com.fashion.chatbotservice.sales.impl.CompareEngineImpl;
import com.fashion.chatbotservice.service.ChatAnalyticsService;
import com.fashion.chatbotservice.service.IntentClassifierService;
import com.fashion.chatbotservice.service.KnowledgeBaseService;
import com.fashion.chatbotservice.service.MultiIntentResolver;
import com.fashion.chatbotservice.service.OutfitRuleEngine;
import com.fashion.chatbotservice.service.ProductQueryHandler;
import com.fashion.chatbotservice.service.ProfileEnrichmentService;
import com.fashion.chatbotservice.service.ProductRecommendationService;
import com.fashion.chatbotservice.service.ProductTaxonomyService;
import com.fashion.chatbotservice.service.SizeAdvisorService;
import com.fashion.chatbotservice.service.SizeFitAdvisoryService;
import com.fashion.chatbotservice.util.VietnameseNormalizer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

class ChatbotServiceFallbackTest {

    private ChatbotServiceImpl service;
    private ChatSessionRepository sessionRepository;
    private ProfileEnrichmentService profileEnrichmentService;
    private IntentClassifierService intentClassifierService;
    private FashionAgent fashionAgent;
    @BeforeEach
    void setUp() {
        fashionAgent = Mockito.mock(FashionAgent.class);
        sessionRepository = Mockito.mock(ChatSessionRepository.class);
        AgentConfig agentConfig = new AgentConfig(sessionRepository);
        intentClassifierService = Mockito.mock(IntentClassifierService.class);
        profileEnrichmentService = Mockito.mock(ProfileEnrichmentService.class);
        ChatAnalyticsService analyticsService = Mockito.mock(ChatAnalyticsService.class);
        SizeAdvisorService sizeAdvisorService = Mockito.mock(SizeAdvisorService.class);
        SizeFitAdvisoryService sizeFitAdvisoryService = Mockito.mock(SizeFitAdvisoryService.class);
        ProductQueryHandler productQueryHandler = Mockito.mock(ProductQueryHandler.class);
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
        when(productQueryHandler.refreshForGenderContext(anyString(), anyString(), any())).thenReturn(
                ChatResponse.builder()
                        .sessionId("session-2")
                        .intent(IntentClassifierService.SEARCH_PRODUCT)
                        .confidence(0.9d)
                        .reply("Ok, mình sẽ ưu tiên lọc theo đồ nữ cho bạn trong các gợi ý tiếp theo nhé.")
                        .suggestions(new ArrayList<>())
                        .promotions(new ArrayList<>())
                        .profile(ChatSession.PreferenceProfile.empty())
                        .createdAt(java.time.Instant.now())
                        .build());
        when(multiIntentResolver.resolve(anyString(), anyString(), any(), any())).thenReturn(null);
        ReflectionTestUtils.setField(service, "useAgent", true);
    }

    @Test
    void shouldFallbackToLoginPromptForGuestOrderLookupWhenAgentReturnsEmpty() {
        when(intentClassifierService.classify("Kiem tra don ORD-123"))
                .thenReturn(new IntentClassifierService.IntentScore(IntentClassifierService.CHECK_ORDER, 0.95d));
        when(fashionAgent.chat(anyString(), anyString())).thenReturn(null);

        ChatRequest request = new ChatRequest();
        request.setSessionId("session-1");
        request.setMessage("Kiem tra don ORD-123");

        ChatResponse response = service.chat(request, null, "trace-1");

        assertTrue(VietnameseNormalizer.normalize(response.getReply()).contains("dang nhap"));
    }

    @Test
    void shouldAskGenderClarificationWhenProfileAndQueryConflict() {
        ChatSession.PreferenceProfile profile = ChatSession.PreferenceProfile.empty();
        profile.setTargetGender("male");
        ChatSession session = ChatSession.builder()
                .sessionId("session-2")
                .userId("user-1")
                .messages(new ArrayList<>(java.util.List.of(
                        ChatSession.ChatMessage.builder()
                                .sender(ChatSession.Sender.USER)
                                .content("Mình đang tìm đồ đi chơi cuối tuần")
                                .build())))
                .preferenceProfile(profile)
                .build();
        when(sessionRepository.findBySessionId("session-2")).thenReturn(Optional.of(session));
        when(intentClassifierService.classify("Minh muon xem dam midi")).thenReturn(
                new IntentClassifierService.IntentScore(IntentClassifierService.SEARCH_PRODUCT, 0.9d));

        ChatRequest request = new ChatRequest();
        request.setSessionId("session-2");
        request.setMessage("Minh muon xem dam midi");

        ChatResponse response = service.chat(request, "user-1", "trace-2");

        assertTrue(VietnameseNormalizer.normalize(response.getReply()).contains("mua giup nguoi khac"));
    }

    @Test
    void shouldAnswerRankingQuestionFromCurrentSuggestionList() {
        ChatSession.PreferenceProfile profile = ChatSession.PreferenceProfile.empty();
        ChatSession session = ChatSession.builder()
                .sessionId("session-3")
                .userId("user-3")
                .messages(new ArrayList<>(java.util.List.of(
                        ChatSession.ChatMessage.builder()
                                .sender(ChatSession.Sender.BOT)
                                .content("Day la list gan nhat")
                                .suggestions(new ArrayList<>(java.util.List.of(
                                        ChatSession.ProductSuggestionSnapshot.builder()
                                                .productId("P1")
                                                .name("Ao so mi lung phoi dang ten")
                                                .category("Ao so mi")
                                                .price("1199000")
                                                .availableSizes(java.util.List.of("S", "M"))
                                                .build(),
                                        ChatSession.ProductSuggestionSnapshot.builder()
                                                .productId("P2")
                                                .name("Ao so mi soc tay phong")
                                                .category("Ao so mi")
                                                .price("1399000")
                                                .availableSizes(java.util.List.of("S"))
                                                .build())))
                                .build())))
                .preferenceProfile(profile)
                .build();
        when(sessionRepository.findBySessionId("session-3")).thenReturn(Optional.of(session));
        when(intentClassifierService.classify("san pham nao nhieu luot mua nhat trong list nay")).thenReturn(
                new IntentClassifierService.IntentScore(IntentClassifierService.SEARCH_PRODUCT, 0.88d));

        ChatRequest request = new ChatRequest();
        request.setSessionId("session-3");
        request.setMessage("san pham nao nhieu luot mua nhat trong list nay");

        ChatResponse response = service.chat(request, "user-3", "trace-3");

        assertTrue(VietnameseNormalizer.normalize(response.getReply()).contains("chua co so luot mua chinh xac"));
        assertTrue(VietnameseNormalizer.normalize(response.getReply()).contains("ao so mi lung phoi dang ten"));
    }

    @Test
    void shouldAnswerComparisonQuestionFromCurrentSuggestionList() {
        ChatSession.PreferenceProfile profile = ChatSession.PreferenceProfile.empty();
        ChatSession session = ChatSession.builder()
                .sessionId("session-4")
                .userId("user-4")
                .messages(new ArrayList<>(java.util.List.of(
                        ChatSession.ChatMessage.builder()
                                .sender(ChatSession.Sender.BOT)
                                .content("Day la list gan nhat")
                                .suggestions(new ArrayList<>(java.util.List.of(
                                        ChatSession.ProductSuggestionSnapshot.builder()
                                                .productId("P1")
                                                .name("Ao so mi lung phoi dang ten")
                                                .category("Ao so mi")
                                                .price("1199000")
                                                .availableSizes(java.util.List.of("S", "M"))
                                                .build(),
                                        ChatSession.ProductSuggestionSnapshot.builder()
                                                .productId("P2")
                                                .name("Ao so mi soc tay phong")
                                                .category("Ao so mi")
                                                .price("1399000")
                                                .availableSizes(java.util.List.of("S"))
                                                .build())))
                                .build())))
                .preferenceProfile(profile)
                .build();
        when(sessionRepository.findBySessionId("session-4")).thenReturn(Optional.of(session));
        when(intentClassifierService.classify("nen chon mau nao an toan hon trong list nay")).thenReturn(
                new IntentClassifierService.IntentScore(IntentClassifierService.SEARCH_PRODUCT, 0.9d));

        ChatRequest request = new ChatRequest();
        request.setSessionId("session-4");
        request.setMessage("nen chon mau nao an toan hon trong list nay");

        ChatResponse response = service.chat(request, "user-4", "trace-4");

        assertTrue(VietnameseNormalizer.normalize(response.getReply()).contains("an toan nhat"));
        assertTrue(VietnameseNormalizer.normalize(response.getReply()).contains("ao so mi lung phoi dang ten"));
    }

    @Test
    void shouldFallbackWithoutClearingMemoryOnGenericAgentFailure() {
        when(intentClassifierService.classify("Kiem tra don ORD-123")).thenReturn(
                new IntentClassifierService.IntentScore(IntentClassifierService.CHECK_ORDER, 0.95d));
        when(fashionAgent.chat(anyString(), anyString()))
                .thenThrow(new RuntimeException("temporary timeout"))
                .thenThrow(new RuntimeException("temporary timeout"));

        ChatRequest request = new ChatRequest();
        request.setSessionId("session-5");
        request.setMessage("Kiem tra don ORD-123");

        ChatResponse response = service.chat(request, null, "trace-5");

        assertTrue(VietnameseNormalizer.normalize(response.getReply()).contains("dang nhap"));
    }
}
