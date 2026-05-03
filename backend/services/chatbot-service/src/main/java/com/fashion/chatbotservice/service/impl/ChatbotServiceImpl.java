package com.fashion.chatbotservice.service.impl;

import com.fashion.chatbotservice.agent.FashionAgent;
import com.fashion.chatbotservice.agent.FashionTools;
import com.fashion.chatbotservice.agent.ResponseAssembler;
import com.fashion.chatbotservice.agent.ToolResultCollector;
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

    private final FashionAgent fashionAgent;
    private final FashionTools fashionTools;
    private final ChatSessionRepository chatSessionRepository;
    private final IntentClassifierService intentClassifierService;
    private final ProfileEnrichmentService profileEnrichmentService;
    private final ChatAnalyticsService chatAnalyticsService;
    private final SizeAdvisorService sizeAdvisorService;
    private final WebClient webClient;

    @Value("${chatbot.use-agent:true}")
    private boolean useAgent;

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

        // Enrich profile từ message + purchase history
        profileEnrichmentService.enrichFromMessage(session.getPreferenceProfile(), request.getMessage());
        profileEnrichmentService.enrichFromPurchaseHistory(session.getPreferenceProfile(), userId);

        // === Task 2: Out-of-Domain Short-Circuit ===
        // Classify intent BEFORE calling LLM to block off-topic/injection queries
        IntentClassifierService.IntentScore preCheck = intentClassifierService.classify(request.getMessage());
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
            persistMessages(session, request.getMessage(), oodResponse);
            return oodResponse;
        }

        // Execute agent hoặc heuristic fallback
        ChatResponse response;
        ToolResultCollector collector = new ToolResultCollector();

        if (useAgent) {
            response = executeAgent(sessionId, request.getMessage(), session, collector);
        } else {
            response = executeHeuristicFallback(sessionId, request.getMessage(), session, collector);
        }

        // Persist messages to MongoDB
        persistMessages(session, request.getMessage(), response);

        // Analytics
        long totalLatency = System.currentTimeMillis() - startTime;
        chatAnalyticsService.record(traceId, sessionId, userId,
                request.getMessage(), response, collector, totalLatency);

        return response;
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

    /**
     * Chế độ Agent: LLM tự quyết định gọi tool nào.
     * Safety net: nếu Agent trả về rỗng nhưng message rõ ràng là tìm SP → fallback heuristic.
     */
    private ChatResponse executeAgent(String sessionId, String message,
                                       ChatSession session, ToolResultCollector collector) {
        try {
            fashionTools.setCollector(collector);

            // === Task 3: Inject active cart context into the message ===
            String cartContext = fetchActiveCartContext(session.getUserId());
            String enrichedMessage = cartContext.isBlank()
                    ? message
                    : "[Ngữ cảnh giỏ hàng] " + cartContext + "\n\n" + message;

            String llmReply = fashionAgent.chat(sessionId, enrichedMessage);
            ChatResponse agentResponse = ResponseAssembler.build(sessionId, llmReply, collector, session.getPreferenceProfile());

            // Safety net: nếu Agent trả về không có sản phẩm nhưng message rõ ràng là tìm sản phẩm
            // → fallback sang heuristic (có logic parse giá tiếng Việt chính xác hơn)
            if (collector.getProducts().isEmpty() && collector.getPromotions().isEmpty()) {
                IntentClassifierService.IntentScore intent = intentClassifierService.classify(message);
                if (IntentClassifierService.SEARCH_PRODUCT.equals(intent.intent())
                        || IntentClassifierService.ASK_PROMOTION.equals(intent.intent())
                        || IntentClassifierService.ASK_POLICY.equals(intent.intent())) {
                    log.info("Agent returned empty results for likely {} intent, falling back to heuristic", intent.intent());
                    ToolResultCollector freshCollector = new ToolResultCollector();
                    return executeHeuristicFallback(sessionId, message, session, freshCollector);
                }
            }

            return agentResponse;
        } catch (Exception ex) {
            log.error("Agent execution failed, falling back to heuristic: {}", ex.getMessage());
            return executeHeuristicFallback(sessionId, message, session, collector);
        } finally {
            fashionTools.clearCollector();
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

                SizeAdvisorService.GarmentType type = sizeAdvisorService.detectGarmentType(message);
                SizeAdvisorService.SizeResult result = sizeAdvisorService.suggest(merged, type);
                collector.setSizeRecommendation(result.recommendedSize());

                // Tư vấn size xong → tự động tìm sản phẩm phù hợp
                String garmentKeyword = extractGarmentKeyword(message);
                if (garmentKeyword != null && !garmentKeyword.isBlank()) {
                    try {
                        fashionTools.setCollector(collector);
                        Double maxPrice = parseBudget(profile.getBudget());
                        fashionTools.searchProducts(garmentKeyword, null, maxPrice != null ? maxPrice.longValue() : null, null);
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
                if (!collector.getProducts().isEmpty()) {
                    sizeReply.append("\n\nMình cũng tìm thấy một số sản phẩm phù hợp cho bạn:");
                }
                yield sizeReply.toString();
            }

            case IntentClassifierService.ASK_POLICY -> {
                // Gọi Knowledge Base để trả lời câu hỏi chính sách, FAQ
                try {
                    fashionTools.setCollector(collector);
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
                    String searchKeyword = extractProductSearchKeyword(message);
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
                    String toolResult = fashionTools.searchProducts(searchKeyword,
                            minPrice != null ? minPrice.longValue() : null,
                            maxPrice != null ? maxPrice.longValue() : null,
                            null);

                    // Nếu có filter giá mà không tìm thấy → retry KHÔNG có giá để gợi ý sản phẩm gần nhất
                    if (collector.getProducts().isEmpty() && hasPriceFilter) {
                        log.info("No products found with price filter [{}-{}], retrying without price for keyword: {}",
                                minPrice, maxPrice, searchKeyword);
                        fashionTools.clearCollector();
                        fashionTools.setCollector(collector);
                        String fallbackResult = fashionTools.searchProducts(searchKeyword, null, null, null);

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
                // Kiểm tra đơn hàng — trích xuất mã đơn nếu có, nếu không thì hỏi lại
                String orderNumber = extractOrderNumber(message);
                if (orderNumber != null) {
                    try {
                        fashionTools.setCollector(collector);
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
                // Fallback: thử search product nếu message có keyword thời trang
                String keyword = extractProductSearchKeyword(message);
                if (keyword.length() >= 2) {
                    try {
                        fashionTools.setCollector(collector);
                        String searchResult = fashionTools.searchProducts(keyword, null, null, null);
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

        // Single price: "dưới 500k" → [null, 500,000]
        Double maxPrice = convertedPrices.stream().max(Double::compareTo).orElse(null);
        return new Double[]{null, maxPrice};
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
        // Normalize diacritics first so we only need non-accent stop words
        String normalized = java.text.Normalizer.normalize(message.toLowerCase(), java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replace('\u0111', 'd');
        // Strip numbers and punctuation FIRST (trước khi xóa stop words)
        String cleaned = normalized
                .replaceAll("[0-9]", " ")
                .replaceAll("[^a-z\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim();
        // Strip non-accent stop words
        cleaned = cleaned
                .replaceAll("\\b(tim|cho|minh|mua|xem|cua|hang|co|ban|nao|giup|voi|toi|ban|thoi|nhe|nha|vay|thi|con|nua|duoc|khong|la|cai|mot|xin|gi|gia|tu|den|toi|duoi|tren|khoang|nhe|dong|trieu|nghin|k|tr)\\b", "")
                .replaceAll("\\s+", " ")
                .trim();
        return cleaned.isEmpty() ? message : cleaned;
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

        try {
            chatSessionRepository.save(session);
        } catch (Exception ex) {
            log.warn("Skip persisting chat session: {}", ex.getMessage());
        }

        // === Task 4: Persist profile for long-term memory ===
        profileEnrichmentService.persistProfileAsync(session.getUserId(), session.getPreferenceProfile());
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
