package com.fashion.chatbotservice.service;

import com.fashion.chatbotservice.dto.ChatRequest;
import com.fashion.chatbotservice.dto.ChatResponse;
import com.fashion.chatbotservice.dto.SessionResponse;
import com.fashion.chatbotservice.model.ChatSession;
import com.fashion.chatbotservice.repository.ChatSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import jakarta.annotation.PostConstruct;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings({ "unchecked", "null" })
public class ChatbotService {

    private static final List<String> SIZE_ORDER = List.of("XS", "S", "M", "L", "XL", "XXL");

    private static final Pattern HEIGHT_CM_PATTERN = Pattern.compile("(\\d{2,3})\\s*(cm|centimet)", Pattern.CASE_INSENSITIVE);
    private static final Pattern HEIGHT_M_PATTERN = Pattern.compile("(1)\\s*m\\s*(\\d{2})", Pattern.CASE_INSENSITIVE);
    private static final Pattern WEIGHT_PATTERN = Pattern.compile("(\\d{2,3})\\s*(kg|ky|kilo)", Pattern.CASE_INSENSITIVE);
    private static final Pattern CHEST_PATTERN = Pattern.compile("(nguc|v1)\\s*[:=]?\\s*(\\d{2,3})", Pattern.CASE_INSENSITIVE);
    private static final Pattern WAIST_PATTERN = Pattern.compile("(eo|v2)\\s*[:=]?\\s*(\\d{2,3})", Pattern.CASE_INSENSITIVE);
    private static final Pattern HIP_PATTERN = Pattern.compile("(hong|v3)\\s*[:=]?\\s*(\\d{2,3})", Pattern.CASE_INSENSITIVE);
    private static final Pattern SIZE_PATTERN = Pattern.compile("size\\s*[:=]?\\s*(xs|s|m|l|xl|xxl|\\d{2})", Pattern.CASE_INSENSITIVE);

    private final WebClient webClient;
    private final ChatSessionRepository chatSessionRepository;
    private final IntentClassifierService intentClassifierService;

    @Value("${chatbot.product-service-url:http://localhost:8080}")
    private String productServiceUrl;

    @Value("${chatbot.promotion-service-url:http://localhost:8080}")
    private String promotionServiceUrl;

    @Value("${chatbot.order-service-url:http://localhost:8080}")
    private String orderServiceUrl;

    @Value("${openrouter.api-key:}")
    private String openRouterApiKey;

    @Value("${openrouter.base-url:https://openrouter.ai/api/v1}")
    private String openRouterBaseUrl;

    @Value("${openrouter.model:openai/gpt-4o-mini}")
    private String openRouterModel;

    @Value("${openrouter.fallback-model:openrouter/auto}")
    private String openRouterFallbackModel;

    @Value("${openrouter.max-tokens:400}")
    private int openRouterMaxTokens;

    @Value("${openrouter.site-url:http://localhost:8087}")
    private String openRouterSiteUrl;

    @Value("${openrouter.app-name:fashion-chatbot-service}")
    private String openRouterAppName;

    @PostConstruct
    public void bootstrapTrainingData() {
        try {
            intentClassifierService.bootstrapDefaultIntentsIfNeeded();
        } catch (Exception ex) {
            log.warn("Skip bootstrap training data at startup: {}", ex.getMessage());
        }
    }

    public ChatResponse chat(ChatRequest request, String userIdHeader) {
        if (request == null || request.getMessage() == null || request.getMessage().isBlank()) {
            throw new IllegalArgumentException("Nội dung tin nhắn không được để trống");
        }

        String sessionId = request.getSessionId();
        if (sessionId == null || sessionId.isBlank()) {
            sessionId = UUID.randomUUID().toString();
        }

        String resolvedUserId = resolveUserId(userIdHeader, sessionId);
        ChatSession session = findOrCreateSession(sessionId, resolvedUserId);
        mergePreferences(session, request.getPreferences());

        IntentClassifierService.IntentScore intentScore = intentClassifierService.classify(request.getMessage());
        updateProfileFromMessage(session.getPreferenceProfile(), request.getMessage());
        hydrateProfileFromPurchaseHistory(session.getPreferenceProfile(), resolvedUserId);

        RouteResult routeResult = routeByIntent(intentScore.intent(), request.getMessage(), session.getPreferenceProfile(), resolvedUserId);

        ChatSession.ChatMessage userMessage = ChatSession.ChatMessage.builder()
                .messageId(UUID.randomUUID().toString())
                .sender(ChatSession.Sender.USER)
                .content(request.getMessage())
                .intent(ChatSession.IntentMeta.builder()
                        .intentName(intentScore.intent())
                        .confidence(intentScore.confidence())
                        .build())
                .createdAt(Instant.now())
                .build();

        ChatSession.ChatMessage botMessage = ChatSession.ChatMessage.builder()
                .messageId(UUID.randomUUID().toString())
                .sender(ChatSession.Sender.BOT)
                .content(routeResult.reply())
                .intent(null)
                .createdAt(Instant.now())
                .build();

        session.getMessages().add(userMessage);
        session.getMessages().add(botMessage);
        session.setEndedAt(Instant.now());
        try {
            chatSessionRepository.save(session);
        } catch (Exception ex) {
            log.warn("Skip persisting chat session because MongoDB is unavailable: {}", ex.getMessage());
        }

        return ChatResponse.builder()
                .sessionId(session.getSessionId())
                .intent(intentScore.intent())
                .confidence(intentScore.confidence())
                .reply(routeResult.reply())
                .missingFields(routeResult.missingFields())
                .suggestions(routeResult.suggestions())
                .promotions(routeResult.promotions())
                .profile(session.getPreferenceProfile())
                .createdAt(Instant.now())
                .build();
    }

