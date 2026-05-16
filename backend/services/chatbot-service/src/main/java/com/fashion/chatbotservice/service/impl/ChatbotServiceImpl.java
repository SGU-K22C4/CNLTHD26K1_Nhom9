package com.fashion.chatbotservice.service.impl;

import com.fashion.chatbotservice.agent.FashionAgent;
import com.fashion.chatbotservice.agent.FashionTools;
import com.fashion.chatbotservice.agent.ResponseAssembler;
import com.fashion.chatbotservice.agent.ResponseGuardrail;
import com.fashion.chatbotservice.agent.ToolResultCollector;
import com.fashion.chatbotservice.config.AgentConfig;
import com.fashion.chatbotservice.dto.ChatRequest;
import com.fashion.chatbotservice.dto.ChatResponse;
import com.fashion.chatbotservice.dto.SessionResponse;
import com.fashion.chatbotservice.model.ChatSession;
import com.fashion.chatbotservice.repository.ChatSessionRepository;
import com.fashion.chatbotservice.service.ChatbotService;
import com.fashion.chatbotservice.service.ChatAnalyticsService;
import com.fashion.chatbotservice.service.IntentClassifierService;
import com.fashion.chatbotservice.service.ProfileEnrichmentService;
import com.fashion.chatbotservice.service.SizeAdvisorService;
import com.fashion.chatbotservice.service.SizeFitAdvisoryService;
import com.fashion.chatbotservice.util.VietnameseNormalizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Orchestrator chính: nhận request → enrich profile → gọi agent → assemble response → persist.
 * Hỗ trợ feature flag để chuyển đổi giữa agent mode và heuristic fallback.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChatbotServiceImpl implements ChatbotService {

    private static final String FLOW_PRODUCT_DISCOVERY = "PRODUCT_DISCOVERY";
    private static final String FLOW_SIZE_COLLECTION = "SIZE_COLLECTION";
    private static final String FLOW_WISHLIST_REVIEW = "WISHLIST_REVIEW";
    private static final String FLOW_LOYALTY_REVIEW = "LOYALTY_REVIEW";
    private static final String PENDING_PRODUCT_TYPE = "PRODUCT_TYPE";
    private static final String PENDING_SIZE = "SIZE";
    private static final String PENDING_MEASUREMENTS = "MEASUREMENTS";
    private static final String PENDING_BUDGET = "BUDGET";
    private static final String PENDING_GENDER_CONTEXT = "GENDER_CONTEXT";
    private static final long CONVERSATION_STATE_TTL_MINUTES = 20L;

    private final FashionAgent fashionAgent;
    private final FashionTools fashionTools;
    private final AgentConfig agentConfig;
    private final ChatSessionRepository chatSessionRepository;
    private final IntentClassifierService intentClassifierService;
    private final ProfileEnrichmentService profileEnrichmentService;
    private final ChatAnalyticsService chatAnalyticsService;
    private final SizeAdvisorService sizeAdvisorService;
    private final SizeFitAdvisoryService sizeFitAdvisoryService;
    private final ResponseGuardrail responseGuardrail;
    private final WebClient webClient;

        private static final Set<String> GENERIC_DESCRIPTORS = Set.of(
            "trang", "den", "do", "xanh", "hong", "vang", "nau", "be", "kem", "xam", "ghi", "bac",
            "navy", "gray", "black", "white", "red", "blue", "pink",
            "nam", "nu", "unisex", "cotton", "kaki", "jean", "denim",
            "tay", "dai", "short", "oversize", "form", "slim", "regular"
        );

    @Value("${chatbot.use-agent:true}")
    private boolean useAgent;

    @Value("${chatbot.product-service-url:http://localhost:8080}")
    private String productServiceUrl;

    @Value("${chatbot.promotion-service-url:http://localhost:8080}")
    private String promotionServiceUrl;

    @Value("${chatbot.order-service-url:http://localhost:8080}")
    private String orderServiceUrl;

    @Value("${chatbot.cart-service-url:http://localhost:8080}")
    private String cartServiceUrl;

    @PostConstruct
    public void bootstrapTrainingData() {
        try {
            intentClassifierService.bootstrapDefaultIntentsIfNeeded();
        } catch (Exception ex) {
            log.warn("Skip bootstrap training data at startup: {}", ex.getMessage());
        }
    }

    @Override
    public ChatResponse chat(ChatRequest request, String userIdHeader, String traceId) {
        long startTime = System.currentTimeMillis();

        if (request == null || request.getMessage() == null || request.getMessage().isBlank()) {
            throw new IllegalArgumentException("Nội dung tin nhắn không được để trống");
        }

        String sessionId = Optional.ofNullable(request.getSessionId())
                .filter(s -> !s.isBlank())
                .orElseGet(() -> UUID.randomUUID().toString());

        String userId = resolveUserId(userIdHeader, sessionId);
        ChatSession session = findOrCreateSession(sessionId, userId);
        mergePreferences(session, request.getPreferences());
        ToolResultCollector collector = new ToolResultCollector();

        // ============ COLD START DETECTION ============
        // Nếu đây là câu đầu tiên trong phiên (session.messages == null hoặc rỗng) → đây là Cold Start
        boolean isColdStart = (request.getColdStart() != null && request.getColdStart()) 
                || session.getMessages() == null 
                || session.getMessages().isEmpty();

        // Nếu Cold Start + yêu cầu là generic (chỉ nói "áo", "quần", "váy") → hỏi chi tiết loại sản phẩm trước
        if (isColdStart) {
            ChatResponse coldStartResponse = handleColdStart(sessionId, request.getMessage(), session);
            if (coldStartResponse != null) {
                coldStartResponse = applyFinalGuardrail(coldStartResponse, collectorFromResponse(coldStartResponse, collector));
                persistMessages(session, request.getMessage(), coldStartResponse);
                return coldStartResponse;
            }
        }

        // Enrich profile từ message + purchase history + wishlist + user profile
        profileEnrichmentService.enrichFromMessage(session.getPreferenceProfile(), request.getMessage());
        profileEnrichmentService.enrichFromPurchaseHistory(session.getPreferenceProfile(), userId);
        profileEnrichmentService.enrichFromWishlist(session.getPreferenceProfile(), userId);
        profileEnrichmentService.enrichFromUserProfile(session.getPreferenceProfile(), userId);
        updateBudgetFromMessage(session.getPreferenceProfile(), request.getMessage());
        refreshProductQueryContext(session.getPreferenceProfile(), request.getMessage());

        ChatResponse genderContextResponse = handleGenderContextStep(sessionId, request.getMessage(), session);
        if (genderContextResponse != null) {
            genderContextResponse = applyFinalGuardrail(genderContextResponse, collectorFromResponse(genderContextResponse, collector));
            persistMessages(session, request.getMessage(), genderContextResponse);
            return genderContextResponse;
        }

        // === Task 2: Out-of-Domain Short-Circuit ===
        // Classify intent BEFORE calling LLM to block off-topic/injection queries
        IntentClassifierService.IntentScore preCheck = applyConversationStateOverride(
                session.getPreferenceProfile(),
                request.getMessage(),
                intentClassifierService.classify(request.getMessage()));
        if (IntentClassifierService.OUT_OF_DOMAIN.equals(preCheck.intent())) {
            log.info("Out-of-domain query blocked (no LLM call): {}", request.getMessage());
            ChatResponse oodResponse = ChatResponse.builder()
                    .sessionId(sessionId)
                    .intent(IntentClassifierService.OUT_OF_DOMAIN)
                    .confidence(preCheck.confidence())
                    .reply("Em là trợ lý thời trang của Fashion Store, em chỉ hỗ trợ các vấn đề về sản phẩm, "
                            + "tư vấn phối đồ và đơn hàng thôi ạ. 😊")
                    .suggestions(List.of())
                    .promotions(List.of())
                    .profile(session.getPreferenceProfile())
                    .createdAt(Instant.now())
                    .build();
            oodResponse = applyFinalGuardrail(oodResponse, collectorFromResponse(oodResponse, collector));
            persistMessages(session, request.getMessage(), oodResponse);
            return oodResponse;
        }

        ChatResponse directIntentResponse = handleDirectIntent(
                sessionId, request.getMessage(), session, collector, preCheck.intent(), preCheck.confidence());
        if (directIntentResponse != null) {
            directIntentResponse = applyFinalGuardrail(directIntentResponse, collector);
            persistMessages(session, request.getMessage(), directIntentResponse);

            long totalLatency = System.currentTimeMillis() - startTime;
            chatAnalyticsService.record(traceId, sessionId, userId,
                    request.getMessage(), directIntentResponse, collector, totalLatency);
            return directIntentResponse;
        }

        // === Deictic reference handling ("áo này", "mẫu này") ===
        ChatResponse deicticResponse = handleDeicticReference(sessionId, request.getMessage(), session, collector);
        if (deicticResponse != null) {
            deicticResponse = applyFinalGuardrail(deicticResponse, collector);
            persistMessages(session, request.getMessage(), deicticResponse);

            long totalLatency = System.currentTimeMillis() - startTime;
            chatAnalyticsService.record(traceId, sessionId, userId,
                    request.getMessage(), deicticResponse, collector, totalLatency);
            return deicticResponse;
        }

        // === Explicit product check handling (strict search) ===
        ChatResponse explicitProductResponse = handleExplicitProductLookup(sessionId, request.getMessage(), session, collector);
        if (explicitProductResponse != null) {
            explicitProductResponse = applyFinalGuardrail(explicitProductResponse, collector);
            persistMessages(session, request.getMessage(), explicitProductResponse);

            long totalLatency = System.currentTimeMillis() - startTime;
            chatAnalyticsService.record(traceId, sessionId, userId,
                    request.getMessage(), explicitProductResponse, collector, totalLatency);
            return explicitProductResponse;
        }

        // Execute Agentic AI — single path, no heuristic fallback
        ChatResponse response = executeAgent(sessionId, request.getMessage(), session, collector);
        response = applyFinalGuardrail(response, collector);

        // Persist messages to MongoDB
        persistMessages(session, request.getMessage(), response);

        // Analytics
        long totalLatency = System.currentTimeMillis() - startTime;
        chatAnalyticsService.record(traceId, sessionId, userId,
                request.getMessage(), response, collector, totalLatency);

        return response;
    }

    private ChatResponse handleDirectIntent(String sessionId, String message,
                                            ChatSession session, ToolResultCollector collector,
                                            String intent, double confidence) {
        if (IntentClassifierService.WISHLIST_RECOMMENDATION.equals(intent)) {
            if (isGuestUser(session.getUserId())) {
                return buildLoginRequiredResponse(sessionId, intent, confidence, collector, session,
                        "Mình cần bạn đăng nhập để mở wishlist và gợi ý đúng các mẫu bạn đã lưu nhé.");
            }
            try {
                fashionTools.setCollector(collector);
                fashionTools.setPreferenceProfile(session.getPreferenceProfile());
                fashionTools.setCurrentUserId(session.getUserId());
                String reply = fashionTools.getWishlistRecommendations(session.getUserId());
                return ResponseAssembler.build(sessionId, reply, collector, session.getPreferenceProfile());
            } catch (Exception ex) {
                log.warn("Direct wishlist handling failed: {}", ex.getMessage());
                return ChatResponse.builder()
                        .sessionId(sessionId)
                        .intent(intent)
                        .confidence(confidence)
                        .reply("Mình chưa thể mở wishlist lúc này. Bạn thử lại sau ít phút nhé.")
                        .suggestions(collector.getProducts())
                        .promotions(collector.getPromotions())
                        .profile(session.getPreferenceProfile())
                        .createdAt(Instant.now())
                        .build();
            } finally {
                fashionTools.clearCollector();
            }
        }

        if (IntentClassifierService.LOYALTY_BENEFIT.equals(intent)) {
            if (isGuestUser(session.getUserId())) {
                return buildLoginRequiredResponse(sessionId, intent, confidence, collector, session,
                        "Mình cần bạn đăng nhập để kiểm tra điểm thưởng và quyền lợi thành viên chính xác nhé.");
            }
            try {
                fashionTools.setCollector(collector);
                fashionTools.setPreferenceProfile(session.getPreferenceProfile());
                fashionTools.setCurrentUserId(session.getUserId());
                String reply = fashionTools.getLoyaltyBenefits(session.getUserId());
                return ResponseAssembler.build(sessionId, reply, collector, session.getPreferenceProfile());
            } catch (Exception ex) {
                log.warn("Direct loyalty handling failed: {}", ex.getMessage());
                return ChatResponse.builder()
                        .sessionId(sessionId)
                        .intent(intent)
                        .confidence(confidence)
                        .reply("Mình chưa thể kiểm tra điểm thưởng lúc này. Bạn thử lại sau ít phút nhé.")
                        .suggestions(collector.getProducts())
                        .promotions(collector.getPromotions())
                        .profile(session.getPreferenceProfile())
                        .createdAt(Instant.now())
                        .build();
            } finally {
                fashionTools.clearCollector();
            }
        }

        if (IntentClassifierService.CONSULT_SIZE.equals(intent) && isStrongDirectSizeIntent(message)) {
            return buildSizeConsultationResponse(sessionId, message, session, collector, intent, confidence);
        }

        return null;
    }

    @Override
    public SessionResponse getSession(String sessionId) {
        ChatSession session;
        try {
            session = chatSessionRepository.findBySessionId(sessionId).orElse(null);
        } catch (Exception ex) {
            throw new IllegalStateException("MongoDB chưa kết nối, chưa thể lấy lịch sử chat");
        }

        if (session == null) {
            // Session may be generated on client side before first chat message is persisted.
            // Return an empty payload to avoid noisy 404 errors on initial widget load.
            return SessionResponse.builder()
                    .sessionId(sessionId)
                    .messages(List.of())
                    .build();
        }

        return SessionResponse.builder()
                .sessionId(session.getSessionId())
                .userId(session.getUserId())
                .messages(session.getMessages())
                .profile(session.getPreferenceProfile())
                .build();
    }

    private IntentClassifierService.IntentScore applyConversationStateOverride(
            ChatSession.PreferenceProfile profile,
            String message,
            IntentClassifierService.IntentScore original) {
        if (profile == null || original == null || !hasActiveConversationState(profile)) {
            return original;
        }

        String normalized = VietnameseNormalizer.normalize(message);
        if (IntentClassifierService.OUT_OF_DOMAIN.equals(original.intent())
                || IntentClassifierService.CHECK_ORDER.equals(original.intent())
                || IntentClassifierService.ASK_POLICY.equals(original.intent())
                || IntentClassifierService.ASK_PROMOTION.equals(original.intent())
                || IntentClassifierService.WISHLIST_RECOMMENDATION.equals(original.intent())
                || IntentClassifierService.LOYALTY_BENEFIT.equals(original.intent())) {
            return original;
        }

        if (FLOW_PRODUCT_DISCOVERY.equals(profile.getConversationFlow())) {
            if (PENDING_MEASUREMENTS.equals(profile.getPendingQuestionType())
                    && (containsMeasurementData(normalized) || isSizeFitFollowUp(normalized))) {
                return new IntentClassifierService.IntentScore(IntentClassifierService.CONSULT_SIZE,
                        Math.max(original.confidence(), 0.93d));
            }
            if (isRefinementFollowUp(message, normalized)) {
                return new IntentClassifierService.IntentScore(IntentClassifierService.SEARCH_PRODUCT,
                        Math.max(original.confidence(), 0.9d));
            }
        }

        if (FLOW_SIZE_COLLECTION.equals(profile.getConversationFlow())
                && (containsMeasurementData(normalized) || isSizeFitFollowUp(normalized))) {
            return new IntentClassifierService.IntentScore(IntentClassifierService.CONSULT_SIZE,
                    Math.max(original.confidence(), 0.94d));
        }

        return original;
    }

    /**
     * Chế độ Agent: LLM tự quyết định gọi tool nào.
     * Safety net: nếu Agent trả về rỗng nhưng message rõ ràng là tìm SP → fallback heuristic.
     */
    private ChatResponse executeAgent(String sessionId, String message,
                                       ChatSession session, ToolResultCollector collector) {
        try {
            fashionTools.setCollector(collector);
            fashionTools.setPreferenceProfile(session.getPreferenceProfile());
            fashionTools.setCurrentUserId(session.getUserId());

            // === Task 3: Inject active cart context into the message ===
            String cartContext = fetchActiveCartContext(session.getUserId());
            String clarificationContext = buildClarificationContext(session.getPreferenceProfile());
            String conversationStateContext = buildConversationStateContext(session.getPreferenceProfile());
            String profileContext = buildProfileContext(session.getPreferenceProfile());

            StringBuilder contextBuilder = new StringBuilder();
            if (!clarificationContext.isBlank()) {
                contextBuilder.append("[Clarification context] ").append(clarificationContext).append("\n");
            }
            if (!conversationStateContext.isBlank()) {
                contextBuilder.append("[Conversation state] ").append(conversationStateContext).append("\n");
            }
            if (!profileContext.isBlank()) {
                contextBuilder.append("[Ngữ cảnh cá nhân] ").append(profileContext).append("\n");
            }
            if (!cartContext.isBlank()) {
                contextBuilder.append("[Ngữ cảnh giỏ hàng] ").append(cartContext).append("\n");
            }

            String enrichedMessage = contextBuilder.isEmpty()
                    ? message
                    : contextBuilder.append("\n").append(message).toString();

            log.info("Gọi Agent cho session: {}, message length: {}", sessionId, enrichedMessage.length());
            String llmReply = invokeAgentWithRetry(sessionId, enrichedMessage, session);
            
            if (llmReply == null || llmReply.isBlank()) {
                log.warn("Agent liên tục lỗi cho session: {}. Chuyển sang chế độ Heuristic Fallback.", sessionId);
                ChatResponse fallbackResponse = executeHeuristicFallback(sessionId, message, session, collector);
                return fallbackResponse;
            }

            // Layer 3: Backend Guardrail — validate LLM output against real data
            return ResponseAssembler.build(sessionId, llmReply, collector, session.getPreferenceProfile());
        } catch (Throwable ex) {
            log.error("LỖI NGHIÊM TRỌNG khi thực thi Agent cho session: {}. Type: {}. Chi tiết: {}",
                    sessionId, ex.getClass().getSimpleName(), ex.getMessage(), ex);
            String errorReply = "Mình gặp chút trục trặc khi xử lý thông tin. Bạn thử nhắn lại sau vài giây nhé! 🙏";
            return ResponseAssembler.build(sessionId, errorReply, collector, session.getPreferenceProfile());
        } finally {
            fashionTools.clearCollector();
        }
    }

    /**
     * Retry agent call once on NullPointerException from LangChain4j.
     * NPE occurs when DeepSeek returns tool_call with null content → corrupts ChatMemory.
     * Fix: clear corrupted memory before retry so LangChain4j starts with a clean state.
     */
    private String invokeAgentWithRetry(String sessionId, String message, ChatSession session) {
        try {
            return fashionAgent.chat(sessionId, message);
        } catch (NullPointerException npe) {
            log.warn("Agent NPE for session {}, clearing corrupted memory and retrying. Cause: {}",
                    sessionId, npe.getMessage());
            // Retry with reduced context rebuilt from persisted state so one malformed
            // tool call does not wipe the whole conversation flow.
            agentConfig.clearSessionMemory(sessionId);
            try {
                Thread.sleep(300);
                return fashionAgent.chat(sessionId, buildRecoveryRetryMessage(session, message));
            } catch (Throwable retryEx) {
                log.error("Agent retry failed for session {} after memory clear: {}", sessionId, retryEx.getMessage());
                return null; // Return null → will be caught upstream as empty reply
            }
        }
    }

    /**
     * Chế độ Heuristic fallback: dùng IntentClassifier khi agent lỗi hoặc bị disable.
     * Giờ đây sẽ gọi Tool thật thay vì trả câu tĩnh.
     */
    private ChatResponse executeHeuristicFallback(String sessionId, String message,
                                                   ChatSession session, ToolResultCollector collector) {
        IntentClassifierService.IntentScore intentScore = intentClassifierService.classify(message);
        ChatSession.PreferenceProfile profile = session.getPreferenceProfile();

        String reply = switch (intentScore.intent()) {

            case IntentClassifierService.CONSULT_SIZE -> {
                // Trích xuất số đo mới từ tin nhắn hiện tại
                SizeAdvisorService.Measurements extracted = sizeAdvisorService.extractMeasurements(message);

                // Merge với số đo đã lưu trong session (context memory)
                Integer heightCm = extracted.heightCm() != null ? extracted.heightCm() : profile.getLastHeightCm();
                Integer weightKg = extracted.weightKg() != null ? extracted.weightKg() : profile.getLastWeightKg();
                Integer chestCm  = extracted.chestCm()  != null ? extracted.chestCm()  : profile.getLastChestCm();
                Integer waistCm  = extracted.waistCm()  != null ? extracted.waistCm()  : profile.getLastWaistCm();
                Integer hipCm    = extracted.hipCm()    != null ? extracted.hipCm()    : profile.getLastHipCm();

                SizeAdvisorService.Measurements merged = new SizeAdvisorService.Measurements(
                        heightCm, weightKg, chestCm, waistCm, hipCm);

                if (!merged.hasMinimumData()) {
                    collector.addMissingFields(merged.missingFields());
                    yield "Để tư vấn size chính xác, bạn cung cấp thêm: " + String.join(", ", merged.missingFields());
                }

                // Lưu số đo vào session profile để dùng cho câu tiếp theo
                profile.setLastHeightCm(heightCm);
                profile.setLastWeightKg(weightKg);
                profile.setLastChestCm(chestCm);
                profile.setLastWaistCm(waistCm);
                profile.setLastHipCm(hipCm);

                String garmentKeyword = extractGarmentKeyword(message);
                if ((garmentKeyword == null || garmentKeyword.isBlank())
                        && profile.getLastProductCategoryQueried() != null
                        && !profile.getLastProductCategoryQueried().isBlank()) {
                    garmentKeyword = profile.getLastProductCategoryQueried();
                }
                SizeAdvisorService.GarmentType type = sizeAdvisorService.detectGarmentType(message);
                SizeFitAdvisoryService.SizeFitAdvice advice =
                        sizeFitAdvisoryService.advise(merged, type, garmentKeyword == null ? message : garmentKeyword, profile);
                collector.setSizeRecommendation(advice.recommendedSize());
                SizeAdvisorService.SizeResult result =
                        new SizeAdvisorService.SizeResult(advice.recommendedSize(), advice.rationale());

                // Tư vấn size xong → tự động tìm sản phẩm phù hợp
                if (garmentKeyword != null && !garmentKeyword.isBlank()) {
                    try {
                        fashionTools.setCollector(collector);
                        fashionTools.setPreferenceProfile(profile);
                        fashionTools.setCurrentUserId(session.getUserId());
                        Double maxPrice = parseBudget(profile.getBudget());
                        fashionTools.searchProducts(garmentKeyword, null, maxPrice != null ? maxPrice.longValue() : null, null, null);
                    } catch (Exception ex) {
                        log.debug("Auto product search after size advice skipped: {}", ex.getMessage());
                    } finally {
                        fashionTools.clearCollector();
                    }
                }

                StringBuilder sizeReply = new StringBuilder();
                sizeReply.append("Với chiều cao ").append(heightCm).append("cm và cân nặng ").append(weightKg)
                        .append("kg, mình gợi ý bạn chọn **size ").append(result.recommendedSize()).append("**. ")
                        .append(result.note());
                if (advice.followUpPrompt() != null && !advice.followUpPrompt().isBlank()) {
                    sizeReply.append("\n\n").append(advice.followUpPrompt());
                }
                if (!collector.getProducts().isEmpty()) {
                    sizeReply.append("\n\nMình cũng tìm thấy một số sản phẩm phù hợp cho bạn:");
                }
                yield sizeReply.toString();
            }

            case IntentClassifierService.ASK_POLICY -> {
                // Gọi Knowledge Base để trả lời câu hỏi chính sách, FAQ
                try {
                    fashionTools.setCollector(collector);
                    fashionTools.setPreferenceProfile(profile);
                    fashionTools.setCurrentUserId(session.getUserId());
                    String knowledgeResult = fashionTools.searchKnowledge(message);
                    yield knowledgeResult;
                } catch (Exception ex) {
                    log.warn("Heuristic knowledge search failed: {}", ex.getMessage());
                    yield "Mình chưa tìm thấy thông tin về chính sách này. Bạn có thể liên hệ CSKH qua hotline hoặc email để được hỗ trợ trực tiếp nhé!";
                } finally {
                    fashionTools.clearCollector();
                }
            }

            case IntentClassifierService.ASK_PROMOTION -> {
                // Gọi Tool thật để lấy khuyến mãi đang hiệu lực
                try {
                    fashionTools.setCollector(collector);
                    fashionTools.setPreferenceProfile(profile);
                    fashionTools.setCurrentUserId(session.getUserId());
                    String toolResult = fashionTools.getActivePromotions();
                    yield toolResult;
                } catch (Exception ex) {
                    log.warn("Heuristic promotion lookup failed: {}", ex.getMessage());
                    yield "Mình chưa thể kiểm tra khuyến mãi lúc này. Bạn thử lại sau nhé!";
                } finally {
                    fashionTools.clearCollector();
                }
            }

            case IntentClassifierService.SEARCH_PRODUCT -> {
                // Gọi Tool thật để tìm sản phẩm — áp dụng budget từ message HOẶC Personalization panel
                try {
                    String searchKeyword = resolveSearchKeywordForTurn(message, profile);
                    if (shouldListProductTypes(message, searchKeyword)) {
                        fashionTools.setCollector(collector);
                        fashionTools.setPreferenceProfile(profile);
                        String groupHint = deriveProductTypeGroupHint(searchKeyword, message);
                        String typeResult = fashionTools.listProductTypes(groupHint);
                        yield typeResult;
                    }
                    // Ưu tiên parse giá từ chính câu message (VD: "quần jean từ 300 đến 500k")
                    Double[] priceRange = parsePriceRangeFromMessage(message);
                    Double minPrice = priceRange[0];
                    Double maxPrice = priceRange[1];
                    // Nếu message không có giá, fallback về budget trong Personalization panel
                    if (maxPrice == null) {
                        maxPrice = parseBudget(profile.getBudget());
                    }
                    boolean hasPriceFilter = (minPrice != null || maxPrice != null);

                    fashionTools.setCollector(collector);
                    fashionTools.setPreferenceProfile(profile);
                    String sizeFilter = extractSizeFilter(message);
                    String colorFilter = extractColorFilterForTurn(message, profile);

                    if (shouldBrowseProducts(message)) {
                        String browseResult = fashionTools.browseProducts(
                                minPrice != null ? minPrice.longValue() : null,
                                maxPrice != null ? maxPrice.longValue() : null,
                                colorFilter,
                                sizeFilter);
                        yield browseResult;
                    }

                    String toolResult = fashionTools.searchProducts(searchKeyword,
                            minPrice != null ? minPrice.longValue() : null,
                            maxPrice != null ? maxPrice.longValue() : null,
                            colorFilter,
                            sizeFilter);

                    // Nếu có filter giá mà không tìm thấy → retry KHÔNG có giá để gợi ý sản phẩm gần nhất
                    if (collector.getProducts().isEmpty() && hasPriceFilter) {
                        log.info("No products found with price filter [{}-{}], retrying without price for keyword: {}",
                                minPrice, maxPrice, searchKeyword);
                        fashionTools.clearCollector();
                        fashionTools.setCollector(collector);
                        fashionTools.setPreferenceProfile(profile);
                        String fallbackResult = fashionTools.searchProducts(searchKeyword, null, null, colorFilter, sizeFilter);

                        if (!collector.getProducts().isEmpty()) {
                            String priceNote = formatPriceRange(minPrice, maxPrice);
                            yield "Hiện chưa có sản phẩm \"" + searchKeyword + "\" trong khoảng giá " + priceNote
                                    + ". Tuy nhiên, mình tìm thấy một số mẫu gần nhất cho bạn tham khảo:";
                        }
                        // Vẫn không tìm thấy gì → thông báo không có sản phẩm
                        yield "Mình chưa tìm thấy sản phẩm nào khớp với \"" + searchKeyword
                                + "\". Bạn thử tìm với từ khóa ngắn hơn (VD: 'áo thun', 'quần jean', 'váy') "
                                + "hoặc cho mình biết thêm chi tiết nhé!";
                    }

                    // Không tìm thấy (không có filter giá) → gợi ý keyword khác
                    if (collector.getProducts().isEmpty()) {
                        yield "Mình chưa tìm thấy sản phẩm nào khớp với \"" + searchKeyword
                                + "\". Bạn thử tìm với từ khóa ngắn hơn (VD: 'áo thun', 'quần jean', 'váy') "
                                + "hoặc cho mình biết thêm chi tiết nhé!";
                    }
                    yield toolResult;
                } catch (Exception ex) {
                    log.warn("Heuristic product search failed: {}", ex.getMessage());
                    yield "Mình chưa thể tìm sản phẩm lúc này. Bạn thử lại sau nhé!";
                } finally {
                    fashionTools.clearCollector();
                }
            }

            case IntentClassifierService.CONSULT_SEASON -> {
                // Gợi ý outfit + tự động tìm sản phẩm theo mùa
                try {
                    fashionTools.setCollector(collector);
                    fashionTools.setPreferenceProfile(profile);
                    if (shouldUseSalesGuidance(message)) {
                        fashionTools.searchSalesGuidance(message);
                    }
                    String outfitResult = fashionTools.suggestOutfit(
                            extractOccasion(message), extractStyle(message));
                    yield outfitResult;
                } catch (Exception ex) {
                    yield "Mình sẵn sàng gợi ý outfit! Bạn cho mình biết dịp cụ thể (đi làm, đi tiệc, du lịch...) nhé!";
                } finally {
                    fashionTools.clearCollector();
                }
            }

            case IntentClassifierService.CHECK_ORDER -> {
                if (isGuestUser(session.getUserId())) {
                    yield "Mình cần bạn đăng nhập để kiểm tra đơn hàng chính xác theo tài khoản nhé.";
                }
                // Kiểm tra đơn hàng — trích xuất mã đơn nếu có, nếu không thì hỏi lại
                String orderNumber = extractOrderNumber(message);
                if (orderNumber != null) {
                    try {
                        fashionTools.setCollector(collector);
                        fashionTools.setPreferenceProfile(profile);
                        String orderResult = fashionTools.checkOrderByNumber(orderNumber);
                        yield orderResult;
                    } catch (Exception ex) {
                        log.warn("Order check failed: {}", ex.getMessage());
                        yield "Mình chưa thể kiểm tra đơn hàng lúc này. Bạn thử lại sau nhé!";
                    } finally {
                        fashionTools.clearCollector();
                    }
                } else {
                    yield "Dạ, bạn vui lòng cung cấp mã đơn hàng để mình kiểm tra nhé! 📦\n"
                            + "VD: 'Kiểm tra đơn ORD-1713200000000'\n"
                            + "Bạn có thể tìm mã đơn trong email xác nhận hoặc trang Đơn hàng của tôi.";
                }
            }

            case IntentClassifierService.GREETING -> {
                // Chào hỏi thân thiện
                String lowerMsg = message.toLowerCase();
                if (lowerMsg.contains("cảm ơn") || lowerMsg.contains("cam on") || lowerMsg.contains("thank")) {
                    yield "Không có gì ạ! Nếu cần gì thêm, cứ hỏi mình nhé! 😊";
                } else if (lowerMsg.contains("tạm biệt") || lowerMsg.contains("bye")) {
                    yield "Hẹn gặp lại bạn nhé! Chúc bạn mua sắm vui vẻ! 👋✨";
                } else {
                    yield "Xin chào! 👋 Mình là trợ lý thời trang AI của Fashion Store.\n"
                            + "Mình có thể giúp bạn:\n"
                            + "👕 Tìm sản phẩm (VD: 'Tìm áo thun nam dưới 500k')\n"
                            + "📏 Tư vấn size (VD: 'Mình cao 1m70, nặng 65kg mặc size gì?')\n"
                            + "🎁 Kiểm tra khuyến mãi (VD: 'Có voucher nào không?')\n"
                            + "📦 Kiểm tra đơn hàng (VD: 'Kiểm tra đơn ORD-xxx')\n"
                            + "👗 Gợi ý outfit (VD: 'Gợi ý đồ đi tiệc')\n"
                            + "Bạn cần hỗ trợ gì nào? 😊";
                }
            }

            default -> {
                if (shouldUseSalesGuidance(message)) {
                    try {
                        fashionTools.setCollector(collector);
                        fashionTools.setPreferenceProfile(profile);
                        String salesReply = fashionTools.searchSalesGuidance(message);
                        if (salesReply != null && !salesReply.isBlank()) {
                            yield salesReply;
                        }
                    } catch (Exception ex) {
                        log.debug("Sales guidance fallback skipped: {}", ex.getMessage());
                    } finally {
                        fashionTools.clearCollector();
                    }
                }

                // Fallback: thử search product nếu message có keyword thời trang
                String keyword = resolveSearchKeywordForTurn(message, profile);
                if (keyword.length() >= 2) {
                    try {
                        fashionTools.setCollector(collector);
                        fashionTools.setPreferenceProfile(profile);
                        String colorFilter = extractColorFilterForTurn(message, profile);
                        String searchResult = fashionTools.searchProducts(keyword, null, null, colorFilter, null);
                        if (!collector.getProducts().isEmpty()) {
                            yield searchResult;
                        }
                    } catch (Exception ignored) {
                    } finally {
                        fashionTools.clearCollector();
                    }
                }
                yield "Mình là trợ lý thời trang AI, có thể giúp bạn:\n"
                        + "👕 Tìm sản phẩm (VD: 'Tìm áo sơ mi nam')\n"
                        + "📏 Tư vấn size (VD: 'Mình cao 1m70, nặng 65kg mặc size gì?')\n"
                        + "🎁 Kiểm tra khuyến mãi (VD: 'Có voucher nào không?')\n"
                        + "📦 Theo dõi đơn hàng (VD: 'Kiểm tra đơn ORD-xxx')\n"
                        + "👗 Gợi ý outfit (VD: 'Gợi ý đồ đi tiệc')";
            }
        };

        return ChatResponse.builder()
                .sessionId(sessionId)
                .intent(intentScore.intent())
                .confidence(intentScore.confidence())
                .reply(reply)
                .missingFields(collector.getMissingFields())
                .suggestions(collector.getProducts())
                .promotions(collector.getPromotions())
                .profile(session.getPreferenceProfile())
                .createdAt(Instant.now())
                .build();
    }

    private boolean shouldUseSalesGuidance(String message) {
        if (message == null || message.isBlank()) {
            return false;
        }
        String normalized = VietnameseNormalizer.normalize(message).toLowerCase();
        return normalized.contains("de mac")
                || normalized.contains("an toan")
                || normalized.contains("de phoi")
                || normalized.contains("phan van")
                || normalized.contains("gia hoi cao")
                || normalized.contains("gia cao")
                || normalized.contains("mem hon")
                || normalized.contains("de chot")
                || normalized.contains("nen chon")
                || normalized.contains("noi bat hon")
                || normalized.contains("di lam nen mac")
                || normalized.contains("di tiec nen mac")
                || normalized.contains("phoi the nao")
                || normalized.contains("combo nao hop");
    }

    /**
     * Handle Cold Start (khởi động lạnh):
     * - Nếu user nói generic ("áo", "quần", "váy") → hỏi chi tiết loại sản phẩm
     * - Nếu user nói specific ("áo thun", "quần jean") → không cần hỏi, xử lý bình thường
     * - Returns ChatResponse nếu Cold Start cần hỏi thêm, null nếu có thể xử lý bình thường
     */
    private ChatResponse handleColdStart(String sessionId, String message, ChatSession session) {
        if (message == null || message.isBlank()) return null;

        if (isStrongDirectSizeIntent(message)) {
            return null;
        }

        String searchKeyword = extractProductSearchKeyword(message);
        if (hasShoppingConstraints(message)) {
            return null;
        }
        
        // Nếu search keyword không phải generic (đã đủ cụ thể), không cần hỏi thêm
        if (!isGenericGarmentKeyword(searchKeyword)) {
            return null; // Xử lý bình thường
        }

        // Đây là generic keyword → hỏi chi tiết
        String garmentCategory = mapToCanonicalGarment(VietnameseNormalizer.normalize(searchKeyword));
        if (garmentCategory.isBlank()) {
            return null; // Không phải loại đồ, xử lý bình thường
        }

        String suggestions = buildProductTypeOptions(garmentCategory);
        String reply = "Dạ, bạn muốn " + garmentCategory + " gì ạ? " + suggestions;

        log.info("Cold start: asking for product type clarification for: {}", garmentCategory);
        
        return ChatResponse.builder()
                .sessionId(sessionId)
                .intent("COLD_START_CLARIFY")
                .confidence(0.95d)
                .reply(reply)
                .suggestions(List.of())
                .promotions(List.of())
                .profile(session.getPreferenceProfile())
                .createdAt(Instant.now())
                .build();
    }

    /**
     * Build product type options cho user chọn.
     * VD: garment="áo" → "áo thun, áo sơ mi, áo khoác, áo polo, áo hoodie, hay loại khác?"
     */
    private String buildProductTypeOptions(String garmentCategory) {
        String normalized = normalizeText(garmentCategory);

        if (normalized.contains("ao")) {
            return "áo thun, áo sơ mi, áo khoác, áo polo, áo hoodie, hay loại khác? 👕";
        } else if (normalized.contains("quan")) {
            return "quần jean, quần tây, quần short, hay quần dài? 👖";
        } else if (normalized.contains("vay") || normalized.contains("dam") || normalized.contains("chan vay")) {
            return "váy xòe, váy bút chì, chân váy, hay đầm liền thân? 👗";
        } else if (normalized.contains("giay")) {
            return "giày thể thao, giày lười, giày cao gót, hay loại khác?👟";
        } else if (normalized.contains("tui")) {
            return "túi tote, túi xách, túi đeo chéo, hay túi gữ? 👜";
        } else if (normalized.contains("non") || normalized.contains("mu")) {
            return "nón lưỡi, nón xô, mũ len, hay mũ beret? 🧢";
        }

        return "";
    }

    private boolean isStrongDirectSizeIntent(String message) {
        if (message == null || message.isBlank()) {
            return false;
        }
        String normalized = VietnameseNormalizer.normalize(message);
        boolean hasMeasurement = containsMeasurementData(normalized);
        boolean hasSizeLanguage = normalized.contains("size")
                || normalized.contains("so do")
                || normalized.contains("nen chon")
                || normalized.contains("size nao")
                || normalized.matches(".*\\b(xs|s|m|l|xl|xxl)\\s+hay\\s+(xs|s|m|l|xl|xxl)\\b.*");
        boolean hasGarment = !extractGarmentKeyword(message).isBlank()
                || containsAnyKeyword(normalized, "ao so mi", "ao thun", "ao khoac", "blazer", "quan jean", "quan tay", "vay", "dam");
        return (hasMeasurement || hasSizeLanguage) && hasGarment;
    }

    private ChatResponse buildSizeConsultationResponse(String sessionId, String message,
                                                       ChatSession session, ToolResultCollector collector,
                                                       String intent, double confidence) {
        return ChatResponse.builder()
                .sessionId(sessionId)
                .intent(intent)
                .confidence(confidence)
                .reply(buildSizeConsultationReply(message, session, collector))
                .missingFields(collector.getMissingFields())
                .suggestions(collector.getProducts())
                .promotions(collector.getPromotions())
                .profile(session.getPreferenceProfile())
                .createdAt(Instant.now())
                .build();
    }

    private String buildSizeConsultationReply(String message, ChatSession session, ToolResultCollector collector) {
        ChatSession.PreferenceProfile profile = session.getPreferenceProfile();
        SizeAdvisorService.Measurements extracted = sizeAdvisorService.extractMeasurements(message);

        Integer heightCm = extracted.heightCm() != null ? extracted.heightCm() : profile.getLastHeightCm();
        Integer weightKg = extracted.weightKg() != null ? extracted.weightKg() : profile.getLastWeightKg();
        Integer chestCm = extracted.chestCm() != null ? extracted.chestCm() : profile.getLastChestCm();
        Integer waistCm = extracted.waistCm() != null ? extracted.waistCm() : profile.getLastWaistCm();
        Integer hipCm = extracted.hipCm() != null ? extracted.hipCm() : profile.getLastHipCm();

        SizeAdvisorService.Measurements merged = new SizeAdvisorService.Measurements(
                heightCm, weightKg, chestCm, waistCm, hipCm);

        if (!merged.hasMinimumData()) {
            collector.addMissingFields(merged.missingFields());
            return "Để tư vấn size chính xác, bạn cung cấp thêm: " + String.join(", ", merged.missingFields());
        }

        profile.setLastHeightCm(heightCm);
        profile.setLastWeightKg(weightKg);
        profile.setLastChestCm(chestCm);
        profile.setLastWaistCm(waistCm);
        profile.setLastHipCm(hipCm);

        String garmentKeyword = extractGarmentKeyword(message);
        if ((garmentKeyword == null || garmentKeyword.isBlank())
                && profile.getLastProductCategoryQueried() != null
                && !profile.getLastProductCategoryQueried().isBlank()) {
            garmentKeyword = profile.getLastProductCategoryQueried();
        }

        SizeAdvisorService.GarmentType type = sizeAdvisorService.detectGarmentType(message);
        SizeFitAdvisoryService.SizeFitAdvice advice =
                sizeFitAdvisoryService.advise(merged, type, garmentKeyword == null ? message : garmentKeyword, profile);
        collector.setSizeRecommendation(advice.recommendedSize());

        if (garmentKeyword != null && !garmentKeyword.isBlank()) {
            try {
                fashionTools.setCollector(collector);
                fashionTools.setPreferenceProfile(profile);
                fashionTools.setCurrentUserId(session.getUserId());
                Double maxPrice = parseBudget(profile.getBudget());
                fashionTools.searchProducts(garmentKeyword, null, maxPrice != null ? maxPrice.longValue() : null, null, null);
            } catch (Exception ex) {
                log.debug("Auto product search after size advice skipped: {}", ex.getMessage());
            } finally {
                fashionTools.clearCollector();
            }
        }

        StringBuilder sizeReply = new StringBuilder();
        sizeReply.append("Với chiều cao ").append(heightCm).append("cm và cân nặng ").append(weightKg)
                .append("kg, mình gợi ý bạn chọn **size ").append(advice.recommendedSize()).append("**. ")
                .append(advice.rationale());
        if (advice.followUpPrompt() != null && !advice.followUpPrompt().isBlank()) {
            sizeReply.append("\n\n").append(advice.followUpPrompt());
        }
        if (!collector.getProducts().isEmpty()) {
            sizeReply.append("\n\nMình cũng tìm thấy một số sản phẩm phù hợp cho bạn:");
        }
        return sizeReply.toString();
    }

    private String normalizeText(String value) {
        return VietnameseNormalizer.normalize(value == null ? "" : value);
    }

    // ========== EXPLICIT PRODUCT CHECK / DEICTIC HANDLING ==========

    private ChatResponse handleDeicticReference(String sessionId,
                                                String message,
                                                ChatSession session,
                                                ToolResultCollector collector) {
        if (!isDeicticReference(message)) return null;

        List<ChatSession.ProductSuggestionSnapshot> recent = getLastSuggestedProducts(session);
        if (recent.isEmpty()) {
            return ChatResponse.builder()
                    .sessionId(sessionId)
                    .intent(IntentClassifierService.SEARCH_PRODUCT)
                    .confidence(0.82d)
                    .reply("Bạn đang nói đến mẫu nào ạ? Bạn cho mình biết tên hoặc mô tả thêm để mình kiểm tra chính xác nhé!")
                    .suggestions(List.of())
                    .promotions(List.of())
                    .profile(session.getPreferenceProfile())
                    .createdAt(Instant.now())
                    .build();
        }

        List<ChatResponse.ProductSuggestion> suggestions = mapSnapshots(recent);
        collector.addProducts(suggestions);

        if (suggestions.size() == 1) {
            ChatResponse.ProductSuggestion s = suggestions.get(0);
            String reply = buildSingleDeicticReply(s);
            return ChatResponse.builder()
                    .sessionId(sessionId)
                    .intent(IntentClassifierService.SEARCH_PRODUCT)
                    .confidence(0.9d)
                    .reply(reply)
                    .suggestions(List.of(s))
                    .promotions(List.of())
                    .profile(session.getPreferenceProfile())
                    .createdAt(Instant.now())
                    .build();
        }

        String nameList = suggestions.stream()
                .limit(3)
                .map(ChatResponse.ProductSuggestion::getName)
                .reduce((a, b) -> a + ", " + b)
                .orElse("mẫu gần đây");
        String moreNote = suggestions.size() > 3 ? "..." : "";

        return ChatResponse.builder()
                .sessionId(sessionId)
                .intent(IntentClassifierService.SEARCH_PRODUCT)
                .confidence(0.86d)
                .reply("Bạn đang nói đến mẫu nào trong danh sách gần đây: " + nameList + moreNote
                        + "? Bạn cho mình biết tên mẫu để mình kiểm tra chính xác nhé!")
                .suggestions(suggestions)
                .promotions(List.of())
                .profile(session.getPreferenceProfile())
                .createdAt(Instant.now())
                .build();
    }

    private ChatResponse handleExplicitProductLookup(String sessionId,
                                                     String message,
                                                     ChatSession session,
                                                     ToolResultCollector collector) {
        String normalized = VietnameseNormalizer.normalize(message == null ? "" : message);
        if (isRefinementFollowUp(message, normalized)
                && session != null
                && session.getPreferenceProfile() != null
                && session.getPreferenceProfile().getLastProductCategoryQueried() != null
                && !session.getPreferenceProfile().getLastProductCategoryQueried().isBlank()) {
            return null;
        }

        if (!isExplicitProductCheck(message)) return null;

        String searchKeyword = extractProductSearchKeyword(message);
        if (searchKeyword.isBlank()) {
            return ChatResponse.builder()
                    .sessionId(sessionId)
                    .intent(IntentClassifierService.SEARCH_PRODUCT)
                    .confidence(0.84d)
                    .reply("Bạn cho mình biết rõ tên/mẫu sản phẩm cần kiểm tra nhé. Ví dụ: 'Áo sơ mi Oxford'.")
                    .suggestions(List.of())
                    .promotions(List.of())
                    .profile(session.getPreferenceProfile())
                    .createdAt(Instant.now())
                    .build();
        }

        if (isGenericGarmentKeyword(searchKeyword)) {
            return null;
        }

        Double[] priceRange = parsePriceRangeFromMessage(message);
        Double minPrice = priceRange[0];
        Double maxPrice = priceRange[1];
        boolean wantsSimilar = wantsSimilarSuggestion(message);

        try {
            fashionTools.setCollector(collector);
            fashionTools.setPreferenceProfile(session.getPreferenceProfile());

            String strictResult = fashionTools.searchProductsStrict(
                    searchKeyword,
                    minPrice != null ? minPrice.longValue() : null,
                    maxPrice != null ? maxPrice.longValue() : null,
                    null,
                    extractSizeFilter(message));

            if (!collector.getProducts().isEmpty()) {
                return ChatResponse.builder()
                        .sessionId(sessionId)
                        .intent(IntentClassifierService.SEARCH_PRODUCT)
                        .confidence(0.92d)
                        .reply(strictResult)
                        .suggestions(collector.getProducts())
                        .promotions(List.of())
                        .profile(session.getPreferenceProfile())
                        .createdAt(Instant.now())
                        .build();
            }
        } catch (Exception ex) {
            log.warn("Strict product lookup failed: {}", ex.getMessage());
        } finally {
            fashionTools.clearCollector();
        }

        String priceNote = formatPriceRange(minPrice, maxPrice);
        String notFoundReply = priceNote.isBlank()
                ? "Hiện chưa có sản phẩm \"" + searchKeyword + "\" trong hệ thống."
                : "Hiện chưa có sản phẩm \"" + searchKeyword + "\" trong khoảng giá " + priceNote + ".";

        if (!wantsSimilar) {
            return ChatResponse.builder()
                    .sessionId(sessionId)
                    .intent(IntentClassifierService.SEARCH_PRODUCT)
                    .confidence(0.86d)
                    .reply(notFoundReply + " Bạn muốn mình gợi ý mẫu tương tự không ạ?")
                    .suggestions(List.of())
                    .promotions(List.of())
                    .profile(session.getPreferenceProfile())
                    .createdAt(Instant.now())
                    .build();
        }

        String relaxedKeyword = extractGarmentKeyword(message);
        if (relaxedKeyword == null || relaxedKeyword.isBlank()) {
            relaxedKeyword = searchKeyword;
        }

        ToolResultCollector relaxedCollector = new ToolResultCollector();
        try {
            fashionTools.setCollector(relaxedCollector);
            fashionTools.setPreferenceProfile(session.getPreferenceProfile());
            fashionTools.searchProducts(relaxedKeyword, null, null, null, extractSizeFilter(message));
        } catch (Exception ex) {
            log.warn("Relaxed product lookup failed: {}", ex.getMessage());
        } finally {
            fashionTools.clearCollector();
        }

        if (!relaxedCollector.getProducts().isEmpty()) {
            collector.addProducts(relaxedCollector.getProducts());
            return ChatResponse.builder()
                    .sessionId(sessionId)
                    .intent(IntentClassifierService.SEARCH_PRODUCT)
                    .confidence(0.88d)
                    .reply(notFoundReply + " Mình gợi ý một số mẫu tương tự để bạn tham khảo nhé:")
                    .suggestions(relaxedCollector.getProducts())
                    .promotions(List.of())
                    .profile(session.getPreferenceProfile())
                    .createdAt(Instant.now())
                    .build();
        }

        return ChatResponse.builder()
                .sessionId(sessionId)
                .intent(IntentClassifierService.SEARCH_PRODUCT)
                .confidence(0.84d)
                .reply(notFoundReply + " Bạn muốn mình tìm theo loại đồ khác không ạ?")
                .suggestions(List.of())
                .promotions(List.of())
                .profile(session.getPreferenceProfile())
                .createdAt(Instant.now())
                .build();
    }

    private List<ChatSession.ProductSuggestionSnapshot> getLastSuggestedProducts(ChatSession session) {
        if (session == null || session.getMessages() == null || session.getMessages().isEmpty()) {
            return List.of();
        }
        List<ChatSession.ChatMessage> messages = session.getMessages();
        for (int i = messages.size() - 1; i >= 0; i--) {
            ChatSession.ChatMessage msg = messages.get(i);
            if (msg == null || msg.getSender() != ChatSession.Sender.BOT) continue;
            if (msg.getSuggestions() != null && !msg.getSuggestions().isEmpty()) {
                return msg.getSuggestions();
            }
        }
        return List.of();
    }

    private List<ChatResponse.ProductSuggestion> mapSnapshots(List<ChatSession.ProductSuggestionSnapshot> snapshots) {
        List<ChatResponse.ProductSuggestion> results = new ArrayList<>();
        for (ChatSession.ProductSuggestionSnapshot snapshot : snapshots) {
            if (snapshot == null) continue;
            results.add(ChatResponse.ProductSuggestion.builder()
                    .productId(snapshot.getProductId())
                    .name(snapshot.getName())
                    .category(snapshot.getCategory())
                    .categoryGender(snapshot.getCategoryGender())
                    .imageUrl(snapshot.getImageUrl())
                    .link(snapshot.getLink())
                    .price(snapshot.getPrice())
                    .availableSizes(snapshot.getAvailableSizes() != null ? snapshot.getAvailableSizes() : List.of())
                    .availableColors(snapshot.getAvailableColors() != null ? snapshot.getAvailableColors() : List.of())
                    .reason("Từ lịch sử gợi ý gần nhất")
                    .build());
        }
        return results;
    }

    private String buildSingleDeicticReply(ChatResponse.ProductSuggestion suggestion) {
        StringBuilder reply = new StringBuilder();
        reply.append("Bạn đang hỏi mẫu \"").append(suggestion.getName()).append("\" đúng không ạ? ");

        if (suggestion.getAvailableSizes() != null && !suggestion.getAvailableSizes().isEmpty()) {
            reply.append("Mẫu này hiện có size ")
                    .append(String.join(", ", suggestion.getAvailableSizes()))
                    .append(". ");
        }

        if (suggestion.getAvailableColors() != null && !suggestion.getAvailableColors().isEmpty()) {
            reply.append("Màu sắc có sẵn: ")
                    .append(String.join(", ", suggestion.getAvailableColors()))
                    .append(". ");
        }

        if (suggestion.getPrice() != null && !suggestion.getPrice().isBlank()) {
            reply.append("Giá hiện tại: ").append(suggestion.getPrice()).append(". ");
        }

        reply.append("Bạn muốn mình gợi ý thêm mẫu tương tự không ạ?");
        return reply.toString();
    }

    private boolean isDeicticReference(String message) {
        if (message == null || message.isBlank()) return false;
        String normalized = VietnameseNormalizer.normalize(message);
        return normalized.contains("cai nay")
            || normalized.contains("mau nay")
            || normalized.contains("san pham nay")
            || normalized.contains("sp nay")
            || normalized.contains("mon nay")
            || normalized.matches(".*\\b(ao|quan|vay|dam)\\s+nay\\b.*");
    }

    private boolean isExplicitProductCheck(String message) {
        if (message == null || message.isBlank()) return false;
        String normalized = VietnameseNormalizer.normalize(message);
        String searchKeyword = extractProductSearchKeyword(message);

        boolean hasCheckPhrase = normalized.contains("co khong")
                || normalized.contains("con khong")
                || normalized.contains("co hang")
                || normalized.contains("con hang")
                || normalized.contains("ton kho")
                || normalized.contains("het hang")
                || normalized.contains("co ban");

        boolean hasSpecificCue = normalized.contains("san pham ")
                || normalized.contains("sp ")
                || normalized.contains("ten ")
                || normalized.contains("ma ");

        boolean hasQuote = message.contains("\"") || message.contains("'");
        boolean hasConcreteKeyword = searchKeyword != null
                && !searchKeyword.isBlank()
                && !isGenericGarmentKeyword(searchKeyword);
        boolean attributeOnlyRefinement = isAttributeOnlyRefinement(message, normalized);

        if (hasQuote) {
            return true;
        }

        // Nhánh explicit lookup chỉ dành cho việc kiểm tra một sản phẩm khá cụ thể.
        // Follow-up kiểu "màu đen không", "size S không", "dưới 1 triệu" phải đi
        // theo context sản phẩm đang tư vấn thay vì bị hiểu là tên sản phẩm mới.
        if (attributeOnlyRefinement) {
            return false;
        }

        return hasCheckPhrase && hasConcreteKeyword
                && (hasSpecificCue || !isRefinementFollowUp(message, normalized));
    }

    private boolean isAttributeOnlyRefinement(String message, String normalized) {
        if (message == null || message.isBlank()) {
            return false;
        }
        boolean hasColor = extractColorFilterForTurn(message, null) != null;
        boolean hasSize = extractSizeFilter(message) != null;
        boolean hasBudget = hasPriceSignal(normalized);
        boolean hasConcreteGarment = !extractGarmentKeyword(message).isBlank();
        boolean refinementFollowUp = isRefinementFollowUp(message, normalized);

        if (!refinementFollowUp) {
            return false;
        }

        if (hasColor || hasSize || hasBudget) {
            return !hasConcreteGarment || isGenericGarmentKeyword(extractProductSearchKeyword(message));
        }

        return false;
    }

    private boolean wantsSimilarSuggestion(String message) {
        if (message == null || message.isBlank()) return false;
        String normalized = VietnameseNormalizer.normalize(message);
        return normalized.contains("tuong tu")
                || normalized.contains("gan giong")
                || normalized.contains("thay the")
                || normalized.contains("goi y")
                || normalized.contains("mau khac")
                || normalized.contains("san pham khac");
    }

    private boolean isGenericGarmentKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) return false;
        String normalized = VietnameseNormalizer.normalize(keyword);
        String[] baseGarments = {
                "ao", "ao thun", "ao so mi", "ao khoac", "ao polo", "ao hoodie", "ao len",
                "quan", "quan jean", "quan tay", "quan short", "quan dai",
                "vay", "dam", "chan vay", "ao dai", "giay", "tui", "non"
        };

        for (String garment : baseGarments) {
            if (normalized.equals(garment)) return true;
            if (normalized.startsWith(garment + " ")) {
                String rest = normalized.substring(garment.length()).trim();
                if (rest.isBlank()) return true;
                String[] tokens = rest.split("\\s+");
                boolean descriptorOnly = true;
                for (String token : tokens) {
                    if (!GENERIC_DESCRIPTORS.contains(token)) {
                        descriptorOnly = false;
                        break;
                    }
                }
                if (descriptorOnly) return true;
            }
        }
        return false;
    }

    /**
     * Xây dựng context từ các yêu cầu Cold Start mà user đã làm rõ.
     * VD: User nói "áo" → chatbot hỏi "áo gì?" → user trả "áo thun"
     * → Profile lưu clarifiedProductTypes = {"áo thun"} → dùng trong câu tiếp theo
     */
    private String buildClarificationContext(ChatSession.PreferenceProfile profile) {
        if (profile == null || profile.getClarifiedProductTypes() == null || profile.getClarifiedProductTypes().isEmpty()) {
            return "";
        }
        
        List<String> types = new ArrayList<>(profile.getClarifiedProductTypes());
        String recent = types.get(types.size() - 1); // Lấy loại sản phẩm được làm rõ gần nhất
        
        if (recent == null || recent.isBlank()) {
            return "";
        }
        
        Instant queryTime = profile.getLastProductQueryTime();
        if (queryTime != null) {
            long elapsedMinutes = java.time.temporal.ChronoUnit.MINUTES.between(queryTime, Instant.now());
            // Nếu đã quá 10 phút, không dùng context cũ nữa (user có thể muốn hỏi cái khác)
            if (elapsedMinutes > 10) {
                return "";
            }
        }
        
        return "Bạn đang tìm " + recent + ". ";
    }

    private String buildConversationStateContext(ChatSession.PreferenceProfile profile) {
        if (!hasActiveConversationState(profile)) {
            return "";
        }
        List<String> parts = new ArrayList<>();
        parts.add("Flow hiện tại: " + profile.getConversationFlow());
        if (profile.getPendingQuestionType() != null && !profile.getPendingQuestionType().isBlank()) {
            parts.add("Đang chờ: " + profile.getPendingQuestionType());
        }
        if (profile.getLastProductCategoryQueried() != null && !profile.getLastProductCategoryQueried().isBlank()) {
            parts.add("Category đang theo: " + profile.getLastProductCategoryQueried());
        }
        return String.join("; ", parts);
    }

    private String buildProfileContext(ChatSession.PreferenceProfile profile) {
        if (profile == null) return "";
        List<String> parts = new ArrayList<>();

        if (profile.getPreferredSizes() != null && !profile.getPreferredSizes().isEmpty()) {
            parts.add("Size hay mặc: " + String.join(", ", profile.getPreferredSizes()));
        }
        if (profile.getPreferredColors() != null && !profile.getPreferredColors().isEmpty()) {
            parts.add("Màu yêu thích: " + String.join(", ", profile.getPreferredColors()));
        }
        if (profile.getPreferredCategories() != null && !profile.getPreferredCategories().isEmpty()) {
            parts.add("Loại đồ quan tâm: " + String.join(", ", profile.getPreferredCategories()));
        }
        if (profile.getStyle() != null && !profile.getStyle().isBlank()) {
            parts.add("Phong cách: " + profile.getStyle());
        }
        if (profile.getPreferredTone() != null && !profile.getPreferredTone().isBlank()) {
            parts.add("Cách xưng hô: " + profile.getPreferredTone());
        }
        if (profile.getBudget() != null && !profile.getBudget().isBlank()) {
            parts.add("Ngân sách: " + profile.getBudget());
        }
        if (profile.getFocusTags() != null && !profile.getFocusTags().isEmpty()) {
            parts.add("Ưu tiên: " + String.join(", ", profile.getFocusTags()));
        }

        if (profile.getPreferredOccasions() != null && !profile.getPreferredOccasions().isEmpty()) {
            parts.add("Dip mac uu tien: " + String.join(", ", profile.getPreferredOccasions()));
        }
        if (profile.getFitPreference() != null && !profile.getFitPreference().isBlank()) {
            parts.add("Fit ua thich: " + profile.getFitPreference());
        }
        if (profile.getCustomerPersona() != null && !profile.getCustomerPersona().isBlank()) {
            parts.add("Persona suy ra: " + profile.getCustomerPersona());
        }
        if (profile.getPriceComfortZone() != null && !profile.getPriceComfortZone().isBlank()) {
            parts.add("Vung gia thoai mai: " + profile.getPriceComfortZone());
        }
        if (profile.getTargetGender() != null && !profile.getTargetGender().isBlank()) {
            parts.add("Nhom gioi tinh dang chon: " + profile.getTargetGender());
        }

        String measurement = buildMeasurementContext(profile);
        if (!measurement.isBlank()) {
            parts.add(measurement);
        }

        return String.join("; ", parts).trim();
    }

    private String buildRecoveryRetryMessage(ChatSession session, String originalMessage) {
        StringBuilder retryContext = new StringBuilder();
        retryContext.append("[Recovery mode] Bám đúng ngữ cảnh hiện có, không hỏi lại thông tin user đã cung cấp.\n");

        if (session != null && session.getPreferenceProfile() != null) {
            String conversationStateContext = buildConversationStateContext(session.getPreferenceProfile());
            String profileContext = buildProfileContext(session.getPreferenceProfile());
            if (!conversationStateContext.isBlank()) {
                retryContext.append("[Conversation state] ").append(conversationStateContext).append("\n");
            }
            if (!profileContext.isBlank()) {
                retryContext.append("[Ngữ cảnh cá nhân] ").append(profileContext).append("\n");
            }
        }

        if (session != null && session.getMessages() != null && !session.getMessages().isEmpty()) {
            int start = Math.max(0, session.getMessages().size() - 4);
            retryContext.append("[Recent turns]\n");
            for (int i = start; i < session.getMessages().size(); i++) {
                ChatSession.ChatMessage item = session.getMessages().get(i);
                if (item == null || item.getContent() == null || item.getContent().isBlank()) {
                    continue;
                }
                String speaker = item.getSender() == ChatSession.Sender.USER ? "User" : "Bot";
                retryContext.append("- ").append(speaker).append(": ").append(item.getContent()).append("\n");
            }
        }

        retryContext.append("\n[Current user message]\n").append(originalMessage);
        return retryContext.toString();
    }

    private String buildMeasurementContext(ChatSession.PreferenceProfile profile) {
        List<String> items = new ArrayList<>();
        if (profile.getLastHeightCm() != null) items.add("cao " + profile.getLastHeightCm() + "cm");
        if (profile.getLastWeightKg() != null) items.add("nặng " + profile.getLastWeightKg() + "kg");
        if (profile.getLastChestCm() != null) items.add("ngực " + profile.getLastChestCm() + "cm");
        if (profile.getLastWaistCm() != null) items.add("eo " + profile.getLastWaistCm() + "cm");
        if (profile.getLastHipCm() != null) items.add("hông " + profile.getLastHipCm() + "cm");
        return items.isEmpty() ? "" : "Số đo gần nhất: " + String.join(", ", items);
    }

    private boolean hasActiveConversationState(ChatSession.PreferenceProfile profile) {
        if (profile == null || profile.getConversationFlow() == null || profile.getConversationFlow().isBlank()) {
            return false;
        }
        Instant updatedAt = profile.getConversationStateUpdatedAt();
        if (updatedAt == null) {
            return false;
        }
        long elapsedMinutes = java.time.temporal.ChronoUnit.MINUTES.between(updatedAt, Instant.now());
        return elapsedMinutes <= CONVERSATION_STATE_TTL_MINUTES;
    }

    private boolean containsMeasurementData(String normalizedMessage) {
        if (normalizedMessage == null || normalizedMessage.isBlank()) {
            return false;
        }
        return normalizedMessage.matches(".*\\d+\\s*(cm|kg|m\\d).*")
                || normalizedMessage.matches(".*\\b(1m\\d{2}|\\d{2,3}kg)\\b.*")
                || normalizedMessage.contains("vong nguc")
                || normalizedMessage.contains("vong eo")
                || normalizedMessage.contains("vong hong");
    }

    private boolean isRefinementFollowUp(String message, String normalizedMessage) {
        if (message == null || message.isBlank()) {
            return false;
        }
        boolean budgetFollowUp = isGenericBudgetOnlyFollowUp(message);
        boolean sizeFollowUp = extractSizeFilter(message) != null;
        boolean measurementFollowUp = containsMeasurementData(normalizedMessage);
        boolean fitFollowUp = isSizeFitFollowUp(normalizedMessage);
        boolean colorFollowUp = containsAnyKeyword(normalizedMessage,
                "den", "trang", "xanh", "do", "hong", "vang", "nau", "be", "xam", "ghi", "navy", "kem");
        boolean quickChoiceFollowUp = containsAnyKeyword(normalizedMessage,
                "mau nay", "mau kia", "loai nay", "loai kia", "cai nay", "cai kia",
                "phuong an nay", "phuong an kia", "vay thi", "the con", "neu vay", "ok vay");
        return budgetFollowUp || sizeFollowUp || measurementFollowUp || fitFollowUp || colorFollowUp || quickChoiceFollowUp;
    }

    private boolean isSizeFitFollowUp(String normalizedMessage) {
        if (normalizedMessage == null || normalizedMessage.isBlank()) {
            return false;
        }
        return normalizedMessage.matches(".*\\b(xs|s|m|l|xl|xxl)\\s*(hay|hoac|vs|voi)\\s*(xs|s|m|l|xl|xxl)\\b.*")
                || containsAnyKeyword(normalizedMessage,
                "phan van size", "size nao hop", "size nao on", "size nao dep",
                "om hay rong", "fit nao hop", "vai rong", "nguc day", "mong to", "dui to",
                "mac rong", "mac om", "vua van", "thoai mai hon");
    }

    private void refreshProductQueryContext(ChatSession.PreferenceProfile profile, String message) {
        if (profile == null || message == null || message.isBlank()) {
            return;
        }
        String inferredCategory = extractProductSearchKeyword(message);
        if (inferredCategory == null || inferredCategory.isBlank() || isGenericBudgetOnlyFollowUp(message)) {
            return;
        }
        if (isGenericGarmentKeyword(inferredCategory) || looksLikeConcreteProductCategory(inferredCategory)) {
            profile.setLastProductCategoryQueried(inferredCategory);
            profile.setLastProductQueryTime(Instant.now());
        }
    }

    private String resolveSearchKeywordForTurn(String message, ChatSession.PreferenceProfile profile) {
        String normalized = VietnameseNormalizer.normalize(message == null ? "" : message);
        if (isRefinementFollowUp(message, normalized)
                && profile != null
                && profile.getLastProductCategoryQueried() != null
                && !profile.getLastProductCategoryQueried().isBlank()) {
            return profile.getLastProductCategoryQueried();
        }
        String extracted = extractProductSearchKeyword(message);
        if (extracted != null && !extracted.isBlank() && !isGenericBudgetOnlyFollowUp(message)) {
            return extracted;
        }
        if (profile != null && profile.getLastProductCategoryQueried() != null && !profile.getLastProductCategoryQueried().isBlank()) {
            return profile.getLastProductCategoryQueried();
        }
        return extracted;
    }

    private String extractColorFilterForTurn(String message, ChatSession.PreferenceProfile profile) {
        if (message == null || message.isBlank()) {
            return null;
        }
        String normalized = VietnameseNormalizer.normalize(message);
        if (normalized.contains("mau den") || normalized.contains("den khong") || normalized.contains("tone den")) return "đen";
        if (normalized.contains("mau trang") || normalized.contains("trang khong") || normalized.contains("tone trang")) return "trắng";
        if (normalized.contains("mau xanh") || normalized.contains("xanh khong") || normalized.contains("tone xanh")) return "xanh";
        if (normalized.contains("mau do") || normalized.contains("do khong") || normalized.contains("tone do")) return "đỏ";
        if (normalized.contains("mau hong") || normalized.contains("hong khong")) return "hồng";
        if (normalized.contains("mau vang") || normalized.contains("vang khong")) return "vàng";
        if (normalized.contains("mau nau") || normalized.contains("nau khong")) return "nâu";
        if (normalized.contains("mau be") || normalized.contains("be khong")) return "be";
        if (normalized.contains("mau xam") || normalized.contains("xam khong") || normalized.contains("ghi khong")) return "xám";
        if (normalized.contains("mau navy") || normalized.contains("navy khong")) return "navy";
        if (normalized.contains("mau kem") || normalized.contains("kem khong")) return "kem";

        if (isRefinementFollowUp(message, normalized)
                && profile != null
                && profile.getPreferredColors() != null
                && !profile.getPreferredColors().isEmpty()) {
            return profile.getPreferredColors().iterator().next();
        }
        return null;
    }

    private ChatResponse handleGenderContextStep(String sessionId, String message, ChatSession session) {
        if (session == null || session.getPreferenceProfile() == null || message == null || message.isBlank()) {
            return null;
        }

        ChatSession.PreferenceProfile profile = session.getPreferenceProfile();
        String normalized = VietnameseNormalizer.normalize(message);

        if (PENDING_GENDER_CONTEXT.equals(profile.getPendingQuestionType())
                && profile.getPendingTargetGender() != null
                && !profile.getPendingTargetGender().isBlank()) {
            if (isCrossGenderAffirmation(normalized)) {
                profile.setTargetGender(profile.getPendingTargetGender());
                profile.setPendingQuestionType(null);
                profile.setPendingTargetGender(null);
                profile.setConversationFlow(FLOW_PRODUCT_DISCOVERY);
                profile.setConversationStateUpdatedAt(Instant.now());

                String rememberedCategory = profile.getLastProductCategoryQueried();
                if (rememberedCategory != null && !rememberedCategory.isBlank()) {
                    return runHeuristicProductRefresh(sessionId, rememberedCategory, session);
                }
                return ChatResponse.builder()
                        .sessionId(sessionId)
                        .intent(IntentClassifierService.SEARCH_PRODUCT)
                        .confidence(0.9d)
                        .reply("Ok, mình sẽ ưu tiên lọc theo đồ "
                                + humanizeGender(profile.getTargetGender())
                                + " cho bạn trong các gợi ý tiếp theo nhé.")
                        .suggestions(List.of())
                        .promotions(List.of())
                        .profile(profile)
                        .createdAt(Instant.now())
                        .build();
            }

            if (isSelfGenderAffirmation(normalized)) {
                profile.setPendingQuestionType(null);
                profile.setPendingTargetGender(null);
                profile.setConversationFlow(FLOW_PRODUCT_DISCOVERY);
                profile.setConversationStateUpdatedAt(Instant.now());
                return ChatResponse.builder()
                        .sessionId(sessionId)
                        .intent(IntentClassifierService.SEARCH_PRODUCT)
                        .confidence(0.88d)
                        .reply("Ok, mình sẽ giữ theo đồ "
                                + humanizeGender(profile.getTargetGender())
                                + " cho bạn. Nếu muốn, mình có thể lọc lại theo màu, size hoặc ngân sách ngay.")
                        .suggestions(List.of())
                        .promotions(List.of())
                        .profile(profile)
                        .createdAt(Instant.now())
                        .build();
            }
        }

        String explicitGenderIntent = inferExplicitTargetGenderFromQuery(normalized);
        if (explicitGenderIntent == null || explicitGenderIntent.isBlank()) {
            return null;
        }
        if (profile.getTargetGender() == null || profile.getTargetGender().isBlank()) {
            return null;
        }
        if (explicitGenderIntent.equalsIgnoreCase(profile.getTargetGender())) {
            return null;
        }

        profile.setConversationFlow(FLOW_PRODUCT_DISCOVERY);
        profile.setPendingQuestionType(PENDING_GENDER_CONTEXT);
        profile.setPendingTargetGender(explicitGenderIntent);
        profile.setConversationStateUpdatedAt(Instant.now());

        return ChatResponse.builder()
                .sessionId(sessionId)
                .intent("GENDER_CONTEXT_CLARIFY")
                .confidence(0.93d)
                .reply("Mình thấy bạn thường mua đồ "
                        + humanizeGender(profile.getTargetGender())
                        + ", nhưng yêu cầu này đang nghiêng sang đồ "
                        + humanizeGender(explicitGenderIntent)
                        + ". Bạn đang muốn xem để mua cho bản thân hay mua giúp người khác vậy?")
                .suggestions(List.of())
                .promotions(List.of())
                .profile(profile)
                .createdAt(Instant.now())
                .build();
    }

    private ChatResponse runHeuristicProductRefresh(String sessionId, String searchKeyword, ChatSession session) {
        ToolResultCollector collector = new ToolResultCollector();
        try {
            fashionTools.setCollector(collector);
            fashionTools.setPreferenceProfile(session.getPreferenceProfile());
            fashionTools.setCurrentUserId(session.getUserId());
            String colorFilter = extractColorFilterForTurn(searchKeyword, session.getPreferenceProfile());
            String reply = fashionTools.searchProducts(searchKeyword, null, null, colorFilter, null);
            return ResponseAssembler.build(sessionId, reply, collector, session.getPreferenceProfile());
        } catch (Exception ex) {
            log.warn("Gender-context refresh failed: {}", ex.getMessage());
            return ChatResponse.builder()
                    .sessionId(sessionId)
                    .intent(IntentClassifierService.SEARCH_PRODUCT)
                    .confidence(0.82d)
                    .reply("Ok, mình sẽ lọc lại theo đồ "
                            + humanizeGender(session.getPreferenceProfile().getTargetGender())
                            + " cho bạn ở lượt tiếp theo nhé.")
                    .suggestions(List.of())
                    .promotions(List.of())
                    .profile(session.getPreferenceProfile())
                    .createdAt(Instant.now())
                    .build();
        } finally {
            fashionTools.clearCollector();
        }
    }

    private boolean isGenericBudgetOnlyFollowUp(String message) {
        if (message == null || message.isBlank()) {
            return false;
        }
        String normalized = VietnameseNormalizer.normalize(message);
        boolean hasPrice = hasPriceSignal(normalized);
        boolean hasGarment = !extractGarmentKeyword(message).isBlank();
        boolean hasColor = containsAnyKeyword(normalized,
                "den", "trang", "xanh", "do", "hong", "vang", "nau", "be", "xam", "ghi");
        boolean hasSize = extractSizeFilter(message) != null;
        return hasPrice && !hasGarment && !hasColor && !hasSize;
    }

    private String inferExplicitTargetGenderFromQuery(String normalizedMessage) {
        if (normalizedMessage == null || normalizedMessage.isBlank()) {
            return null;
        }

        if (containsAnyKeyword(normalizedMessage,
                "cho nu", "do nu", "ban gai", "vo", "me", "chi gai", "em gai",
                "vay", "dam", "chan vay", "blouse", "ao kieu", "midi", "wrap dress")) {
            return "female";
        }

        if (containsAnyKeyword(normalizedMessage,
                "cho nam", "do nam", "ban trai", "chong", "anh trai", "em trai",
                "ao so mi nam", "quan chino", "ao oxford", "ao polo nam")) {
            return "male";
        }

        return null;
    }

    private boolean isCrossGenderAffirmation(String normalizedMessage) {
        if (normalizedMessage == null || normalizedMessage.isBlank()) {
            return false;
        }
        return containsAnyKeyword(normalizedMessage,
                "mua cho nguoi khac", "mua giup", "cho ban gai", "cho vo", "cho me",
                "cho chi", "cho em gai", "cho ban trai", "cho chong", "cho anh trai", "cho em trai");
    }

    private boolean isSelfGenderAffirmation(String normalizedMessage) {
        if (normalizedMessage == null || normalizedMessage.isBlank()) {
            return false;
        }
        return containsAnyKeyword(normalizedMessage,
                "cho minh", "mua cho minh", "mua cho ban than", "khong, cho minh", "khong cho nguoi khac");
    }

    private String humanizeGender(String gender) {
        String normalized = VietnameseNormalizer.normalize(gender == null ? "" : gender);
        if ("male".equals(normalized)) {
            return "nam";
        }
        if ("female".equals(normalized)) {
            return "nữ";
        }
        return "phù hợp";
    }

    private boolean looksLikeConcreteProductCategory(String keyword) {
        String normalized = VietnameseNormalizer.normalize(keyword);
        return normalized.contains("ao")
                || normalized.contains("quan")
                || normalized.contains("vay")
                || normalized.contains("dam")
                || normalized.contains("chan vay")
                || normalized.contains("giay")
                || normalized.contains("tui")
                || normalized.contains("ao khoac");
    }

    private void updateBudgetFromMessage(ChatSession.PreferenceProfile profile, String message) {
        if (profile == null || message == null || message.isBlank()) return;
        String normalized = VietnameseNormalizer.normalize(message);
        if (!hasPriceSignal(normalized)) return;
        Double[] priceRange = parsePriceRangeFromMessage(message);
        Double maxPrice = priceRange[1];
        if (maxPrice == null) return;
        profile.setBudget(String.valueOf(maxPrice.longValue()));
    }

    private boolean hasPriceSignal(String normalizedMessage) {
        if (normalizedMessage == null || normalizedMessage.isBlank()) return false;
        return normalizedMessage.contains("gia")
                || normalizedMessage.contains("ngan sach")
                || normalizedMessage.contains("budget")
                || normalizedMessage.contains("duoi")
                || normalizedMessage.contains("tren")
                || normalizedMessage.contains("khoang")
                || normalizedMessage.contains("trieu")
                || normalizedMessage.contains("vnd")
            || normalizedMessage.contains("dong");
    }

    // ========== PRICE PARSING HELPERS ==========

    /**
     * Parse khoảng giá từ tin nhắn tiếng Việt.
     * VD: "quần jean từ 300 đến 500k" → [300000, 500000]
     *     "dưới 2 triệu"             → [null, 2000000]
     *     "áo sơ mi 1.500.000"       → [null, 1500000]
     * Returns: Double[]{minPrice, maxPrice}
     */
    private Double[] parsePriceRangeFromMessage(String message) {
        if (message == null || message.isBlank()) return new Double[]{null, null};

        String normalized = java.text.Normalizer.normalize(message.toLowerCase(), java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replace('\u0111', 'd');

        // Detect range keywords: "từ...đến", "từ...tới"
        boolean hasRange = normalized.matches(".*\\btu\\b.*\\b(den|toi)\\b.*")
                || normalized.matches(".*\\bgiua\\b.*\\bva\\b.*")
                || normalized.contains("-");

        // Extract number+unit patterns: "300k", "1.5 triệu", "1.500.000", "500 nghin"
        Pattern pricePattern = Pattern.compile(
                "(\\d{1,3}(?:\\.\\d{3}){1,2}|\\d+(?:[.,]\\d+)?)\\s*(k|tr(?:ieu)?|trieu|nghin|nghìn|dong|d|vnd)?",
                Pattern.CASE_INSENSITIVE
        );
        Matcher matcher = pricePattern.matcher(normalized);

        List<Double> convertedPrices = new ArrayList<>();
        while (matcher.find()) {
            String rawNumber = matcher.group(1);
            String unit = matcher.group(2);

            try {
                double value;
                // Detect VND-formatted: "1.500.000" (has dot separating thousands)
                if (rawNumber.matches("\\d{1,3}(\\.\\d{3}){1,2}")) {
                    value = Double.parseDouble(rawNumber.replace(".", ""));
                    // Already in VND, no multiplier needed
                } else {
                    value = Double.parseDouble(rawNumber.replace(",", "."));

                    if ("k".equalsIgnoreCase(unit) || "nghin".equalsIgnoreCase(unit) || "nghìn".equalsIgnoreCase(unit)) {
                        value *= 1_000;
                    } else if (unit != null && unit.toLowerCase().startsWith("tr")) {
                        value *= 1_000_000;
                    } else if (unit == null || unit.isEmpty()) {
                        // Guess unit from magnitude: 300 → 300k, 1.5 → 1.5 triệu
                        if (value < 10) {
                            value *= 1_000_000; // "2 triệu" written as "2"
                        } else if (value < 1000) {
                            value *= 1_000;     // "300" → 300k
                        }
                        // >= 1000: treat as raw VND (e.g., "500000")
                    }

                    convertedPrices.add(value);
                    continue;
                }

                convertedPrices.add(value);
            } catch (NumberFormatException ignored) {}
        }

        if (convertedPrices.isEmpty()) return new Double[]{null, null};

        boolean hasLowerBound = normalized.contains("tren") || normalized.matches(".*\\btu\\b.*");
        boolean hasUpperBound = normalized.contains("duoi") || normalized.matches(".*\\b(den|toi)\\b.*");

        // Range: "từ 300 đến 500k" → [300,000, 500,000]
        if (convertedPrices.size() >= 2 && hasRange) {
            // If two numbers found and one has a unit, apply the same unit to the other
            double first = convertedPrices.get(0);
            double second = convertedPrices.get(1);
            // Heuristic: if first is much smaller than second, they might share units
            if (first < 1000 && second >= 1000) {
                double ratio = second / first;
                if (ratio > 500) first *= 1000; // "300 đến 500k" → 300*1000=300k
            }
            double min = Math.min(first, second);
            double max = Math.max(first, second);
            return new Double[]{min, max};
        }

        // Single price: "trên 2 triệu" → [2,000,000, null]
        Double singlePrice = convertedPrices.stream().max(Double::compareTo).orElse(null);
        if (singlePrice == null) return new Double[]{null, null};

        if (hasLowerBound && !hasUpperBound) {
            return new Double[]{singlePrice, null};
        }
        if (hasUpperBound && !hasLowerBound) {
            return new Double[]{null, singlePrice};
        }

        // Default to upper bound if intent is unclear
        return new Double[]{null, singlePrice};
    }

    /**
     * Format price range for display in Vietnamese.
     * VD: [300000, 500000] → "từ 300.000đ đến 500.000đ"
     *     [null, 2000000]  → "dưới 2.000.000đ"
     */
    private String formatPriceRange(Double min, Double max) {
        java.text.DecimalFormat fmt = new java.text.DecimalFormat("#,###");
        if (min != null && max != null) {
            return "từ " + fmt.format(min.longValue()) + "đ đến " + fmt.format(max.longValue()) + "đ";
        } else if (max != null) {
            return "dưới " + fmt.format(max.longValue()) + "đ";
        } else if (min != null) {
            return "trên " + fmt.format(min.longValue()) + "đ";
        }
        return "";
    }

    // ========== TEXT EXTRACTION HELPERS ==========

    /**
     * Extract order number from message (VD: ORD-1713200000000, ORD123, #12345)
     * Returns null if no order number found.
     */
    private String extractOrderNumber(String message) {
        if (message == null) return null;
        // Match ORD-xxx, ORD_xxx, or ORD followed by digits
        java.util.regex.Matcher matcher = Pattern.compile(
                "(?i)(ORD[-_]?\\d{5,}|#\\d{5,})", Pattern.CASE_INSENSITIVE
        ).matcher(message);
        if (matcher.find()) {
            return matcher.group(1).toUpperCase();
        }
        return null;
    }

    /**
     * Extract product search keyword from user message.
     * Strips numbers/punctuation first, then Vietnamese stop words.
     */
    private String extractProductSearchKeyword(String message) {
        if (message == null) return "";

        String quoted = extractQuotedKeyword(message);
        if (!quoted.isBlank()) return quoted;

        String normalized = VietnameseNormalizer.normalize(message);
        String garment = mapToCanonicalGarment(normalized);
        if (!garment.isBlank()) return garment;

        // Strip numbers and punctuation FIRST (trước khi xóa stop words)
        String cleaned = normalized
                .replaceAll("[0-9]", " ")
                .replaceAll("[^a-z\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim();

        // Strip non-accent stop words
        cleaned = cleaned
                .replaceAll("\\b(tu|van|tu van|tim|cho|minh|mua|xem|cua|hang|co|ban|nao|giup|voi|toi|thoi|nhe|nha|vay|thi|con|nua|duoc|khong|la|cai|mot|so|mot so|vai|xin|gi|gia|ve|tu van|tu van ve|tu van cho|minh|dong|trieu|nghin|k|tr)\\b", "")
                .replaceAll("\\s+", " ")
                .trim();

        String fallbackGarment = mapToCanonicalGarment(cleaned);
        if (!fallbackGarment.isBlank()) return fallbackGarment;

        return cleaned.isEmpty() ? message : cleaned;
    }

    private String extractQuotedKeyword(String message) {
        if (message == null) return "";
        Matcher matcher = Pattern.compile("[\"']([^\"']+)[\"']").matcher(message);
        String best = "";
        while (matcher.find()) {
            String candidate = matcher.group(1).trim();
            if (candidate.length() > best.length()) {
                best = candidate;
            }
        }
        return best;
    }

    private String mapToCanonicalGarment(String normalized) {
        if (normalized == null || normalized.isBlank()) return "";

        String n = normalized;
        if (n.contains("ao khoac") || n.contains("jacket") || n.contains("blazer") || n.contains("coat")) return "áo khoác";
        if (n.contains("ao thun") || n.contains("t-shirt") || n.contains("tee")) return "áo thun";
        if (n.contains("ao so mi") || n.contains("shirt")) return "áo sơ mi";
        if (n.contains("ao polo")) return "áo polo";
        if (n.contains("ao hoodie") || n.contains("hoodie")) return "áo hoodie";
        if (n.contains("ao len") || n.contains("sweater")) return "áo len";

        if (n.contains("quan jean") || n.contains("jeans") || n.contains("denim")) return "quần jean";
        if (n.contains("quan tay")) return "quần tây";
        if (n.contains("quan short") || n.contains("short")) return "quần short";
        if (n.contains("quan dai")) return "quần dài";

        if (n.contains("chan vay") || n.contains("skirt")) return "chân váy";
        if (n.contains("dam") || n.contains("dress")) return "đầm";
        if (n.contains("vay")) return "váy";
        if (n.contains("ao dai")) return "áo dài";

        if (n.contains("giay") || n.contains("shoes")) return "giày";
        if (n.contains("tui") || n.contains("bag")) return "túi";
        if (n.contains("non") || n.contains("hat") || n.contains("cap")) return "nón";

        return "";
    }

    /**
     * Trích xuất keyword loại đồ từ tin nhắn để search sản phẩm sau khi tư vấn size.
     * VD: "mình mặc áo thun size gì" → "ao thun"
     */
    private String extractGarmentKeyword(String message) {
        if (message == null) return "";
        String normalized = java.text.Normalizer.normalize(message.toLowerCase(), java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").replace('\u0111', 'd');
        // Tìm cụm từ chỉ loại đồ
        String[] garmentPatterns = {
                "ao so mi", "ao thun", "ao khoac", "ao polo", "ao hoodie", "ao len",
                "quan jean", "quan tay", "quan short", "quan dai",
                "vay", "dam", "chan vay", "ao dai"
        };
        for (String pattern : garmentPatterns) {
            if (normalized.contains(pattern)) return pattern;
        }
        // Fallback: tìm keyword đơn
        if (normalized.contains("ao")) return "ao";
        if (normalized.contains("quan")) return "quan";
        if (normalized.contains("vay") && !normalized.contains("vay neu")) return "vay";
        if (normalized.contains("dam")) return "dam";
        return "";
    }

    private String extractOccasion(String message) {
        if (message == null) return "he";
        String n = message.toLowerCase();
        if (n.contains("di lam") || n.contains("cong so")) return "di_lam";
        if (n.contains("di tiec") || n.contains("tiec")) return "di_tiec";
        if (n.contains("du lich")) return "du_lich";
        if (n.contains("mua dong") || n.contains("dong")) return "dong";
        if (n.contains("mua thu") || n.contains("thu")) return "thu";
        return "he";
    }

    private String extractStyle(String message) {
        if (message == null) return null;
        String n = message.toLowerCase();
        if (n.contains("thanh lich") || n.contains("sang trong")) return "thanh_lich";
        if (n.contains("casual") || n.contains("thoai mai")) return "casual";
        if (n.contains("sporty") || n.contains("the thao")) return "sporty";
        return null;
    }

    private String extractSizeFilter(String message) {
        if (message == null || message.isBlank()) return null;
        String normalized = VietnameseNormalizer.normalize(message);
        Matcher matcher = Pattern.compile("\\bsize\\s*[:=]?\\s*(xs|s|m|l|xl|xxl|\\d{2})\\b")
                .matcher(normalized);
        if (matcher.find()) {
            return matcher.group(1).toUpperCase();
        }
        return null;
    }

    private boolean shouldBrowseProducts(String message) {
        if (message == null || message.isBlank()) return false;
        String normalized = VietnameseNormalizer.normalize(message);
        return normalized.contains("tu van")
                || normalized.contains("goi y")
                || normalized.contains("recommend")
                || normalized.contains("tu van mua sam")
                || normalized.contains("tham khao")
                || normalized.contains("nhieu mau")
                || normalized.contains("da dang")
                || normalized.contains("mac gi")
                || normalized.contains("chon gi")
                || normalized.contains("phu hop")
                || normalized.contains("phoi do")
                || normalized.contains("combo")
                || normalized.contains("di lam")
                || normalized.contains("di choi")
                || normalized.contains("di tiec");
    }

    private boolean shouldListProductTypes(String message, String searchKeyword) {
        if (message == null || message.isBlank()) return false;
        String normalized = VietnameseNormalizer.normalize(message);
        boolean askingCatalog = normalized.contains("co nhung loai")
                || normalized.contains("loai nao")
                || normalized.contains("shop ban gi")
                || normalized.contains("san pham gi")
                || normalized.contains("co gi")
                || normalized.contains("danh muc")
                || normalized.contains("ban gi");
        boolean genericKeyword = searchKeyword == null || searchKeyword.isBlank() || isGenericGarmentKeyword(searchKeyword);
        boolean hasConstraints = hasPriceSignal(normalized)
                || extractSizeFilter(message) != null
                || normalized.contains("mau");

        return askingCatalog || (genericKeyword && shouldBrowseProducts(message) && !hasConstraints);
    }

    private boolean hasShoppingConstraints(String message) {
        if (message == null || message.isBlank()) return false;
        String normalized = VietnameseNormalizer.normalize(message);
        return hasPriceSignal(normalized)
                || extractSizeFilter(message) != null
                || containsAnyKeyword(normalized,
                "den", "trang", "xanh", "do", "hong", "vang", "nau", "be", "xam", "ghi",
                "di lam", "di choi", "di tiec", "cong so", "du lich",
                "casual", "sporty", "thanh lich", "sang trong", "oversize", "slim fit",
                "cotton", "linen", "jean", "kaki", "form", "chat lieu");
    }

    private boolean containsAnyKeyword(String text, String... keywords) {
        if (text == null || text.isBlank()) return false;
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private String deriveProductTypeGroupHint(String searchKeyword, String message) {
        String normalized = VietnameseNormalizer.normalize(searchKeyword);
        if (normalized.isBlank() && message != null) {
            normalized = VietnameseNormalizer.normalize(message);
        }
        if (normalized.contains("ao")) return "áo";
        if (normalized.contains("quan")) return "quần";
        if (normalized.contains("vay") || normalized.contains("dam") || normalized.contains("chan vay")) {
            return "váy";
        }
        if (normalized.contains("giay") || normalized.contains("tui") || normalized.contains("non")) {
            return "phụ kiện";
        }
        return null;
    }

    /**
     * Parse budget string from Personalization panel into maxPrice (Double).
     */
    private Double parseBudget(String budget) {
        if (budget == null || budget.isBlank()) return null;
        String cleaned = budget.toLowerCase()
                .replaceAll("[^0-9kmtrd.]", " ")
                .trim();
        Matcher m = Pattern.compile("([0-9][0-9.,]*)\\s*(k|tr|trieu|d|dong)?")
                .matcher(cleaned);
        Double maxPrice = null;
        while (m.find()) {
            try {
                double value = Double.parseDouble(m.group(1).replace(".", "").replace(",", ""));
                String unit = m.group(2);
                if ("k".equals(unit)) value *= 1_000;
                else if ("tr".equals(unit) || "trieu".equals(unit)) value *= 1_000_000;
                if (maxPrice == null || value > maxPrice) maxPrice = value;
            } catch (NumberFormatException ignored) {}
        }
        return maxPrice;
    }

    // ========== SESSION MANAGEMENT ==========

    private void persistMessages(ChatSession session, String userMessage, ChatResponse response) {
        ChatSession.ChatMessage userMsg = ChatSession.ChatMessage.builder()
                .messageId(UUID.randomUUID().toString())
                .sender(ChatSession.Sender.USER)
                .content(userMessage)
                .intent(ChatSession.IntentMeta.builder()
                        .intentName(response.getIntent())
                        .confidence(response.getConfidence())
                        .build())
                .createdAt(Instant.now())
                .build();

        // Map product suggestions to snapshots for persistence
        List<ChatSession.ProductSuggestionSnapshot> productSnapshots = null;
        if (response.getSuggestions() != null && !response.getSuggestions().isEmpty()) {
            productSnapshots = new ArrayList<>();
            for (ChatResponse.ProductSuggestion s : response.getSuggestions()) {
                productSnapshots.add(ChatSession.ProductSuggestionSnapshot.builder()
                        .productId(s.getProductId())
                        .name(s.getName())
                        .category(s.getCategory())
                        .categoryGender(s.getCategoryGender())
                        .price(s.getPrice())
                        .imageUrl(s.getImageUrl())
                        .link(s.getLink())
                        .availableSizes(s.getAvailableSizes())
                        .availableColors(s.getAvailableColors())
                        .build());
            }
        }

        // Map promotion suggestions to snapshots
        List<ChatSession.PromotionSuggestionSnapshot> promoSnapshots = null;
        if (response.getPromotions() != null && !response.getPromotions().isEmpty()) {
            promoSnapshots = new ArrayList<>();
            for (ChatResponse.PromotionSuggestion p : response.getPromotions()) {
                promoSnapshots.add(ChatSession.PromotionSuggestionSnapshot.builder()
                        .code(p.getCode())
                        .discountType(p.getDiscountType())
                        .discountValue(p.getDiscountValue())
                        .minOrderAmount(p.getMinOrderAmount())
                        .endDate(p.getEndDate())
                        .build());
            }
        }

        ChatSession.ChatMessage botMsg = ChatSession.ChatMessage.builder()
                .messageId(UUID.randomUUID().toString())
                .sender(ChatSession.Sender.BOT)
                .content(response.getReply())
                .suggestions(productSnapshots)
                .promotions(promoSnapshots)
                .createdAt(Instant.now())
                .build();

        session.getMessages().add(userMsg);
        session.getMessages().add(botMsg);
        session.setEndedAt(Instant.now());
        updateConversationState(session.getPreferenceProfile(), userMessage, response);

        try {
            chatSessionRepository.save(session);
        } catch (Exception ex) {
            log.warn("Skip persisting chat session: {}", ex.getMessage());
        }

        // === Task 4: Persist profile for long-term memory ===
        profileEnrichmentService.persistProfileAsync(session.getUserId(), session.getPreferenceProfile());
        
        // === Update Cold Start clarifications if needed ===
        // Nếu intent là COLD_START_CLARIFY, lưu loại sản phẩm được làm rõ vào profile
        if ("COLD_START_CLARIFY".equals(response.getIntent())) {
            ChatSession.PreferenceProfile profile = session.getPreferenceProfile();
            String userText = userMessage.toLowerCase();
            String clarifiedType = extractProductTypeFromClarification(userText);
            if (clarifiedType != null && !clarifiedType.isBlank()) {
                profile.getClarifiedProductTypes().add(clarifiedType);
                profile.setLastProductCategoryQueried(clarifiedType);
                profile.setLastProductQueryTime(Instant.now());
                log.info("Saved clarified product type: {} for user {}", clarifiedType, session.getUserId());
            }
        }
    }

    private void updateConversationState(ChatSession.PreferenceProfile profile, String userMessage, ChatResponse response) {
        if (profile == null || response == null) {
            return;
        }

        String intent = response.getIntent();
        Instant now = Instant.now();

        if ("COLD_START_CLARIFY".equals(intent)) {
            profile.setConversationFlow(FLOW_PRODUCT_DISCOVERY);
            profile.setPendingQuestionType(PENDING_PRODUCT_TYPE);
            profile.setConversationStateUpdatedAt(now);
            return;
        }

        if (IntentClassifierService.CONSULT_SIZE.equals(intent)) {
            if (response.getMissingFields() != null && !response.getMissingFields().isEmpty()) {
                profile.setConversationFlow(FLOW_SIZE_COLLECTION);
                profile.setPendingQuestionType(PENDING_MEASUREMENTS);
            } else {
                profile.setConversationFlow(FLOW_PRODUCT_DISCOVERY);
                profile.setPendingQuestionType(PENDING_BUDGET);
            }
            profile.setConversationStateUpdatedAt(now);
            return;
        }

        if (IntentClassifierService.SEARCH_PRODUCT.equals(intent) || IntentClassifierService.CONSULT_SEASON.equals(intent)) {
            profile.setConversationFlow(FLOW_PRODUCT_DISCOVERY);
            if (response.getSuggestions() == null || response.getSuggestions().isEmpty()) {
                profile.setPendingQuestionType(PENDING_BUDGET);
            } else if (response.getMissingFields() != null && !response.getMissingFields().isEmpty()) {
                profile.setPendingQuestionType(PENDING_SIZE);
            } else {
                profile.setPendingQuestionType(null);
            }
            profile.setConversationStateUpdatedAt(now);
            return;
        }

        if (IntentClassifierService.WISHLIST_RECOMMENDATION.equals(intent)) {
            profile.setConversationFlow(FLOW_WISHLIST_REVIEW);
            profile.setPendingQuestionType(null);
            profile.setConversationStateUpdatedAt(now);
            return;
        }

        if (IntentClassifierService.LOYALTY_BENEFIT.equals(intent)) {
            profile.setConversationFlow(FLOW_LOYALTY_REVIEW);
            profile.setPendingQuestionType(null);
            profile.setConversationStateUpdatedAt(now);
            return;
        }

        if (IntentClassifierService.GREETING.equals(intent) || IntentClassifierService.OUT_OF_DOMAIN.equals(intent)) {
            clearConversationState(profile);
        }
    }

    private void clearConversationState(ChatSession.PreferenceProfile profile) {
        if (profile == null) {
            return;
        }
        profile.setConversationFlow(null);
        profile.setPendingQuestionType(null);
        profile.setPendingTargetGender(null);
        profile.setConversationStateUpdatedAt(null);
    }

    /**
     * Extract product type từ câu trả lời của user khi được hỏi trong Cold Start.
     * VD: "áo thun" → "áo thun", "thun" → "áo thun"
     */
    private String extractProductTypeFromClarification(String userText) {
        if (userText == null || userText.isBlank()) return null;
        
        String normalized = VietnameseNormalizer.normalize(userText);
        
        // Matching các loại sản phẩm phổ biến
        if (normalized.contains("ao thun")) return "áo thun";
        if (normalized.contains("ao so mi")) return "áo sơ mi";
        if (normalized.contains("ao khoac")) return "áo khoác";
        if (normalized.contains("ao polo")) return "áo polo";
        if (normalized.contains("ao hoodie")) return "áo hoodie";
        if (normalized.contains("ao len")) return "áo len";
        
        if (normalized.contains("quan jean")) return "quần jean";
        if (normalized.contains("quan tay")) return "quần tây";
        if (normalized.contains("quan short")) return "quần short";
        if (normalized.contains("quan dai")) return "quần dài";
        
        if (normalized.contains("vay")) return "váy";
        if (normalized.contains("dam")) return "đầm";
        if (normalized.contains("chan vay")) return "chân váy";
        if (normalized.contains("giay")) return "giày";
        if (normalized.contains("tui")) return "túi";
        
        return null;
    }

    private ChatSession findOrCreateSession(String sessionId, String userId) {
        try {
            Optional<ChatSession> existing = chatSessionRepository.findBySessionId(sessionId);
            if (existing.isPresent()) {
                ChatSession session = existing.get();
                if (session.getUserId() == null || session.getUserId().isBlank()) {
                    session.setUserId(userId);
                }
                if (session.getPreferenceProfile() == null) {
                    session.setPreferenceProfile(ChatSession.PreferenceProfile.empty());
                }
                if (session.getMessages() == null) {
                    session.setMessages(new ArrayList<>());
                }
                return session;
            }
        } catch (Exception ex) {
            log.warn("Use in-memory chat session: {}", ex.getMessage());
        }

        // === Task 4: Bootstrap new session with persisted profile for returning users ===
        ChatSession.PreferenceProfile persistedProfile = profileEnrichmentService.loadPersistedProfile(userId);

        return ChatSession.builder()
                .sessionId(sessionId)
                .userId(userId)
                .startedAt(Instant.now())
                .endedAt(Instant.now())
                .messages(new ArrayList<>())
                .preferenceProfile(persistedProfile)
                .build();
    }

    private void mergePreferences(ChatSession session, ChatRequest.UserPreferences preferences) {
        if (preferences == null) return;
        ChatSession.PreferenceProfile profile = session.getPreferenceProfile();
        if (profile == null) {
            profile = ChatSession.PreferenceProfile.empty();
            session.setPreferenceProfile(profile);
        }
        if (preferences.getTone() != null && !preferences.getTone().isBlank()) {
            profile.setPreferredTone(preferences.getTone());
        }
        if (preferences.getStyle() != null && !preferences.getStyle().isBlank()) {
            profile.setStyle(preferences.getStyle());
        }
        if (preferences.getBudget() != null && !preferences.getBudget().isBlank()) {
            profile.setBudget(preferences.getBudget());
        }
        if (preferences.getFocus() != null) {
            profile.getFocusTags().addAll(preferences.getFocus());
        }
    }

    private String resolveUserId(String userIdHeader, String sessionId) {
        if (userIdHeader != null && !userIdHeader.isBlank()) {
            return userIdHeader.trim();
        }
        return "guest-" + sessionId.substring(0, Math.min(12, sessionId.length()));
    }

    private boolean isGuestUser(String userId) {
        return userId == null || userId.isBlank() || userId.startsWith("guest-");
    }

    private ChatResponse buildLoginRequiredResponse(String sessionId,
                                                    String intent,
                                                    double confidence,
                                                    ToolResultCollector collector,
                                                    ChatSession session,
                                                    String reply) {
        return ChatResponse.builder()
                .sessionId(sessionId)
                .intent(intent)
                .confidence(confidence)
                .reply(reply)
                .missingFields(collector.getMissingFields())
                .suggestions(collector.getProducts())
                .promotions(collector.getPromotions())
                .profile(session.getPreferenceProfile())
                .createdAt(Instant.now())
                .build();
    }

    /**
     * Guardrail được đặt ở cuối flow để mọi response path đều bị hậu kiểm như nhau:
     * direct intent, heuristic fallback, deictic lookup và agent path.
     */
    private ChatResponse applyFinalGuardrail(ChatResponse response, ToolResultCollector collector) {
        if (response == null) {
            return null;
        }
        ToolResultCollector effectiveCollector = collectorFromResponse(response, collector);
        String sanitizedReply = responseGuardrail.validateAndSanitize(response.getReply(), effectiveCollector);
        response.setReply(sanitizedReply);
        return response;
    }

    private ToolResultCollector collectorFromResponse(ChatResponse response, ToolResultCollector baseCollector) {
        if (baseCollector != null) {
            return baseCollector;
        }
        ToolResultCollector collector = new ToolResultCollector();
        if (response == null) {
            return collector;
        }
        collector.addProducts(response.getSuggestions());
        collector.addPromotions(response.getPromotions());
        collector.addMissingFields(response.getMissingFields());
        return collector;
    }

    // ========== CART CONTEXT INJECTION (Task 3) ==========

    /**
     * Fetch the user's active cart from cart-service and format as context string.
     * Used to give the LLM awareness of what the user is currently buying.
     * Gracefully returns empty string on any failure or for guest users.
     */
    @SuppressWarnings("unchecked")
    private String fetchActiveCartContext(String userId) {
        if (userId == null || userId.startsWith("guest-")) return "";

        try {
            Map<String, Object> cartData = webClient.get()
                    .uri(cartServiceUrl + "/api/v1/cart")
                    .headers(headers -> headers.add("X-User-Id", userId))
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(5))
                    .block();

            if (cartData == null) return "";

            Object items = cartData.get("items");
            if (!(items instanceof List<?> itemList) || itemList.isEmpty()) return "";

            StringBuilder context = new StringBuilder("Khách hàng đang có trong giỏ: ");
            for (int i = 0; i < itemList.size(); i++) {
                if (!(itemList.get(i) instanceof Map<?, ?> item)) continue;

                String name = stringVal(item.get("productName"));
                String size = stringVal(item.get("size"));
                String color = stringVal(item.get("color"));
                String qty = stringVal(item.get("quantity"));
                String price = stringVal(item.get("price"));

                if (name.isBlank()) continue;

                context.append(name);
                boolean hasDetails = !size.isBlank() || !color.isBlank();
                if (hasDetails) context.append(" (");
                if (!size.isBlank()) context.append("Size ").append(size);
                if (!size.isBlank() && !color.isBlank()) context.append(", ");
                if (!color.isBlank()) context.append("Màu ").append(color);
                if (hasDetails) context.append(")");
                if (!qty.isBlank()) context.append(" x").append(qty);
                if (!price.isBlank()) context.append(" - ").append(price).append("đ");

                if (i < itemList.size() - 1) context.append("; ");
            }
            context.append(". Hãy ưu tiên gợi ý sản phẩm phối hợp với các sản phẩm này.");

            String result = context.toString();
            log.debug("Cart context for user {}: {}", userId, result);
            return result;

        } catch (Exception ex) {
            log.debug("Skip cart context injection: {}", ex.getMessage());
            return "";
        }
    }

    private String stringVal(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }
}