    public SessionResponse getSession(String sessionId) {
        ChatSession session;
        try {
            session = chatSessionRepository.findBySessionId(sessionId)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phiên chat"));
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalStateException("MongoDB chưa kết nối, chưa thể lấy lịch sử chat");
        }

        return SessionResponse.builder()
                .sessionId(session.getSessionId())
                .userId(session.getUserId())
                .messages(session.getMessages())
                .profile(session.getPreferenceProfile())
                .build();
    }

    private RouteResult routeByIntent(String intent, String message,
                                      ChatSession.PreferenceProfile profile,
                                      String userId) {
        return switch (intent) {
            case IntentClassifierService.CONSULT_SIZE -> handleSizeIntent(message, profile);
            case IntentClassifierService.CONSULT_SEASON -> handleSeasonIntent(message, profile);
            case IntentClassifierService.ASK_PROMOTION -> handlePromotionIntent(profile, userId);
            default -> handleGeneralIntent(message, profile);
        };
    }

    private RouteResult handleSizeIntent(String message, ChatSession.PreferenceProfile profile) {
        Measurements measurements = extractMeasurements(message);
        List<String> missing = new ArrayList<>();

        if (measurements.heightCm == null) missing.add("chiều cao (cm)");
        if (measurements.weightKg == null) missing.add("cân nặng (kg)");
        if (measurements.chestCm == null) missing.add("vòng ngực (cm)");
        if (measurements.waistCm == null) missing.add("vòng eo (cm)");
        if (measurements.hipCm == null) missing.add("vòng hông (cm)");

        if (!missing.isEmpty()) {
            String reply = "Để tư vấn size chính xác, bạn vui lòng cung cấp thêm: " + String.join(", ", missing)
                    + ". Ví dụ: cao 170cm, nặng 62kg, ngực 92, eo 74, hông 95.";
            return new RouteResult(reply, missing, List.of(), List.of());
        }

        GarmentType garmentType = detectGarmentType(message);
        String suggestedSize = suggestSize(measurements, garmentType);
        profile.getPreferredSizes().add(suggestedSize);

        String query = garmentType == GarmentType.BOTTOM ? "quần" : "áo";
        List<ProductCard> products = searchProducts(query, 30);
        products = rankByProfile(products, profile);

        List<ChatResponse.ProductSuggestion> suggestions = products.stream()
                .filter(product -> product.availableSizes().isEmpty() || product.availableSizes().contains(suggestedSize))
                .limit(6)
                .map(product -> toSuggestion(product,
                        "Phù hợp size " + suggestedSize + " và còn hàng"))
                .toList();

        if (suggestions.isEmpty()) {
            suggestions = products.stream()
                    .limit(4)
                    .map(product -> toSuggestion(product,
                            "Mẫu tương đồng để bạn tham khảo thêm"))
                    .toList();
        }

        String notes = garmentType == GarmentType.BOTTOM
                ? "Quần/chân váy thường phụ thuộc nhiều vào số đo eo-hông, nên nếu bạn thích ôm sát có thể giảm 1 size."
                : "Áo thường phụ thuộc vòng ngực và vai, nếu thích thoải mái có thể tăng 1 size.";

        String reply = "Mình đã đối chiếu số đo và gợi ý cho bạn size " + suggestedSize + ". " + notes;
        return new RouteResult(reply, List.of(), suggestions, List.of());
    }

    private RouteResult handleSeasonIntent(String message, ChatSession.PreferenceProfile profile) {
        String normalizedMessage = normalize(message);
        Set<String> requestedKeywords = extractRequestedKeywords(normalizedMessage);
        List<String> queries = buildSeasonQueries(message, profile, requestedKeywords);
        Map<String, ProductCard> merged = new LinkedHashMap<>();
        for (String query : queries) {
            for (ProductCard product : searchProducts(query, 16)) {
                merged.putIfAbsent(product.id(), product);
            }
        }

        List<ProductCard> ranked = rankByProfileAndRequestedItems(new ArrayList<>(merged.values()), profile, requestedKeywords);

        List<ProductCard> exactRequested = requestedKeywords.isEmpty()
                ? ranked
                : ranked.stream().filter(product -> matchesRequestedKeywords(product, requestedKeywords)).toList();

        List<ProductCard> sourceForSuggestions = !exactRequested.isEmpty() ? exactRequested : ranked;

        List<ChatResponse.ProductSuggestion> suggestions = sourceForSuggestions.stream()
                .limit(8)
                .map(product -> toSuggestion(product,
                        "Gợi ý theo nhu cầu mùa/xu hướng bạn vừa hỏi"))
                .toList();

        String focus = profile.getFocusTags().isEmpty()
                ? "phù hợp thời tiết và dễ phối"
                : "ưu tiên " + String.join(", ", profile.getFocusTags());

        String reply;
        if (suggestions.isEmpty()) {
            reply = "Hiện mình chưa tìm thấy sản phẩm phù hợp theo tiêu chí này. Mình sẽ đề xuất thêm mẫu nổi bật khác trong lần kế tiếp.";
        } else if (!requestedKeywords.isEmpty() && exactRequested.isEmpty()) {
            reply = "Mình chưa thấy đúng sản phẩm bạn nêu trong dữ liệu hiện tại, nên đang gửi các lựa chọn gần nhất để bạn tham khảo. "
                    + "Bạn có thể nói rõ thêm chất liệu/mức giá để mình lọc sát hơn.";
        } else {
            reply = "Mình đã lọc các mẫu " + focus + " từ dữ liệu sản phẩm thực tế. Bạn có thể xem danh sách gợi ý bên dưới.";
        }

        return new RouteResult(reply, List.of(), suggestions, List.of());
    }

    private RouteResult handlePromotionIntent(ChatSession.PreferenceProfile profile, String userId) {
        List<PromotionCard> promotions = getActivePromotions();

        List<ChatResponse.PromotionSuggestion> promotionSuggestions = promotions.stream()
                .limit(4)
                .map(this::toPromotionSuggestion)
                .toList();

        List<ProductCard> featured = rankByProfile(getFeaturedProducts(8), profile);
        List<ChatResponse.ProductSuggestion> productSuggestions = featured.stream()
                .limit(6)
                .map(product -> toSuggestion(product,
                        "Sản phẩm nổi bật có thể áp dụng khuyến mãi đang hiệu lực"))
                .toList();

        if (promotionSuggestions.isEmpty()) {
            String reply = "Hiện chưa có chương trình khuyến mãi nào đang diễn ra. Mình gửi bạn vài sản phẩm nổi bật để tham khảo ngay.";
            return new RouteResult(reply, List.of(), productSuggestions, List.of());
        }

        String personalizedNote = userId.startsWith("guest-")
                ? "Bạn có thể đăng nhập để theo dõi ưu đãi cá nhân hóa tốt hơn."
                : "Mình đã ưu tiên các mẫu gần với lịch sử mua sắm của bạn.";

        String reply = "Mình đã lấy danh sách khuyến mãi đang hiệu lực từ hệ thống. " + personalizedNote;
        return new RouteResult(reply, List.of(), productSuggestions, promotionSuggestions);
    }

    private RouteResult handleGeneralIntent(String message, ChatSession.PreferenceProfile profile) {
        String llmReply = generateGeneralReplyWithOpenRouter(message, profile);
        if (!llmReply.isBlank()) {
            return new RouteResult(llmReply, List.of(), List.of(), List.of());
        }

        String tone = Optional.ofNullable(profile.getPreferredTone()).orElse("thân thiện");
        String style = Optional.ofNullable(profile.getStyle()).orElse("linh hoạt");

        String reply = "Mình luôn sẵn sàng hỗ trợ bạn tư vấn size, gợi ý outfit theo mùa/xu hướng và kiểm tra khuyến mãi. "
                + "Hiện mình đang ưu tiên phong cách " + style + " với cách trả lời " + tone + ". "
                + "Bạn có thể bắt đầu bằng: 'Tư vấn size cho mình' hoặc 'Có khuyến mãi nào đang chạy không?'.";

        return new RouteResult(reply, List.of(), List.of(), List.of());
    }

    private String generateGeneralReplyWithOpenRouter(String message, ChatSession.PreferenceProfile profile) {
        if (openRouterApiKey == null || openRouterApiKey.isBlank()) {
            return "";
        }

        String tone = Optional.ofNullable(profile.getPreferredTone()).orElse("thân thiện");
        String style = Optional.ofNullable(profile.getStyle()).orElse("linh hoạt");
        String budget = Optional.ofNullable(profile.getBudget()).orElse("");
        String focus = profile.getFocusTags().isEmpty() ? "không có" : String.join(", ", profile.getFocusTags());

        String systemPrompt = "Bạn là trợ lý mua sắm thời trang cho Fashion Store. "
                + "Trả lời tiếng Việt ngắn gọn, đúng trọng tâm, tránh bịa thông tin ngoài dữ liệu hệ thống. "
                + "Phong cách mong muốn: " + style + ". Tông giọng: " + tone + ". "
                + "Focus ưu tiên: " + focus + ". "
                + (budget.isBlank() ? "" : "Ngân sách tham chiếu: " + budget + ". ");

        String endpoint = openRouterBaseUrl.endsWith("/")
                ? openRouterBaseUrl + "chat/completions"
                : openRouterBaseUrl + "/chat/completions";

        try {
            Map<String, Object> response = callOpenRouter(endpoint, openRouterModel, systemPrompt, message);

            if (response == null) {
                return "";
            }

            return extractCompletionContent(response);
        } catch (WebClientResponseException ex) {
            if (ex.getStatusCode().value() == 402
                    && openRouterFallbackModel != null
                    && !openRouterFallbackModel.isBlank()
                    && !openRouterFallbackModel.equalsIgnoreCase(openRouterModel)) {
                log.warn("OpenRouter model '{}' failed with 402, retrying fallback model '{}'",
                        openRouterModel, openRouterFallbackModel);
                try {
                    Map<String, Object> fallbackResponse = callOpenRouter(endpoint, openRouterFallbackModel, systemPrompt, message);
                    if (fallbackResponse != null) {
                        return extractCompletionContent(fallbackResponse);
                    }
                } catch (Exception fallbackEx) {
                    log.warn("OpenRouter fallback model call failed: {}", fallbackEx.getMessage());
                }
            }

            log.warn("OpenRouter call failed: {}", ex.getMessage());
            return "";
        } catch (Exception ex) {
            log.warn("OpenRouter call failed: {}", ex.getMessage());
            return "";
        }
    }

    private Map<String, Object> callOpenRouter(String endpoint, String model, String systemPrompt, String userMessage) {
        Map<String, Object> body = Map.of(
                "model", model,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userMessage)
                ),
                "max_tokens", openRouterMaxTokens,
                "temperature", 0.7
        );

        return webClient.post()
                .uri(endpoint)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + openRouterApiKey)
                .header("HTTP-Referer", openRouterSiteUrl)
                .header("X-Title", openRouterAppName)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Map.class)
                .block();
    }

    private String extractCompletionContent(Map<String, Object> response) {
        for (Object choiceObj : toList(response.get("choices"))) {
            Map<String, Object> choice = toMap(choiceObj);
            Map<String, Object> message = toMap(choice.get("message"));
            Object content = message.get("content");

            if (content instanceof String text && !text.isBlank()) {
                return text.trim();
            }

            if (content instanceof List<?> parts) {
                StringBuilder merged = new StringBuilder();
                for (Object partObj : parts) {
                    String text = asString(toMap(partObj).get("text"));
                    if (!text.isBlank()) {
                        if (!merged.isEmpty()) {
                            merged.append('\n');
                        }
                        merged.append(text);
                    }
                }
                if (!merged.isEmpty()) {
                    return merged.toString().trim();
                }
            }
        }

        return "";
    }

    private List<ProductCard> rankByProfile(List<ProductCard> products, ChatSession.PreferenceProfile profile) {
        return rankByProfileAndRequestedItems(products, profile, Set.of());
    }

    private List<ProductCard> rankByProfileAndRequestedItems(
            List<ProductCard> products,
            ChatSession.PreferenceProfile profile,
            Set<String> requestedKeywords) {
        if (products.isEmpty()) {
            return products;
        }

        Map<String, Integer> scoreMap = new HashMap<>();
        for (ProductCard product : products) {
            int score = 0;

            if (!profile.getPreferredCategories().isEmpty()
                    && profile.getPreferredCategories().stream().anyMatch(cat ->
                    product.category().toLowerCase(Locale.ROOT).contains(cat.toLowerCase(Locale.ROOT)))) {
                score += 3;
            }

            if (!profile.getPreferredSizes().isEmpty()
                    && profile.getPreferredSizes().stream().anyMatch(size -> product.availableSizes().contains(size))) {
                score += 2;
            }

            if (!profile.getPreferredColors().isEmpty()
                    && profile.getPreferredColors().stream().anyMatch(color ->
                    product.name().toLowerCase(Locale.ROOT).contains(color.toLowerCase(Locale.ROOT)))) {
                score += 1;
            }

            if (!requestedKeywords.isEmpty() && matchesRequestedKeywords(product, requestedKeywords)) {
                score += 5;
            }

            scoreMap.put(product.id(), score);
        }

        return products.stream()
                .sorted(Comparator
                        .comparingInt((ProductCard p) -> scoreMap.getOrDefault(p.id(), 0)).reversed()
                        .thenComparing(ProductCard::name))
                .toList();
    }

    private List<String> buildSeasonQueries(String message, ChatSession.PreferenceProfile profile, Set<String> requestedKeywords) {
        String normalized = normalize(message);
        LinkedHashSet<String> queries = new LinkedHashSet<>();

        for (String keyword : requestedKeywords) {
            queries.add(resolveQueryFromKeyword(keyword));
        }

        if (normalized.contains("he")) {
            if (requestedKeywords.isEmpty()) {
                queries.add("áo phông");
                queries.add("váy");
            }
            queries.add("linen");
        }
        if (normalized.contains("dong")) {
            if (requestedKeywords.isEmpty()) {
                queries.add("áo khoác");
                queries.add("jeans");
            }
            queries.add("len");
        }
        if (normalized.contains("thu")) {
            if (requestedKeywords.isEmpty()) {
                queries.add("áo sơ mi");
            }
            queries.add("quần jeans");
        }
        if (normalized.contains("xuan")) {
            if (requestedKeywords.isEmpty()) {
                queries.add("áo sơ mi");
                queries.add("chân váy");
            }
        }
        if (normalized.contains("di lam") || normalized.contains("cong so")) {
            queries.add("áo sơ mi");
            queries.add("quần");
        }
        if (normalized.contains("di tiec") || normalized.contains("su kien")) {
            queries.add("đầm");
            queries.add("áo khoác");
        }

        if (queries.isEmpty() && !profile.getPreferredCategories().isEmpty()) {
            queries.addAll(profile.getPreferredCategories());
        }
        if (queries.isEmpty()) {
            queries.add("áo");
            queries.add("quần");
        }
        return new ArrayList<>(queries);
    }

    private Set<String> extractRequestedKeywords(String normalizedMessage) {
        LinkedHashSet<String> keywords = new LinkedHashSet<>();
        if (normalizedMessage.contains("ao so mi")) keywords.add("ao so mi");
        if (normalizedMessage.contains("ao phong") || normalizedMessage.contains("tshirt") || normalizedMessage.contains("thun")) keywords.add("ao phong");
        if (normalizedMessage.contains("polo")) keywords.add("polo");
        if (normalizedMessage.contains("ao khoac") || normalizedMessage.contains("jacket") || normalizedMessage.contains("blazer")) keywords.add("ao khoac");
        if (normalizedMessage.contains("hoodie")) keywords.add("hoodie");
        if (normalizedMessage.contains("quan jean") || normalizedMessage.contains("jeans")) keywords.add("jeans");
        if (normalizedMessage.contains("quan tay") || normalizedMessage.contains("trouser")) keywords.add("quan tay");
        if (normalizedMessage.contains("chan vay") || normalizedMessage.contains("vay")) keywords.add("chan vay");
        if (normalizedMessage.contains("dam")) keywords.add("dam");
        return keywords;
    }

    private String resolveQueryFromKeyword(String keyword) {
        return switch (keyword) {
            case "ao so mi" -> "áo sơ mi";
            case "ao phong" -> "áo phông";
            case "polo" -> "polo";
            case "ao khoac" -> "áo khoác";
            case "hoodie" -> "hoodie";
            case "jeans" -> "jeans";
            case "quan tay" -> "quần tây";
            case "chan vay" -> "chân váy";
            case "dam" -> "đầm";
            default -> keyword;
        };
    }

    private boolean matchesRequestedKeywords(ProductCard product, Set<String> requestedKeywords) {
        if (requestedKeywords.isEmpty()) {
            return false;
        }

        String combined = normalize(product.name() + " " + product.category());
        for (String keyword : requestedKeywords) {
            if (combined.contains(keyword) || combined.contains(normalize(resolveQueryFromKeyword(keyword)))) {
                return true;
            }
        }

        return false;
    }

    private ChatResponse.ProductSuggestion toSuggestion(ProductCard product, String reason) {
        return ChatResponse.ProductSuggestion.builder()
                .productId(product.id())
                .name(product.name())
                .category(product.category())
                .imageUrl(product.imageUrl())
                .link(product.link())
                .price(product.price())
                .availableSizes(product.availableSizes())
                .reason(reason)
                .build();
    }

    private ChatResponse.PromotionSuggestion toPromotionSuggestion(PromotionCard promotion) {
        return ChatResponse.PromotionSuggestion.builder()
                .code(promotion.code())
                .discountType(promotion.discountType())
                .discountValue(promotion.discountValue())
                .minOrderAmount(promotion.minOrderAmount())
                .endDate(promotion.endDate())
                .note("Khuyến mãi đang hiệu lực theo dữ liệu thực tế")
                .build();
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
            log.warn("Use in-memory chat session because MongoDB is unavailable: {}", ex.getMessage());
        }

        return ChatSession.builder()
                .sessionId(sessionId)
                .userId(userId)
                .startedAt(Instant.now())
                .endedAt(Instant.now())
                .messages(new ArrayList<>())
                .preferenceProfile(ChatSession.PreferenceProfile.empty())
                .build();
    }

    private void mergePreferences(ChatSession session, ChatRequest.UserPreferences preferences) {
        if (preferences == null) {
            return;
        }

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

    private void updateProfileFromMessage(ChatSession.PreferenceProfile profile, String message) {
        String normalized = normalize(message);

        Matcher sizeMatcher = SIZE_PATTERN.matcher(normalized);
        if (sizeMatcher.find()) {
            String size = sizeMatcher.group(1).toUpperCase(Locale.ROOT);
            profile.getPreferredSizes().add(size);
        }

        if (normalized.contains("xanh") || normalized.contains("blue")) profile.getPreferredColors().add("xanh");
        if (normalized.contains("den") || normalized.contains("black")) profile.getPreferredColors().add("đen");
        if (normalized.contains("trang") || normalized.contains("white")) profile.getPreferredColors().add("trắng");

        if (normalized.contains("ao")) profile.getPreferredCategories().add("áo");
        if (normalized.contains("quan")) profile.getPreferredCategories().add("quần");
        if (normalized.contains("vay")) profile.getPreferredCategories().add("váy");

        if (normalized.contains("than thien") || normalized.contains("friendly")) {
            profile.setPreferredTone("Friendly");
        }
        if (normalized.contains("chuyen nghiep") || normalized.contains("professional")) {
            profile.setPreferredTone("Professional");
        }

        if (normalized.contains("sustain") || normalized.contains("ben vung")) {
            profile.getFocusTags().add("Sustainability");
        }
        if (normalized.contains("fit") || normalized.contains("silhouette")) {
            profile.getFocusTags().add("Silhouette & Fit");
        }
    }

    private void hydrateProfileFromPurchaseHistory(ChatSession.PreferenceProfile profile, String userId) {
        if (userId == null || userId.startsWith("guest-")) {
            return;
        }

        try {
            Map<String, Object> pageData = webClient.get()
                    .uri(orderServiceUrl + "/api/v1/orders?page=0&size=20")
                    .headers(headers -> headers.add("X-User-Id", userId))
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (pageData == null) {
                return;
            }

            for (Object orderObj : toList(pageData.get("content"))) {
                Map<String, Object> order = toMap(orderObj);
                for (Object itemObj : toList(order.get("items"))) {
                    Map<String, Object> item = toMap(itemObj);

                    String size = asString(item.get("size"));
                    if (!size.isBlank()) {
                        profile.getPreferredSizes().add(size.toUpperCase(Locale.ROOT));
                    }

                    String color = asString(item.get("color"));
                    if (!color.isBlank()) {
                        profile.getPreferredColors().add(color);
                    }
                }
            }
        } catch (Exception ex) {
            log.warn("Unable to hydrate profile from order history: {}", ex.getMessage());
        }
    }

    private List<ProductCard> searchProducts(String search, int size) {
        try {
            Map<String, Object> payload = webClient.get()
                .uri(productServiceUrl + "/api/v1/products?search={search}&page={page}&size={size}&sortBy={sortBy}&sortDir={sortDir}",
                    search, 0, size, "createdAt", "desc")
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            return mapProductPage(payload);
        } catch (Exception ex) {
            log.warn("searchProducts failed: {}", ex.getMessage());
            return List.of();
        }
    }

    private List<ProductCard> getFeaturedProducts(int size) {
        try {
            Map<String, Object> payload = webClient.get()
                .uri(productServiceUrl + "/api/v1/products/featured?page={page}&size={size}", 0, size)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            return mapProductPage(payload);
        } catch (Exception ex) {
            log.warn("getFeaturedProducts failed: {}", ex.getMessage());
            return List.of();
        }
    }

    private List<PromotionCard> getActivePromotions() {
        try {
            Object payload = webClient.get()
                    .uri(promotionServiceUrl + "/api/v1/promotions/active")
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .bodyToMono(Object.class)
                    .block();

            List<PromotionCard> cards = new ArrayList<>();
            for (Object itemObj : toList(payload)) {
                Map<String, Object> item = toMap(itemObj);
                cards.add(new PromotionCard(
                        asString(item.get("code")),
                        asString(item.get("discountType")),
                        asString(item.get("discountValue")),
                        asString(item.get("minOrderAmount")),
                        asString(item.get("endDate"))
                ));
            }
            return cards;
        } catch (Exception ex) {
            log.warn("getActivePromotions failed: {}", ex.getMessage());
            return List.of();
        }
    }

    private List<ProductCard> mapProductPage(Map<String, Object> payload) {
        if (payload == null) {
            return List.of();
        }

        List<ProductCard> cards = new ArrayList<>();
        for (Object productObj : toList(payload.get("content"))) {
            Map<String, Object> product = toMap(productObj);
            String productId = asString(product.get("id"));
            String name = asString(product.get("name"));
            String category = asString(product.get("categoryName"));

            BigDecimal minPrice = null;
            String imageUrl = "";
            String link = "";
            Set<String> availableSizes = new LinkedHashSet<>();

            for (Object variantObj : toList(product.get("variants"))) {
                Map<String, Object> variant = toMap(variantObj);

                BigDecimal variantPrice = toBigDecimal(variant.get("price"));
                if (variantPrice != null && (minPrice == null || variantPrice.compareTo(minPrice) < 0)) {
                    minPrice = variantPrice;
                }

                if (link.isBlank()) {
                    link = asString(variant.get("productUrl"));
                }

                if (imageUrl.isBlank()) {
                    List<Object> images = toList(variant.get("images"));
                    if (!images.isEmpty()) {
                        imageUrl = asString(toMap(images.get(0)).get("imageUrl"));
                    }
                }

                for (Object sizeObj : toList(variant.get("sizes"))) {
                    Map<String, Object> sizeItem = toMap(sizeObj);
                    int quantity = toInt(sizeItem.get("quantity"));
                    String status = asString(sizeItem.get("status"));
                    if (quantity > 0 && !"hết hàng".equalsIgnoreCase(status)) {
                        availableSizes.add(asString(sizeItem.get("sizeName")).toUpperCase(Locale.ROOT));
                    }
                }
            }

            if (link.isBlank()) {
                link = "/products/" + productId;
            }

            cards.add(new ProductCard(
                    productId,
                    name,
                    category,
                    imageUrl,
                    link,
                    formatMoney(minPrice),
                    new ArrayList<>(availableSizes)
            ));
        }
        return cards;
    }

    private String formatMoney(BigDecimal value) {
        if (value == null) {
            return "";
        }
        DecimalFormat format = (DecimalFormat) DecimalFormat.getInstance(Locale.forLanguageTag("vi-VN"));
        format.setMaximumFractionDigits(0);
        format.setGroupingUsed(true);
        return format.format(value.setScale(0, RoundingMode.HALF_UP)) + " đ";
    }

    private Measurements extractMeasurements(String message) {
        String normalized = normalize(message);

        Integer heightCm = null;
        Matcher mHeightCm = HEIGHT_CM_PATTERN.matcher(normalized);
        if (mHeightCm.find()) {
            heightCm = toIntNullable(mHeightCm.group(1));
        }
        if (heightCm == null) {
            Matcher mHeightMeter = HEIGHT_M_PATTERN.matcher(normalized);
            if (mHeightMeter.find()) {
                heightCm = Integer.parseInt(mHeightMeter.group(1)) * 100 + Integer.parseInt(mHeightMeter.group(2));
            }
        }

        Integer weightKg = findNumber(WEIGHT_PATTERN, normalized, 1);
        Integer chestCm = findNumber(CHEST_PATTERN, normalized, 2);
        Integer waistCm = findNumber(WAIST_PATTERN, normalized, 2);
        Integer hipCm = findNumber(HIP_PATTERN, normalized, 2);

        return new Measurements(heightCm, weightKg, chestCm, waistCm, hipCm);
    }

    private Integer findNumber(Pattern pattern, String text, int group) {
        Matcher matcher = pattern.matcher(text);
        if (!matcher.find()) {
            return null;
        }
        return toIntNullable(matcher.group(group));
    }

    private String suggestSize(Measurements measurements, GarmentType garmentType) {
        int index;

        if (measurements.weightKg >= 75 || measurements.heightCm >= 178) {
            index = 4;
        } else if (measurements.weightKg >= 67 || measurements.heightCm >= 172) {
            index = 3;
        } else if (measurements.weightKg <= 50 || measurements.heightCm <= 158) {
            index = 1;
        } else {
            index = 2;
        }

        if (garmentType == GarmentType.TOP && measurements.chestCm >= 100) {
            index++;
        }

        if (garmentType == GarmentType.BOTTOM) {
            if (measurements.waistCm >= 86 || measurements.hipCm >= 102) {
                index++;
            } else if (measurements.waistCm <= 70 && measurements.hipCm <= 90) {
                index--;
            }
        }

        index = Math.max(0, Math.min(index, SIZE_ORDER.size() - 1));
        return SIZE_ORDER.get(index);
    }

    private GarmentType detectGarmentType(String message) {
        String normalized = normalize(message);
        if (normalized.contains("quan") || normalized.contains("jean") || normalized.contains("chan vay") || normalized.contains("vay")) {
            return GarmentType.BOTTOM;
        }
        return GarmentType.TOP;
    }

    private String resolveUserId(String userIdHeader, String sessionId) {
        if (userIdHeader != null && !userIdHeader.isBlank()) {
            return userIdHeader.trim();
        }
        return "guest-" + sessionId.substring(0, Math.min(12, sessionId.length()));
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        String lower = value.toLowerCase(Locale.ROOT)
                .replace('đ', 'd')
                .replaceAll("[áàảãạâấầẩẫậăắằẳẵặ]", "a")
                .replaceAll("[éèẻẽẹêếềểễệ]", "e")
                .replaceAll("[íìỉĩị]", "i")
                .replaceAll("[óòỏõọôốồổỗộơớờởỡợ]", "o")
                .replaceAll("[úùủũụưứừửữự]", "u")
                .replaceAll("[ýỳỷỹỵ]", "y");

        return lower.replaceAll("[^a-z0-9\\s]", " ").replaceAll("\\s+", " ").trim();
    }

    private List<Object> toList(Object value) {
        if (value instanceof List<?> list) {
            return new ArrayList<>(list);
        }
        if (value instanceof Collection<?> collection) {
            return new ArrayList<>(collection);
        }
        return List.of();
    }

    private Map<String, Object> toMap(Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> output = new HashMap<>();
            map.forEach((key, val) -> output.put(String.valueOf(key), val));
            return output;
        }
        return Map.of();
    }

    private String asString(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private Integer toIntNullable(String value) {
        try {
            return Integer.parseInt(value);
        } catch (Exception ex) {
            return null;
        }
    }

    private int toInt(Object value) {
        if (value == null) {
            return 0;
        }
        try {
            if (value instanceof Number number) {
                return number.intValue();
            }
            return Integer.parseInt(String.valueOf(value));
        } catch (Exception ex) {
            return 0;
        }
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return null;
        }
        try {
            if (value instanceof BigDecimal decimal) {
                return decimal;
            }
            if (value instanceof Number number) {
                return BigDecimal.valueOf(number.doubleValue());
            }
            return new BigDecimal(String.valueOf(value));
        } catch (Exception ex) {
            return null;
        }
    }

    private record Measurements(Integer heightCm, Integer weightKg, Integer chestCm, Integer waistCm, Integer hipCm) {
    }

    private enum GarmentType {
        TOP,
        BOTTOM
    }

    private record ProductCard(
            String id,
            String name,
            String category,
            String imageUrl,
            String link,
            String price,
            List<String> availableSizes
    ) {
        private ProductCard {
            Objects.requireNonNullElse(category, "");
        }
    }

    private record PromotionCard(
            String code,
            String discountType,
            String discountValue,
            String minOrderAmount,
            String endDate
    ) {
    }

    private record RouteResult(
            String reply,
            List<String> missingFields,
            List<ChatResponse.ProductSuggestion> suggestions,
            List<ChatResponse.PromotionSuggestion> promotions
    ) {
    }
}
