package com.fashion.chatbotservice.agent;

import com.fashion.chatbotservice.dto.ChatResponse;
import com.fashion.chatbotservice.model.ChatSession;
import com.fashion.chatbotservice.service.KnowledgeBaseService;
import com.fashion.chatbotservice.service.OutfitRuleEngine;
import com.fashion.chatbotservice.service.ProductRecommendationService;
import com.fashion.chatbotservice.service.ProductTaxonomyService;
import com.fashion.chatbotservice.service.SizeAdvisorService;
import com.fashion.chatbotservice.service.SizeFitAdvisoryService;
import com.fashion.chatbotservice.util.VietnameseNormalizer;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.function.Supplier;

/**
 * Tất cả @Tool methods mà LangChain4j agent có thể gọi.
 * Mỗi tool thực hiện dual output:
 * 1. Trả text tóm tắt cho LLM (để tổng hợp câu trả lời)
 * 2. Lưu structured data vào ToolResultCollector (để frontend render)
 */
@Component
@Slf4j
public class FashionTools {

    private static final Duration TOOL_TIMEOUT = Duration.ofSeconds(10);

    private final WebClient webClient;
    private final SizeAdvisorService sizeAdvisorService;
    private final OutfitRuleEngine outfitRuleEngine;
    private final KnowledgeBaseService knowledgeBaseService;
    private final ProductRecommendationService productRecommendationService;
    private final ProductTaxonomyService productTaxonomyService;
    private final SizeFitAdvisoryService sizeFitAdvisoryService;
    private CircuitBreakerRegistry circuitBreakerRegistry = CircuitBreakerRegistry.ofDefaults();
    private RetryRegistry retryRegistry = RetryRegistry.ofDefaults();
    private TimeLimiterRegistry timeLimiterRegistry = TimeLimiterRegistry.ofDefaults();
    private BulkheadRegistry bulkheadRegistry = BulkheadRegistry.ofDefaults();
    private ExecutorService resilienceExecutorService;

    @Value("${chatbot.product-service-url:http://localhost:8080}")
    private String productServiceUrl;

    @Value("${chatbot.promotion-service-url:http://localhost:8080}")
    private String promotionServiceUrl;

    @Value("${chatbot.order-service-url:http://localhost:8080}")
    private String orderServiceUrl;

    @Value("${chatbot.review-service-url:http://localhost:8088}")
    private String reviewServiceUrl;

    private List<String> buildSearchCandidates(String search, boolean allowFallback) {
        String original = search == null ? "" : search.trim();
        if (original.isBlank()) return List.of();

        if (!allowFallback) {
            return List.of(original);
        }

        Set<String> candidates = new LinkedHashSet<>();
        candidates.add(original);

        String normalized = normalizeText(original);
        if (!normalized.isBlank() && !normalized.equalsIgnoreCase(original)) {
            candidates.add(normalized);
        }

        candidates.addAll(expandSearchSynonyms(original));

        for (String fallback : generateFallbackKeywords(original)) {
            if (fallback == null || fallback.isBlank()) continue;
            candidates.add(fallback);
            String normalizedFallback = normalizeText(fallback);
            if (!normalizedFallback.isBlank() && !normalizedFallback.equalsIgnoreCase(fallback)) {
                candidates.add(normalizedFallback);
            }
            candidates.addAll(expandSearchSynonyms(fallback));
        }

        return candidates.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .filter(value -> value.length() >= 2)
                .limit(12)
                .toList();
    }

    private List<String> expandSearchSynonyms(String keyword) {
        if (keyword == null || keyword.isBlank()) return List.of();
        String normalized = normalizeText(keyword);
        Set<String> synonyms = new LinkedHashSet<>();

        if (normalized.contains("dam") || normalized.contains("vay") || normalized.contains("dress")) {
            synonyms.add("dam");
            synonyms.add("vay");
            synonyms.add("dress");
        }
        if (normalized.contains("ao khoac") || normalized.contains("jacket") || normalized.contains("blazer")) {
            synonyms.add("ao khoac");
            synonyms.add("jacket");
            synonyms.add("blazer");
        }
        if (normalized.contains("ao thun") || normalized.contains("ao phong")
                || normalized.contains("t shirt") || normalized.contains("tee")) {
            synonyms.add("ao thun");
            synonyms.add("ao phong");
            synonyms.add("t-shirt");
        }
        if (normalized.contains("ao so mi") || normalized.contains("shirt")) {
            synonyms.add("ao so mi");
            synonyms.add("shirt");
        }
        if (normalized.contains("quan jean") || normalized.contains("jean")
                || normalized.contains("jeans") || normalized.contains("denim")) {
            synonyms.add("quan jean");
            synonyms.add("jeans");
            synonyms.add("denim");
        }
        if (normalized.contains("chan vay") || normalized.contains("skirt")) {
            synonyms.add("chan vay");
            synonyms.add("skirt");
        }

        return new ArrayList<>(synonyms);
    }

    private void appendUniqueSuggestions(List<ChatResponse.ProductSuggestion> base,
                                         List<ChatResponse.ProductSuggestion> incoming,
                                         Set<String> seenKeys,
                                         int limit) {
        if (incoming == null || incoming.isEmpty()) return;

        for (ChatResponse.ProductSuggestion suggestion : incoming) {
            if (suggestion == null) continue;
            String key = buildSuggestionKey(suggestion);
            if (!seenKeys.add(key)) continue;
            base.add(suggestion);
            if (base.size() >= limit) break;
        }
    }

    private String buildSuggestionKey(ChatResponse.ProductSuggestion suggestion) {
        if (suggestion == null) return "";
        String productId = suggestion.getProductId();
        if (productId != null && !productId.isBlank()) return productId.trim();
        return (stringValue(suggestion.getName()) + "|" + stringValue(suggestion.getPrice())).toLowerCase(Locale.ROOT);
    }

    private List<ChatResponse.ProductSuggestion> executeSemanticBrowseFallback(String search,
                                                                               Long minPrice,
                                                                               Long maxPrice,
                                                                               int maxPages,
                                                                               int pageSize) {
        List<ChatResponse.ProductSuggestion> pooled = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();

        for (int page = 0; page < Math.max(1, maxPages); page++) {
            List<ChatResponse.ProductSuggestion> pageItems = executeBrowse(minPrice, maxPrice, pageSize, page);
            appendUniqueSuggestions(pooled, pageItems, seen, pageSize * Math.max(1, maxPages));
            if (pageItems.size() < pageSize) {
                break;
            }
        }

        if (pooled.isEmpty()) return List.of();
        return filterSemanticMatches(pooled, search, 12);
    }

    private List<ChatResponse.ProductSuggestion> filterSemanticMatches(List<ChatResponse.ProductSuggestion> items,
                                                                       String search,
                                                                       int limit) {
        if (items == null || items.isEmpty()) return List.of();

        List<String> tokens = buildSemanticTokens(search);
        if (tokens.isEmpty()) return List.of();

        List<SemanticMatch> matches = new ArrayList<>();
        int index = 0;

        for (ChatResponse.ProductSuggestion suggestion : items) {
            String haystack = normalizeText(stringValue(suggestion.getName()) + " " + stringValue(suggestion.getCategory()));
            if (haystack.isBlank()) {
                index++;
                continue;
            }

            int score = 0;
            for (String token : tokens) {
                if (token.isBlank()) continue;
                if (haystack.contains(token)) {
                    score += (token.length() >= 4 ? 2 : 1);
                }
            }

            if (score > 0) {
                matches.add(new SemanticMatch(suggestion, score, index));
            }
            index++;
        }

        return matches.stream()
                .sorted(Comparator.comparingInt(SemanticMatch::score).reversed()
                        .thenComparingInt(SemanticMatch::index))
                .map(SemanticMatch::suggestion)
                .limit(limit)
                .toList();
    }

    private List<String> buildSemanticTokens(String search) {
        String normalized = normalizeText(search);
        if (normalized.isBlank()) return List.of();

        Set<String> tokens = new LinkedHashSet<>();
        for (String token : normalized.split("\\s+")) {
            if (token.length() < 2) continue;
            if (Set.of("tim", "kiem", "mau", "size", "cho", "minh", "toi", "voi", "gia", "duoi", "tren").contains(token)) {
                continue;
            }
            tokens.add(token);
        }

        tokens.addAll(expandSearchSynonyms(search).stream()
                .map(this::normalizeText)
                .toList());

        return new ArrayList<>(tokens);
    }

    /**
     * ThreadLocal để đảm bảo thread-safety khi nhiều request đồng thời.
     * Mỗi thread (request) sẽ có collector riêng, tránh race condition.
     */
    private final ThreadLocal<ToolResultCollector> collectorHolder = new ThreadLocal<>();

    /**
     * ThreadLocal lưu thông tin cá nhân hóa hiện tại (nếu có).
     */
    private final ThreadLocal<ChatSession.PreferenceProfile> preferenceHolder = new ThreadLocal<>();

    /**
     * User context của phiên hiện tại để gọi các protected endpoint theo đúng ownership.
     */
    private final ThreadLocal<String> currentUserIdHolder = new ThreadLocal<>();

    public FashionTools(WebClient webClient,
                        SizeAdvisorService sizeAdvisorService,
                        OutfitRuleEngine outfitRuleEngine,
                        KnowledgeBaseService knowledgeBaseService,
                        ProductRecommendationService productRecommendationService,
                        ProductTaxonomyService productTaxonomyService,
                        SizeFitAdvisoryService sizeFitAdvisoryService) {
        this.webClient = webClient;
        this.sizeAdvisorService = sizeAdvisorService;
        this.outfitRuleEngine = outfitRuleEngine;
        this.knowledgeBaseService = knowledgeBaseService;
        this.productRecommendationService = productRecommendationService;
        this.productTaxonomyService = productTaxonomyService;
        this.sizeFitAdvisoryService = sizeFitAdvisoryService;
    }

    public void setCollector(ToolResultCollector collector) {
        this.collectorHolder.set(collector);
    }

    public void clearCollector() {
        this.collectorHolder.remove();
        this.preferenceHolder.remove();
        this.currentUserIdHolder.remove();
    }

    public void setPreferenceProfile(ChatSession.PreferenceProfile profile) {
        this.preferenceHolder.set(profile);
    }

    public void setCurrentUserId(String userId) {
        this.currentUserIdHolder.set(userId);
    }

    @Autowired(required = false)
    public void setCircuitBreakerRegistry(CircuitBreakerRegistry circuitBreakerRegistry) {
        if (circuitBreakerRegistry != null) {
            this.circuitBreakerRegistry = circuitBreakerRegistry;
        }
    }

    @Autowired(required = false)
    public void setRetryRegistry(RetryRegistry retryRegistry) {
        if (retryRegistry != null) {
            this.retryRegistry = retryRegistry;
        }
    }

    @Autowired(required = false)
    public void setTimeLimiterRegistry(TimeLimiterRegistry timeLimiterRegistry) {
        if (timeLimiterRegistry != null) {
            this.timeLimiterRegistry = timeLimiterRegistry;
        }
    }

    @Autowired(required = false)
    public void setBulkheadRegistry(BulkheadRegistry bulkheadRegistry) {
        if (bulkheadRegistry != null) {
            this.bulkheadRegistry = bulkheadRegistry;
        }
    }

    @Autowired(required = false)
    public void setResilienceExecutorService(ExecutorService resilienceExecutorService) {
        this.resilienceExecutorService = resilienceExecutorService;
    }

    private ToolResultCollector collector() {
        return collectorHolder.get();
    }

    private ChatSession.PreferenceProfile preferenceProfile() {
        return preferenceHolder.get();
    }

    private String currentUserId() {
        return currentUserIdHolder.get();
    }

    private boolean isGuestUser(String userId) {
        return userId == null || userId.isBlank() || userId.startsWith("guest-");
    }

    private void markToolFailure() {
        if (collector() != null) {
            collector().markToolFailure();
        }
    }

    private <T> T executeResilient(String backendName, Supplier<T> supplier) throws Exception {
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(backendName);
        Retry retry = retryRegistry.retry(backendName);
        TimeLimiter timeLimiter = timeLimiterRegistry.timeLimiter(backendName);
        Bulkhead bulkhead = bulkheadRegistry.bulkhead(backendName);

        java.util.concurrent.Callable<T> guardedCallable =
                Retry.decorateCallable(retry,
                        CircuitBreaker.decorateCallable(circuitBreaker,
                                Bulkhead.decorateCallable(bulkhead, supplier::get)));

        ExecutorService executor = resilienceExecutorService;
        if (executor == null) {
            return guardedCallable.call();
        }

        Future<T> future = executor.submit(guardedCallable);
        try {
            return timeLimiter.executeFutureSupplier(() -> future);
        } catch (CallNotPermittedException | BulkheadFullException ex) {
            future.cancel(true);
            throw ex;
        } catch (Exception ex) {
            future.cancel(true);
            throw ex;
        }
    }

    // ========== PRODUCT TOOLS ==========

    @Tool(name = "searchProducts", value = """
            Tìm kiếm sản phẩm thời trang theo từ khóa, mức giá.
            Gọi khi người dùng hỏi về sản phẩm, giá cả, màu sắc, chất liệu, tồn kho.
            VD: 'áo sơ mi đen', 'quần jean dưới 500k', 'váy mùa hè'.

            CÁCH TÁCH TỪ KHÓA — RẤT QUAN TRỌNG:
            - search: CHỈ chứa TÊN LOẠI SẢN PHẨM, KHÔNG gộp màu sắc/chất liệu.
              VD: 'áo thun nam màu đen' → search='áo thun', color='đen'
              VD: 'quần jean nữ xanh' → search='quần jean', color='xanh'
              VD: 'váy đầm dự tiệc' → search='váy đầm', color=null
            - color: tách riêng thông tin màu sắc nếu có
            - Nếu user nói 'nam' hoặc 'nữ', BỎ QUA từ này trong search (hệ thống không phân biệt giới tính theo tên)

            QUY ĐỔI GIÁ TIẾNG VIỆT sang VND:
            - "500k" = 500000, "300k" = 300000
            - "2 triệu" hoặc "2tr" = 2000000
            - "dưới 500k" → minPrice=null, maxPrice=500000
            - "từ 300 đến 500k" → minPrice=300000, maxPrice=500000
            - "dưới 2 triệu" → minPrice=null, maxPrice=2000000

            CRITICAL: Extract the search keyword ONLY from what the user actually said.
            Do NOT invent product names or add details the user did not mention.
            """)
    public String searchProductsTool(
            @P("Từ khóa tìm kiếm: CHỈ tên loại đồ (VD: 'quần jean', 'áo thun', 'váy đầm'). KHÔNG gộp màu/giá/giới tính") String search,
            @P("Giá tối thiểu (VND), null nếu không giới hạn. VD: 300k=300000") String minPrice,
            @P("Giá tối đa (VND), null nếu không giới hạn. VD: 500k=500000, 2 triệu=2000000") String maxPrice,
            @P("Màu sắc sản phẩm nếu user có đề cập (VD: 'đen', 'trắng', 'xanh'). null nếu không đề cập.") String color,
            @P("Size nếu user có đề cập (VD: 'S', 'M', 'L', 'XL'). null nếu không đề cập.") String size) {
        return searchProductsInternal(search, parseLongSafe(minPrice), parseLongSafe(maxPrice), color, size, true);
    }

    public String searchProducts(
            String search,
            Long minPrice,
            Long maxPrice,
            String color,
            String size) {
        return searchProductsInternal(search, minPrice, maxPrice, color, size, true);
    }

    @Tool(name = "searchProductsStrict", value = """
            Tìm kiếm sản phẩm theo TỪ KHÓA CỤ THỂ, KHÔNG fallback rút gọn.
            Dùng khi user hỏi có mẫu X hay không, hoặc muốn kiểm tra tên sản phẩm cụ thể.

            CRITICAL:
            - Không tự động rút gọn từ khóa.
            - Nếu không có kết quả, phải trả lời rõ ràng là không có sản phẩm đó trong hệ thống.
            """)
    public String searchProductsStrictTool(
            @P("Từ khóa tìm kiếm: càng gần với tên sản phẩm càng tốt") String search,
            @P("Giá tối thiểu (VND), null nếu không giới hạn") String minPrice,
            @P("Giá tối đa (VND), null nếu không giới hạn") String maxPrice,
            @P("Màu sắc nếu user đề cập (VD: 'đen', 'trắng'). null nếu không đề cập.") String color,
            @P("Size nếu user có đề cập (VD: 'S', 'M', 'L', 'XL'). null nếu không đề cập.") String size) {
        return searchProductsInternal(search, parseLongSafe(minPrice), parseLongSafe(maxPrice), color, size, false);
    }

    public String searchProductsStrict(
            String search,
            Long minPrice,
            Long maxPrice,
            String color,
            String size) {
        return searchProductsInternal(search, minPrice, maxPrice, color, size, false);
    }

    /**
     * Fetches canonical product detail by product id. This is used for follow-up
     * questions after the user explicitly selects a card, so later turns bind to
     * a concrete product instead of relying on fuzzy text matching.
     */
    public String getProductDetail(String productId) {
        try {
            log.info("Tool: getProductDetail(productId={})", productId);
            @SuppressWarnings("unchecked")
            Map<String, Object> payload = executeResilient("product-service", () -> webClient.get()
                    .uri(productServiceUrl + "/api/v1/products/" + productId)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(TOOL_TIMEOUT)
                    .block());

            if (payload == null || payload.isEmpty()) {
                markToolFailure();
                return "Mình chưa lấy được thông tin chi tiết sản phẩm lúc này.";
            }

            ChatResponse.ProductSuggestion suggestion = mapProductDetail(payload);
            if (suggestion != null && collector() != null) {
                collector().addProducts(List.of(suggestion));
            }

            String name = stringValue(payload.get("name"));
            String description = stringValue(payload.get("description"));
            String category = stringValue(payload.get("categoryName"));
            String price = suggestion != null ? stringValue(suggestion.getPrice()) : "";
            String sizes = suggestion != null ? joinOrFallback(suggestion.getAvailableSizes(), "chưa rõ size") : "chưa rõ size";
            String colors = suggestion != null ? joinOrFallback(suggestion.getAvailableColors(), "chưa rõ màu") : "chưa rõ màu";

            StringBuilder result = new StringBuilder();
            result.append("Thông tin chi tiết sản phẩm ").append(name.isBlank() ? "này" : name).append(":\n");
            if (!category.isBlank()) {
                result.append("- Danh mục: ").append(category).append("\n");
            }
            if (!price.isBlank()) {
                result.append("- Giá: ").append(price).append("\n");
            }
            result.append("- Size còn: ").append(sizes).append("\n");
            result.append("- Màu sắc: ").append(colors).append("\n");
            if (!description.isBlank()) {
                result.append("- Mô tả: ").append(description);
            }
            return result.toString().trim();
        } catch (Throwable ex) {
            log.error("getProductDetail failed", ex);
            markToolFailure();
            return "Mình chưa thể lấy thông tin chi tiết sản phẩm lúc này. Bạn thử lại sau nhé!";
        }
    }

    @Tool(name = "browseProducts", value = """
            Duyệt danh sách sản phẩm để tư vấn khi user không nêu rõ từ khóa cụ thể.
            Dùng cho các câu như: "tư vấn áo khoác", "gợi ý đồ đi làm", "có mẫu nào phù hợp".

            CRITICAL:
            - Không yêu cầu từ khóa bắt buộc. Hãy dùng filter giá/size/màu nếu có.
            - Nếu không có sản phẩm phù hợp, phải nói rõ là không có.
            """)
    public String browseProductsTool(
            @P("Giá tối thiểu (VND), null nếu không giới hạn") String minPriceStr,
            @P("Giá tối đa (VND), null nếu không giới hạn") String maxPriceStr,
            @P("Màu sắc nếu user có đề cập (VD: 'đen', 'trắng'). null nếu không đề cập.") String color,
            @P("Size nếu user có đề cập (VD: 'S', 'M', 'L', 'XL'). null nếu không đề cập.") String size) {
        return browseProducts(parseLongSafe(minPriceStr), parseLongSafe(maxPriceStr), color, size);
    }

    public String browseProducts(
            Long minPrice,
            Long maxPrice,
            String color,
            String size) {
        try {
            log.info("Tool: browseProducts(min={}, max={}, color={}, size={})", minPrice, maxPrice, color, size);
            List<ChatResponse.ProductSuggestion> suggestions = executeBrowse(minPrice, maxPrice, 12);
            suggestions = productRecommendationService.rankSuggestions(
                    suggestions,
                    preferenceProfile(),
                    null,
                    minPrice,
                    maxPrice,
                    color,
                    size
            );
            suggestions = applyColorFilter(suggestions, color);
            suggestions = applySizeFilter(suggestions, size);
            suggestions = productRecommendationService.diversifySuggestionsByCategory(suggestions, 12);

            if (collector() != null) collector().addProducts(suggestions);

            if (suggestions.isEmpty()) {
                return "Hiện chưa có sản phẩm phù hợp với yêu cầu này.";
            }

            return buildConciseSuggestionReply(
                    suggestions,
                    "Mình đã gom sẵn vài mẫu phù hợp để bạn xem nhanh bên dưới.",
                    minPrice,
                    maxPrice,
                    color,
                    size,
                    "Nếu bạn muốn, mình sẽ lọc tiếp theo form, chất liệu hoặc chọn giúp 1-2 mẫu dễ mặc nhất."
            );
        } catch (Throwable ex) {
            log.error("browseProducts failed", ex);
            markToolFailure();
            return "Dịch vụ sản phẩm tạm thời không phản hồi. Vui lòng thử lại sau.";
        }
    }

    @Tool("""
            Liệt kê các loại sản phẩm hiện có trong hệ thống (dựa trên toàn bộ sản phẩm trong database).
            Dùng khi người dùng hỏi: "shop có bán gì", "có những loại nào", "áo/quần/váy loại gì".

            CRITICAL:
            - Chỉ liệt kê loại thực sự có trong DB.
            - Không bịa ra loại sản phẩm không tồn tại.
            """)
    public String listProductTypes(
            @P("Nhóm loại đồ muốn xem (VD: 'áo', 'quần', 'váy', 'phụ kiện'). null nếu muốn xem tất cả")
            String groupHint) {
        try {
            log.info("Tool: listProductTypes(groupHint={})", groupHint);
            List<String> allTypes = collectAllProductTypes(20, 200);
            if (allTypes.isEmpty()) {
                return "Hiện chưa có sản phẩm nào trong hệ thống.";
            }

            String normalizedHint = normalizeText(groupHint);
            List<String> filteredTypes = filterTypesByGroup(allTypes, normalizedHint);

            if (filteredTypes.isEmpty()) {
                return "Hiện chưa có loại sản phẩm phù hợp trong hệ thống. Bạn muốn mình tư vấn nhóm khác không ạ?";
            }

            if (!normalizedHint.isBlank()) {
                String groupLabel = productTaxonomyService.resolveGroupLabel(normalizedHint);
                return "Các loại " + groupLabel + " hiện có: " + String.join(", ", filteredTypes)
                        + ". Bạn muốn mình tư vấn loại nào ạ?";
            }

            return formatGroupedTypeList(filteredTypes);
        } catch (Throwable ex) {
            log.error("listProductTypes failed", ex);
            return "Mình chưa thể lấy danh mục sản phẩm lúc này. Bạn thử lại sau nhé!";
        }
    }

    private String searchProductsInternal(String search,
                                          Long minPrice,
                                          Long maxPrice,
                                          String color,
                                          String size,
                                          boolean allowFallback) {
        if (search == null || search.isBlank()) {
            return "Bạn cho mình biết loại sản phẩm cần tìm nhé (VD: 'áo thun', 'quần jean', 'váy').";
        }

        try {
            log.info("Tool: searchProductsInternal(search={}, allowFallback={})", search, allowFallback);
            List<ChatResponse.ProductSuggestion> suggestions = new ArrayList<>();
            Set<String> seenProductKeys = new LinkedHashSet<>();
            List<String> candidates = buildSearchCandidates(search, allowFallback);

            for (String candidate : candidates) {
                List<ChatResponse.ProductSuggestion> matches = executeSearch(candidate, minPrice, maxPrice);
                appendUniqueSuggestions(suggestions, matches, seenProductKeys, 18);
                if (!matches.isEmpty()) {
                    log.info("Search hit with keyword: '{}', count={}", candidate, matches.size());
                }
                if (suggestions.size() >= 12) break;
            }

            if (allowFallback && suggestions.isEmpty()) {
                List<ChatResponse.ProductSuggestion> semanticMatches =
                        executeSemanticBrowseFallback(search, minPrice, maxPrice, 2, 60);
                appendUniqueSuggestions(suggestions, semanticMatches, seenProductKeys, 18);
            }

            suggestions = productRecommendationService.rankSuggestions(
                    suggestions,
                    preferenceProfile(),
                    search,
                    minPrice,
                    maxPrice,
                    color,
                    size
            );
            suggestions = applyCategoryLock(suggestions, search);
            suggestions = applyGenderGate(suggestions, preferenceProfile(), search);
            suggestions = applyColorFilter(suggestions, color);
            suggestions = applySizeFilter(suggestions, size);
            suggestions = productRecommendationService.diversifySuggestionsByCategory(suggestions, 12);
            if (collector() != null) collector().addProducts(suggestions);

            if (suggestions.isEmpty()) {
                if (allowFallback) {
                    if (color != null && !color.isBlank()) {
                        return "Hiện chưa có sản phẩm màu " + color + " phù hợp với yêu cầu.";
                    }
                    if (size != null && !size.isBlank()) {
                        return "Hiện chưa có sản phẩm phù hợp với size " + size + ". Bạn muốn mình gợi ý mẫu khác không ạ?";
                    }
                    return "Không tìm thấy sản phẩm phù hợp sau khi đã tìm theo từ khóa và quét danh mục hiện có. "
                            + "Bạn có thể mô tả thêm kiểu dáng/màu/chất liệu để mình tìm lại.";
                }
                if (size != null && !size.isBlank()) {
                    return "Hiện chưa có sản phẩm khớp với từ khóa bạn cung cấp và size " + size + ".";
                }
                if (color != null && !color.isBlank()) {
                    return "Hiện chưa có sản phẩm khớp với từ khóa bạn cung cấp và màu " + color + ".";
                }
                return "Hiện chưa có sản phẩm khớp với từ khóa bạn cung cấp trong hệ thống.";
            }

            return buildConciseSuggestionReply(
                    suggestions,
                    "Mình đã chọn sẵn vài mẫu khá sát với nhu cầu của bạn.",
                    minPrice,
                    maxPrice,
                    color,
                    size,
                    "Bạn xem card bên dưới, nếu thích mình có thể so sánh nhanh hoặc lọc tiếp xuống còn 1-2 mẫu."
            );
        } catch (Throwable ex) {
            log.error("searchProductsInternal failed", ex);
            markToolFailure();
            return "Dịch vụ sản phẩm tạm thời không phản hồi. Vui lòng thử lại sau.";
        }
    }

    /**
     * Execute search against product-service API.
     */
    @SuppressWarnings("unchecked")
    private List<ChatResponse.ProductSuggestion> executeSearch(String search, Long minPrice, Long maxPrice) {
        String encodedSearch = search != null
                ? URLEncoder.encode(search, StandardCharsets.UTF_8)
                : "";
        StringBuilder uri = new StringBuilder(productServiceUrl)
                .append("/api/v1/products?search=").append(encodedSearch)
                .append("&page=0&size=5&sortBy=createdAt&sortDir=desc");
        if (minPrice != null) uri.append("&minPrice=").append(minPrice);
        if (maxPrice != null) uri.append("&maxPrice=").append(maxPrice);

        Map<String, Object> payload;
        try {
            payload = executeResilient("product-service", () -> webClient.get()
                    .uri(uri.toString())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(TOOL_TIMEOUT)
                    .block());
        } catch (Exception ex) {
            throw new RuntimeException("Product search failed", ex);
        }

        return mapProductPage(payload);
    }

    /**
     * Generate progressively shorter search keywords for fallback.
     * "áo thun nam" → ["áo thun", "áo"]
     * "quần jean nữ" → ["quần jean", "quần"]
     */
    private List<String> generateFallbackKeywords(String original) {
        // Strip common non-searchable words first
        String cleaned = original.toLowerCase(Locale.ROOT)
                .replaceAll("\\b(nam|nữ|mau|màu|cho|của|cái|chiếc|loại|kiểu)\\b", "")
                .replaceAll("\\s+", " ").trim();

        List<String> fallbacks = new ArrayList<>();
        // If cleaned differs from original, try it first
        if (!cleaned.equalsIgnoreCase(original.trim())) {
            fallbacks.add(cleaned);
        }
        // Progressively remove last word
        String[] words = cleaned.split("\\s+");
        for (int len = words.length - 1; len >= 1; len--) {
            fallbacks.add(String.join(" ", java.util.Arrays.copyOf(words, len)));
        }
        return fallbacks;
    }

    private List<ChatResponse.ProductSuggestion> rankBusinessSuggestions(
            List<ChatResponse.ProductSuggestion> suggestions,
            ChatSession.PreferenceProfile profile,
            String search,
            Long minPrice,
            Long maxPrice,
            String color,
            String size) {
        if (suggestions == null || suggestions.isEmpty()) return suggestions;

        List<ScoredSuggestion> scored = new ArrayList<>();
        Long preferredMaxPrice = (maxPrice != null) ? maxPrice : parseBudget(profile == null ? null : profile.getBudget());
        List<String> searchTokens = buildSemanticTokens(search);
        String normalizedSearch = normalizeText(search);
        String inferredOccasion = inferOccasionContext(normalizedSearch);
        boolean wantsSafeOption = normalizedSearch.contains("an toan")
                || normalizedSearch.contains("de mac")
                || normalizedSearch.contains("de phoi")
                || normalizedSearch.contains("basic");
        boolean wantsStatementOption = normalizedSearch.contains("noi bat")
                || normalizedSearch.contains("co diem nhan")
                || normalizedSearch.contains("statement");

        int index = 0;
        for (ChatResponse.ProductSuggestion suggestion : suggestions) {
            double score = 0.0;
            List<String> reasons = new ArrayList<>();
            String haystack = normalizeText(stringValue(suggestion.getName()) + " " + stringValue(suggestion.getCategory()));
            Set<String> taxonomyLabels = inferTaxonomyLabels(haystack);

            if (!searchTokens.isEmpty()) {
                int tokenHits = 0;
                for (String token : searchTokens) {
                    if (!token.isBlank() && haystack.contains(token)) {
                        tokenHits++;
                    }
                }
                if (tokenHits > 0) {
                    score += Math.min(3.2d, tokenHits * 1.2d);
                    reasons.add("Đúng nhu cầu đang tìm");
                }
            }

            if (!safeList(suggestion.getAvailableSizes()).isEmpty()) {
                score += 0.9d;
            } else {
                score -= 2.5d;
                reasons.add("Cần kiểm tra lại tồn size");
            }

            boolean explicitSizeMatch = size != null && !size.isBlank()
                    && safeList(suggestion.getAvailableSizes()).stream()
                    .anyMatch(avail -> normalizeText(avail).equals(normalizeText(size)));
            if (explicitSizeMatch) {
                score += 2.4d;
                reasons.add("Có đúng size bạn đang cần");
            } else if (profile != null && hasAnyMatch(profile.getPreferredSizes(), suggestion.getAvailableSizes())) {
                score += 1.4d;
                reasons.add("Hợp size bạn hay mặc");
            }

            boolean explicitColorMatch = color != null && !color.isBlank()
                    && safeList(suggestion.getAvailableColors()).stream()
                    .anyMatch(avail -> normalizeText(avail).contains(normalizeText(color)));
            if (explicitColorMatch) {
                score += 2.6d;
                reasons.add("Có đúng tông màu bạn ưu tiên");
            } else if (profile != null && hasAnyMatch(profile.getPreferredColors(), suggestion.getAvailableColors())) {
                score += 1.8d;
                reasons.add("Hợp màu bạn thường thích");
            }

            boolean categoryMatch = profile != null && hasCategoryMatch(profile.getPreferredCategories(), suggestion.getCategory());
            if (categoryMatch) {
                score += 1.4d;
                reasons.add("Đúng nhóm đồ bạn quan tâm");
            }

            if (profile != null
                    && profile.getLastProductCategoryQueried() != null
                    && !profile.getLastProductCategoryQueried().isBlank()
                    && haystack.contains(normalizeText(profile.getLastProductCategoryQueried()))) {
                score += 0.9d;
            }

            Long productPrice = parsePriceToLong(suggestion.getPrice());
            if (productPrice != null) {
                if (minPrice != null && productPrice >= minPrice) {
                    score += 0.5d;
                }
                if (preferredMaxPrice != null) {
                    if (productPrice <= preferredMaxPrice) {
                        score += 1.5d;
                        reasons.add("Nằm trong tầm giá dễ chốt");
                        score += budgetClosenessBonus(productPrice, preferredMaxPrice);
                    } else {
                        score -= 1.0d;
                    }
                }
            }

            double styleScore = scoreStyleFit(profile, haystack);
            if (styleScore > 0.0d) {
                score += styleScore;
                reasons.add("Hợp phong cách đang ưu tiên");
            }

            if (profile != null && profile.getFocusTags() != null) {
                score += scoreFocusTags(profile.getFocusTags(), haystack);
            }

            double occasionScore = scoreOccasionFit(inferredOccasion, taxonomyLabels);
            if (occasionScore > 0.0d) {
                score += occasionScore;
                reasons.add(buildOccasionReason(inferredOccasion));
            }

            if (wantsSafeOption) {
                double safetyScore = scoreSafetyFit(taxonomyLabels);
                if (safetyScore > 0.0d) {
                    score += safetyScore;
                    reasons.add("Dễ mặc và dễ phối");
                }
            }

            if (wantsStatementOption) {
                double statementScore = scoreStatementFit(taxonomyLabels);
                if (statementScore > 0.0d) {
                    score += statementScore;
                    reasons.add("Có điểm nhấn hơn trong outfit");
                }
            }

            suggestion.setReason(buildBusinessReason(reasons));
            scored.add(new ScoredSuggestion(suggestion, score, index++));
        }

        return scored.stream()
                .sorted(Comparator.comparingDouble(ScoredSuggestion::score).reversed()
                        .thenComparingInt(ScoredSuggestion::index))
                .map(ScoredSuggestion::suggestion)
                .toList();
    }

    private boolean hasAnyMatch(Set<String> preferredValues, List<String> actualValues) {
        if (preferredValues == null || preferredValues.isEmpty() || actualValues == null || actualValues.isEmpty()) {
            return false;
        }
        for (String preferred : preferredValues) {
            String normalizedPreferred = normalizeText(preferred);
            for (String actual : actualValues) {
                if (normalizeText(actual).contains(normalizedPreferred)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean hasCategoryMatch(Set<String> preferredCategories, String category) {
        if (preferredCategories == null || preferredCategories.isEmpty() || category == null) return false;
        String normalizedCategory = normalizeText(category);
        for (String preferred : preferredCategories) {
            if (normalizedCategory.contains(normalizeText(preferred))) return true;
        }
        return false;
    }

    private Long parseBudget(String budget) {
        if (budget == null || budget.isBlank()) return null;
        String cleaned = budget.toLowerCase(Locale.ROOT)
                .replaceAll("[^0-9kmtrd.]", " ")
                .trim();
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("([0-9][0-9.,]*)\\s*(k|tr|trieu|d|dong)?")
                .matcher(cleaned);
        Long maxPrice = null;
        while (m.find()) {
            try {
                double value = Double.parseDouble(m.group(1).replace(".", "").replace(",", ""));
                String unit = m.group(2);
                if ("k".equals(unit)) value *= 1_000;
                else if ("tr".equals(unit) || "trieu".equals(unit)) value *= 1_000_000;
                long vnd = Math.round(value);
                if (maxPrice == null || vnd > maxPrice) maxPrice = vnd;
            } catch (NumberFormatException ignored) {}
        }
        return maxPrice;
    }

    private Long parsePriceToLong(String price) {
        if (price == null || price.isBlank()) return null;
        String digits = price.replaceAll("[^0-9]", "");
        if (digits.isBlank()) return null;
        try {
            return Long.parseLong(digits);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String normalizeText(String value) {
        return VietnameseNormalizer.normalize(value == null ? "" : value);
    }

    private double budgetClosenessBonus(Long productPrice, Long preferredMaxPrice) {
        if (productPrice == null || preferredMaxPrice == null || preferredMaxPrice <= 0) {
            return 0.0d;
        }
        double ratio = (double) productPrice / preferredMaxPrice;
        if (ratio >= 0.6d && ratio <= 1.0d) {
            return 0.45d;
        }
        if (ratio >= 0.4d) {
            return 0.2d;
        }
        return 0.0d;
    }

    private double scoreStyleFit(ChatSession.PreferenceProfile profile, String haystack) {
        if (profile == null || profile.getStyle() == null || profile.getStyle().isBlank()) {
            return 0.0d;
        }
        String style = normalizeText(profile.getStyle());
        if (style.contains("minimal") || style.contains("basic")) {
            return containsAny(haystack, "basic", "regular", "so mi", "ao thun", "midi", "tron") ? 0.9d : 0.0d;
        }
        if (style.contains("thanh lich") || style.contains("elegant") || style.contains("smart casual")) {
            return containsAny(haystack, "so mi", "blazer", "midi", "dam", "trouser", "chan vay") ? 1.0d : 0.0d;
        }
        if (style.contains("sporty") || style.contains("casual") || style.contains("relaxed")) {
            return containsAny(haystack, "ao thun", "jean", "short", "hoodie", "bomber", "oversize") ? 0.9d : 0.0d;
        }
        return 0.0d;
    }

    private String inferOccasionContext(String normalizedSearch) {
        if (normalizedSearch == null || normalizedSearch.isBlank()) {
            return "";
        }
        if (containsAny(normalizedSearch, "di lam", "cong so", "office")) return "office";
        if (containsAny(normalizedSearch, "di tiec", "su kien", "party")) return "party";
        if (containsAny(normalizedSearch, "du lich", "di choi", "hang ngay", "casual")) return "casual";
        if (containsAny(normalizedSearch, "mua he", "he")) return "summer";
        if (containsAny(normalizedSearch, "mua dong", "dong")) return "winter";
        return "";
    }

    private Set<String> inferTaxonomyLabels(String haystack) {
        LinkedHashSet<String> labels = new LinkedHashSet<>();
        if (haystack == null || haystack.isBlank()) {
            return labels;
        }

        if (containsAny(haystack, "ao so mi", "shirt", "blazer", "trouser", "quan tay")) {
            labels.add("office");
            labels.add("safe");
        }
        if (containsAny(haystack, "ao thun", "ao phong", "jean", "short", "hoodie", "bomber")) {
            labels.add("casual");
        }
        if (containsAny(haystack, "dam", "dress", "ao kieu", "chan vay", "jacquard")) {
            labels.add("statement");
        }
        if (containsAny(haystack, "midi", "regular", "basic", "cotton", "linen", "tron")) {
            labels.add("safe");
        }
        if (containsAny(haystack, "linen", "short", "midi", "ao phong")) {
            labels.add("summer");
        }
        if (containsAny(haystack, "len", "hoodie", "ao khoac", "jacket", "coat")) {
            labels.add("winter");
        }
        if (containsAny(haystack, "ao khoac", "blazer", "dress", "dam", "chan vay")) {
            labels.add("party");
        }
        return labels;
    }

    private double scoreOccasionFit(String occasion, Set<String> taxonomyLabels) {
        if (occasion == null || occasion.isBlank() || taxonomyLabels == null || taxonomyLabels.isEmpty()) {
            return 0.0d;
        }
        if (taxonomyLabels.contains(occasion)) {
            return 1.4d;
        }
        if ("office".equals(occasion) && taxonomyLabels.contains("safe")) {
            return 0.8d;
        }
        if ("party".equals(occasion) && taxonomyLabels.contains("statement")) {
            return 1.0d;
        }
        if ("casual".equals(occasion) && taxonomyLabels.contains("safe")) {
            return 0.5d;
        }
        return 0.0d;
    }

    private double scoreSafetyFit(Set<String> taxonomyLabels) {
        if (taxonomyLabels == null || taxonomyLabels.isEmpty()) {
            return 0.0d;
        }
        return taxonomyLabels.contains("safe") ? 1.0d : 0.0d;
    }

    private double scoreStatementFit(Set<String> taxonomyLabels) {
        if (taxonomyLabels == null || taxonomyLabels.isEmpty()) {
            return 0.0d;
        }
        return taxonomyLabels.contains("statement") || taxonomyLabels.contains("party") ? 0.9d : 0.0d;
    }

    private String buildOccasionReason(String occasion) {
        if (occasion == null || occasion.isBlank()) {
            return "Hợp ngữ cảnh đang tìm";
        }
        return switch (occasion) {
            case "office" -> "Hợp đi làm và dễ chốt";
            case "party" -> "Hợp dịp cần lên outfit";
            case "casual" -> "Hợp mặc hằng ngày hoặc đi chơi";
            case "summer" -> "Hợp thời tiết nóng hoặc mùa hè";
            case "winter" -> "Hợp thời tiết mát hoặc mùa lạnh";
            default -> "Hợp ngữ cảnh đang tìm";
        };
    }

    private double scoreFocusTags(Set<String> focusTags, String haystack) {
        double score = 0.0d;
        for (String tag : focusTags) {
            String normalizedTag = normalizeText(tag);
            if (normalizedTag.startsWith("fit:")) {
                String fitValue = normalizedTag.substring(4);
                if (!fitValue.isBlank() && haystack.contains(fitValue)) {
                    score += 0.5d;
                }
            }
        }
        return score;
    }

    private String buildBusinessReason(List<String> reasons) {
        if (reasons == null || reasons.isEmpty()) {
            return "Dễ cân nhắc cho nhu cầu hiện tại";
        }
        return reasons.stream()
                .distinct()
                .limit(2)
                .reduce((left, right) -> left + " | " + right)
                .orElse("Dễ cân nhắc cho nhu cầu hiện tại");
    }

    private List<ChatResponse.ProductSuggestion> applySizeFilter(
            List<ChatResponse.ProductSuggestion> suggestions,
            String size) {
        if (suggestions == null || suggestions.isEmpty() || size == null || size.isBlank()) {
            return suggestions;
        }
        String normalizedSize = normalizeText(size);
        List<ChatResponse.ProductSuggestion> filtered = suggestions.stream()
                .filter(s -> safeList(s.getAvailableSizes()).stream()
                        .anyMatch(avail -> normalizeText(avail).equals(normalizedSize)))
                .toList();
        return filtered.isEmpty() ? suggestions : filtered;
    }

    private List<ChatResponse.ProductSuggestion> applyColorFilter(
            List<ChatResponse.ProductSuggestion> suggestions,
            String color) {
        if (suggestions == null || suggestions.isEmpty() || color == null || color.isBlank()) {
            return suggestions;
        }
        String normalizedColor = normalizeText(color);
        List<ChatResponse.ProductSuggestion> filtered = suggestions.stream()
                .filter(s -> safeList(s.getAvailableColors()).stream()
                        .anyMatch(avail -> normalizeText(avail).contains(normalizedColor)))
                .toList();
        return filtered.isEmpty() ? suggestions : filtered;
    }

    private List<ChatResponse.ProductSuggestion> applyCategoryLock(
            List<ChatResponse.ProductSuggestion> suggestions,
            String search) {
        if (suggestions == null || suggestions.isEmpty() || search == null || search.isBlank()) {
            return suggestions;
        }

        List<String> lockedTypes = productTaxonomyService.extractTypeLabels(search, search);
        if (lockedTypes == null || lockedTypes.isEmpty()) {
            return suggestions;
        }

        Set<String> normalizedLockedTypes = new LinkedHashSet<>();
        for (String type : lockedTypes) {
            String normalizedType = normalizeText(type);
            if (!normalizedType.isBlank()) {
                normalizedLockedTypes.add(normalizedType);
            }
        }
        if (normalizedLockedTypes.isEmpty()) {
            return suggestions;
        }

        List<ChatResponse.ProductSuggestion> filtered = new ArrayList<>();
        for (ChatResponse.ProductSuggestion suggestion : suggestions) {
            if (hasLockedCategoryMatch(suggestion, normalizedLockedTypes)) {
                filtered.add(suggestion);
            }
        }
        return filtered.isEmpty() ? suggestions : filtered;
    }

    private List<ChatResponse.ProductSuggestion> applyGenderGate(
            List<ChatResponse.ProductSuggestion> suggestions,
            ChatSession.PreferenceProfile profile,
            String search) {
        if (suggestions == null || suggestions.isEmpty() || profile == null) {
            return suggestions;
        }

        String effectiveGender = resolveEffectiveTargetGender(profile, search);
        if (effectiveGender == null || effectiveGender.isBlank()) {
            return suggestions;
        }

        List<ChatResponse.ProductSuggestion> filtered = suggestions.stream()
                .filter(suggestion -> matchesGenderGate(suggestion, effectiveGender))
                .toList();

        return filtered.isEmpty() ? suggestions : filtered;
    }

    private String resolveEffectiveTargetGender(ChatSession.PreferenceProfile profile, String search) {
        String normalizedSearch = normalizeText(search);
        if (containsAny(normalizedSearch, "vay", "dam", "chan vay", "blouse", "ao kieu", "wrap dress", "midi")) {
            return "female";
        }
        if (containsAny(normalizedSearch, "ao so mi nam", "do nam", "quan chino", "ao oxford", "ao polo nam")) {
            return "male";
        }
        return normalizeText(profile.getTargetGender());
    }

    private boolean matchesGenderGate(ChatResponse.ProductSuggestion suggestion, String targetGender) {
        if (suggestion == null || targetGender == null || targetGender.isBlank()) {
            return true;
        }

        String categoryGender = normalizeText(suggestion.getCategoryGender());
        if (!categoryGender.isBlank()) {
            if ("male".equals(targetGender)) {
                return "male".equals(categoryGender);
            }
            if ("female".equals(targetGender)) {
                return "female".equals(categoryGender);
            }
        }

        String normalized = normalizeText(stringValue(suggestion.getName()) + " " + stringValue(suggestion.getCategory()));
        if ("female".equals(targetGender)) {
            return !containsAny(normalized, "ao so mi nam", "oxford", "chino", "regular fit nam");
        }
        if ("male".equals(targetGender)) {
            return !containsAny(normalized, "vay", "dam", "chan vay", "blouse", "midi", "wrap dress");
        }
        return true;
    }

    private boolean hasLockedCategoryMatch(
            ChatResponse.ProductSuggestion suggestion,
            Set<String> normalizedLockedTypes) {
        if (suggestion == null || normalizedLockedTypes == null || normalizedLockedTypes.isEmpty()) {
            return false;
        }
        List<String> suggestionTypes = productTaxonomyService.extractTypeLabels(
                suggestion.getName(),
                suggestion.getCategory()
        );
        if (suggestionTypes == null || suggestionTypes.isEmpty()) {
            return false;
        }
        for (String suggestionType : suggestionTypes) {
            if (normalizedLockedTypes.contains(normalizeText(suggestionType))) {
                return true;
            }
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private List<ChatResponse.ProductSuggestion> executeBrowse(Long minPrice, Long maxPrice, int size) {
        return executeBrowse(minPrice, maxPrice, size, 0);
    }

    @SuppressWarnings("unchecked")
    private List<ChatResponse.ProductSuggestion> executeBrowse(Long minPrice, Long maxPrice, int size, int page) {
        int pageSize = Math.max(5, size);
        int safePage = Math.max(0, page);
        Map<String, Object> payload = fetchProductPage(safePage, pageSize, minPrice, maxPrice);

        return mapProductPage(payload);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> fetchProductPage(int page, int size, Long minPrice, Long maxPrice) {
        int pageSize = Math.max(5, size);
        int safePage = Math.max(0, page);
        StringBuilder uri = new StringBuilder(productServiceUrl)
                .append("/api/v1/products?page=").append(safePage).append("&size=").append(pageSize)
                .append("&sortBy=createdAt&sortDir=desc");
        if (minPrice != null) uri.append("&minPrice=").append(minPrice);
        if (maxPrice != null) uri.append("&maxPrice=").append(maxPrice);

        try {
            return executeResilient("product-service", () -> webClient.get()
                    .uri(uri.toString())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(TOOL_TIMEOUT)
                    .block());
        } catch (Exception ex) {
            throw new RuntimeException("Product browse failed", ex);
        }
    }

    @SuppressWarnings("unchecked")
    private List<String> collectAllProductTypes(int maxPages, int pageSize) {
        LinkedHashSet<String> types = new LinkedHashSet<>();
        int safeMaxPages = Math.max(1, maxPages);
        int safePageSize = Math.max(20, pageSize);
        Integer totalPages = null;

        for (int page = 0; page < safeMaxPages; page++) {
            Map<String, Object> payload = fetchProductPage(page, safePageSize, null, null);
            if (payload == null || payload.isEmpty()) break;

            if (totalPages == null) {
                Object totalPagesRaw = payload.get("totalPages");
                if (totalPagesRaw instanceof Number number) {
                    totalPages = number.intValue();
                }
            }

            List<ChatResponse.ProductSuggestion> products = mapProductPage(payload);
            if (products.isEmpty()) break;

            for (ChatResponse.ProductSuggestion product : products) {
                types.addAll(productTaxonomyService.extractTypeLabels(product.getName(), product.getCategory()));
            }

            Object lastFlag = payload.get("last");
            if (Boolean.TRUE.equals(lastFlag)) break;
            if (totalPages != null && page + 1 >= totalPages) break;
            if (products.size() < safePageSize && totalPages == null) break;
        }

        if (types.size() > 40) {
            return new ArrayList<>(types).subList(0, 40);
        }
        return new ArrayList<>(types);
    }

    private List<String> extractTypeLabels(String name, String category) {
        String normalized = normalizeText(stringValue(name) + " " + stringValue(category));
        LinkedHashSet<String> types = new LinkedHashSet<>();
        boolean matched = false;

        if (containsAny(normalized, "ao thun", "ao phong", "t-shirt", "tee")) {
            types.add("áo thun");
            matched = true;
        }
        if (containsAny(normalized, "ao so mi", "shirt")) {
            types.add("áo sơ mi");
            matched = true;
        }
        if (containsAny(normalized, "ao khoac", "jacket", "blazer", "coat")) {
            types.add("áo khoác");
            matched = true;
        }
        if (containsAny(normalized, "parka", "bomber")) {
            types.add("áo khoác");
            matched = true;
        }
        if (containsAny(normalized, "ao polo", "polo")) {
            types.add("áo polo");
            matched = true;
        }
        if (containsAny(normalized, "ao hoodie", "hoodie")) {
            types.add("áo hoodie");
            matched = true;
        }
        if (containsAny(normalized, "ao len", "sweater", "knit")) {
            types.add("áo len");
            matched = true;
        }
        if (containsAny(normalized, "ao kieu")) {
            types.add("áo kiểu");
            matched = true;
        }
        if (containsAny(normalized, "ao ghi le", "ao gi le", "gilet", "vest")) {
            types.add("áo ghi lê");
            matched = true;
        }
        if (containsAny(normalized, "quan jean", "jeans", "denim")) {
            types.add("quần jean");
            matched = true;
        }
        if (containsAny(normalized, "quan tay", "trouser", "slacks")) {
            types.add("quần tây");
            matched = true;
        }
        if (containsAny(normalized, "quan short", "short")) {
            types.add("quần short");
            matched = true;
        }
        if (containsAny(normalized, "quan dai")) {
            types.add("quần dài");
            matched = true;
        }
        if (containsAny(normalized, "quan vay")) {
            types.add("quần váy");
            matched = true;
        }
        if (containsAny(normalized, "chan vay", "skirt")) {
            types.add("chân váy");
            matched = true;
        }
        if (containsAny(normalized, "dam", "dress")) {
            types.add("đầm");
            matched = true;
        }
        if (containsAny(normalized, "vay")) {
            types.add("váy");
            matched = true;
        }
        if (containsAny(normalized, "jumpsuit")) {
            types.add("jumpsuit");
            matched = true;
        }
        if (containsAny(normalized, "ao dai")) {
            types.add("áo dài");
            matched = true;
        }
        if (containsAny(normalized, "giay", "shoes", "sneaker", "boot")) {
            types.add("giày");
            matched = true;
        }
        if (containsAny(normalized, "tui", "bag", "handbag", "backpack")) {
            types.add("túi");
            matched = true;
        }
        if (containsAny(normalized, "non", "hat", "cap")) {
            types.add("nón");
            matched = true;
        }

        if (!matched) {
            String categoryLabel = stringValue(category).trim();
            if (!categoryLabel.isBlank()) {
                types.add(categoryLabel);
            }
        }

        return new ArrayList<>(types);
    }

    private boolean containsAny(String haystack, String... needles) {
        if (haystack == null || haystack.isBlank()) return false;
        for (String needle : needles) {
            if (needle != null && !needle.isBlank() && haystack.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    private List<String> filterTypesByGroup(List<String> allTypes, String groupHint) {
        if (allTypes == null || allTypes.isEmpty()) return List.of();
        if (groupHint == null || groupHint.isBlank()) return allTypes;

        String targetGroup = productTaxonomyService.resolveGroupLabel(groupHint);
        List<String> filtered = new ArrayList<>();
        for (String type : allTypes) {
            if (productTaxonomyService.resolveGroupLabel(type).equals(targetGroup)) {
                filtered.add(type);
            }
        }
        return filtered;
    }

    private String resolveGroupLabel(String value) {
        String normalized = normalizeText(value);
        if (normalized.contains("ao")) return "áo";
        if (normalized.contains("quan")) return "quần";
        if (normalized.contains("vay") || normalized.contains("dam") || normalized.contains("chan vay")
            || normalized.contains("jumpsuit")) {
            return "váy/đầm";
        }
        if (normalized.contains("giay") || normalized.contains("tui") || normalized.contains("non")
                || normalized.contains("phu kien")) {
            return "phụ kiện";
        }
        return "khác";
    }

    private String formatGroupedTypeList(List<String> types) {
        LinkedHashMap<String, List<String>> grouped = new LinkedHashMap<>();
        grouped.put("Áo", new ArrayList<>());
        grouped.put("Quần", new ArrayList<>());
        grouped.put("Váy/Đầm", new ArrayList<>());
        grouped.put("Phụ kiện", new ArrayList<>());
        grouped.put("Khác", new ArrayList<>());

        for (String type : types) {
            String group = resolveGroupLabel(type);
            String key = switch (group) {
                case "áo" -> "Áo";
                case "quần" -> "Quần";
                case "váy/đầm" -> "Váy/Đầm";
                case "phụ kiện" -> "Phụ kiện";
                default -> "Khác";
            };
            grouped.computeIfAbsent(key, ignored -> new ArrayList<>()).add(type);
        }

        StringBuilder result = new StringBuilder("Hiện shop đang có các loại sản phẩm:\n");
        for (Map.Entry<String, List<String>> entry : grouped.entrySet()) {
            if (entry.getValue().isEmpty()) continue;
            result.append("- ").append(entry.getKey()).append(": ")
                    .append(String.join(", ", entry.getValue()))
                    .append("\n");
        }
        result.append("Bạn muốn mình tư vấn loại nào ạ?");
        return result.toString();
    }

    private List<ChatResponse.ProductSuggestion> diversifySuggestionsByCategory(
            List<ChatResponse.ProductSuggestion> suggestions,
            int maxResults) {
        if (suggestions == null || suggestions.isEmpty()) return suggestions;

        int target = Math.min(maxResults, suggestions.size());
        Map<String, Integer> categoryCounts = new LinkedHashMap<>();
        List<ChatResponse.ProductSuggestion> diversified = new ArrayList<>();
        List<ChatResponse.ProductSuggestion> overflow = new ArrayList<>();

        for (ChatResponse.ProductSuggestion suggestion : suggestions) {
            String key = suggestion.getCategory() == null || suggestion.getCategory().isBlank()
                    ? "Khác"
                    : suggestion.getCategory();
            int count = categoryCounts.getOrDefault(key, 0);
            if (count < 2) {
                diversified.add(suggestion);
                categoryCounts.put(key, count + 1);
            } else {
                overflow.add(suggestion);
            }
            if (diversified.size() >= target) {
                return diversified;
            }
        }

        for (ChatResponse.ProductSuggestion suggestion : overflow) {
            diversified.add(suggestion);
            if (diversified.size() >= target) {
                break;
            }
        }

        return diversified.isEmpty() ? suggestions : diversified;
    }

    private List<String> safeList(List<String> values) {
        return values == null ? List.of() : values;
    }

    private String buildConciseSuggestionReply(List<ChatResponse.ProductSuggestion> suggestions,
                                               String opener,
                                               Long minPrice,
                                               Long maxPrice,
                                               String color,
                                               String size,
                                               String closing) {
        StringBuilder reply = new StringBuilder(opener);
        List<String> highlights = new ArrayList<>();

        if (color != null && !color.isBlank()) {
            highlights.add("ưu tiên tông " + color);
        }
        if (size != null && !size.isBlank()) {
            highlights.add("size " + size.toUpperCase(Locale.ROOT));
        }

        String priceRange = describePriceRange(minPrice, maxPrice);
        if (!priceRange.isBlank()) {
            highlights.add(priceRange);
        }

        if (!highlights.isEmpty()) {
            reply.append(" Mình đang ưu tiên ").append(String.join(", ", highlights)).append(".");
        }

        if (closing != null && !closing.isBlank()) {
            reply.append(" ").append(closing);
        }
        return reply.toString();
    }

    private String describePriceRange(Long minPrice, Long maxPrice) {
        if (minPrice == null && maxPrice == null) {
            return "";
        }
        if (minPrice != null && maxPrice != null) {
            return "khoảng giá " + formatMoney(BigDecimal.valueOf(minPrice))
                    + " - " + formatMoney(BigDecimal.valueOf(maxPrice));
        }
        if (minPrice != null) {
            return "mức giá từ " + formatMoney(BigDecimal.valueOf(minPrice));
        }
        return "mức giá dưới " + formatMoney(BigDecimal.valueOf(maxPrice));
    }

    private String humanizeOccasion(String occasion) {
        if (occasion == null || occasion.isBlank()) {
            return "nhu cầu này";
        }
        return switch (normalizeText(occasion)) {
            case "di lam" -> "đi làm";
            case "di tiec" -> "đi tiệc";
            case "du lich" -> "du lịch";
            case "he" -> "mùa hè";
            case "dong" -> "mùa đông";
            case "thu" -> "mùa thu";
            case "xuan" -> "mùa xuân";
            default -> occasion.replace('_', ' ');
        };
    }

    private String humanizeStyle(String style) {
        if (style == null || style.isBlank()) {
            return "";
        }
        return switch (normalizeText(style)) {
            case "thanh lich" -> "thanh lịch";
            case "casual" -> "casual";
            case "sporty" -> "sporty";
            default -> style.replace('_', ' ');
        };
    }

    private record SemanticMatch(ChatResponse.ProductSuggestion suggestion, int score, int index) {}

    private record ScoredSuggestion(ChatResponse.ProductSuggestion suggestion, double score, int index) {}

    // ========== ORDER TOOLS ==========

    @Tool("""
            Kiểm tra trạng thái đơn hàng theo mã đơn.
            Gọi khi người dùng hỏi về đơn hàng, giao hàng, trạng thái.
            VD: 'đơn ORD-123 giao chưa', 'kiểm tra đơn hàng'.

            CRITICAL: Do NOT invent or guess the Order Number.
            If the user has not explicitly provided an Order ID (e.g., ORD-xxx),
            you MUST ask them to provide it before calling this tool.
            NEVER fabricate an order number.
            """)
    public String checkOrderByNumber(
            @P("Mã đơn hàng, VD: ORD-1713200000000") String orderNumber) {
        try {
            log.info("Tool: checkOrderByNumber(orderNumber={})", orderNumber);
            if (orderNumber == null || orderNumber.isBlank()) {
                return "Bạn gửi giúp mình mã đơn hàng (ví dụ: ORD-123456) để mình kiểm tra chính xác nhé.";
            }
            String userId = currentUserId();
            if (isGuestUser(userId)) {
                return "Báº¡n cáº§n Ä‘Äƒng nháº­p Ä‘á»ƒ mÃ¬nh kiá»ƒm tra Ä‘Æ¡n hÃ ng chÃ­nh xÃ¡c nhÃ©.";
            }
            var request = webClient.get()
                    .uri(orderServiceUrl + "/api/v1/orders/by-number/{number}", orderNumber)
                    .accept(MediaType.APPLICATION_JSON);
            if (userId != null && !userId.isBlank()) {
                request = request.header("X-User-Id", userId);
            }
            var finalRequest = request;

            @SuppressWarnings("unchecked")
            Map<String, Object> order = executeResilient("order-service", () -> finalRequest
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(TOOL_TIMEOUT)
                    .block());

            if (order == null) return "Không tìm thấy đơn hàng " + orderNumber;

            String status = stringValue(order.get("status"));
            String totalAmount = stringValue(order.get("totalAmount"));

            return "Đơn hàng " + orderNumber + ": trạng thái " + status + ", tổng tiền " + totalAmount + " VND.";
        } catch (Throwable ex) {
            log.error("checkOrderByNumber failed", ex);
            markToolFailure();
            return "Dịch vụ đơn hàng tạm thời không phản hồi. Vui lòng thử lại sau.";
        }
    }

    // ========== PROMOTION TOOLS ==========

    @Tool(name = "validateCoupon", value = """
            Kiểm tra mã giảm giá / coupon có hợp lệ với đơn hàng không.
            Gọi khi người dùng hỏi về mã giảm giá, coupon, voucher.
            VD: 'mã SALE20 dùng được không', 'coupon cho đơn 1 triệu'.

            CRITICAL: Do NOT invent or guess the Coupon Code or Order Amount.
            If the user has not explicitly provided BOTH the coupon code AND the order amount,
            you MUST ask them to provide the missing information before calling this tool.
            """)
    public String validateCouponTool(
            @P("Mã coupon") String code,
            @P("Giá trị đơn hàng (VND)") String orderAmountStr) {
        return validateCoupon(code, parseLongSafe(orderAmountStr));
    }

    public String validateCoupon(
            String code,
            Long orderAmount) {
        try {
            log.info("Tool: validateCoupon(code={}, amount={})", code, orderAmount);
            if (code == null || code.isBlank()) {
                return "Bạn gửi giúp mình mã coupon cần kiểm tra nhé.";
            }
            if (orderAmount == null) {
                return "Bạn cho mình thêm giá trị đơn hàng để mình kiểm tra coupon chính xác nhé.";
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> result = executeResilient("promotion-service", () -> webClient.post()
                    .uri(promotionServiceUrl + "/api/v1/promotions/validate?code={code}&orderAmount={amount}",
                            code, orderAmount)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(TOOL_TIMEOUT)
                    .block());

            if (result == null) return "Không thể kiểm tra mã " + code;

            boolean valid = Boolean.TRUE.equals(result.get("valid"));
            if (valid) {
                return "Mã " + code + " hợp lệ! Giảm " + result.get("discountAmount") + " VND cho đơn " + orderAmount + " VND.";
            } else {
                String reason = stringValue(result.get("message"));
                return "Mã " + code + " không hợp lệ: " + (reason.isBlank() ? "không đạt điều kiện" : reason);
            }
        } catch (Throwable ex) {
            log.error("validateCoupon failed", ex);
            markToolFailure();
            return "Dịch vụ khuyến mãi tạm thời không phản hồi. Vui lòng thử lại sau.";
        }
    }

    @Tool("""
            Lấy danh sách khuyến mãi đang hiệu lực.
            Gọi khi người dùng hỏi về chương trình khuyến mãi, giảm giá, deal.
            """)
    public String getActivePromotions() {
        try {
            log.info("Tool: getActivePromotions()");
            @SuppressWarnings("unchecked")
            Object payload = executeResilient("promotion-service", () -> webClient.get()
                    .uri(promotionServiceUrl + "/api/v1/promotions/active")
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .bodyToMono(Object.class)
                    .timeout(TOOL_TIMEOUT)
                    .block());

            List<ChatResponse.PromotionSuggestion> promos = mapPromotions(payload);
            if (collector() != null) collector().addPromotions(promos);

            if (promos.isEmpty()) return "Hiện không có chương trình khuyến mãi nào đang diễn ra.";

            StringBuilder result = new StringBuilder("Có " + promos.size() + " khuyến mãi đang hiệu lực:\n");
            for (var p : promos) {
                result.append("- Mã: ").append(p.getCode())
                        .append(" | Giảm: ").append(p.getDiscountValue()).append(" ").append(p.getDiscountType())
                        .append(" | Đơn tối thiểu: ").append(p.getMinOrderAmount())
                        .append("\n");
            }
            return result.toString();
        } catch (Throwable ex) {
            log.error("getActivePromotions failed", ex);
            markToolFailure();
            return "Dịch vụ khuyến mãi tạm thời không phản hồi. Vui lòng thử lại sau.";
        }
    }

    // ========== SIZE CONSULTATION ==========

    @Tool(name = "consultSize", value = """
            Tư vấn size thời trang dựa trên số đo cơ thể.
            Gọi NGAY LẬP TỨC khi user cung cấp số đo (chiều cao, cân nặng...) và hỏi size, KỂ CẢ KHI user nhắc đến tên sản phẩm.
            KHÔNG GỌI searchProducts nếu user đang nhờ tư vấn size cho một sản phẩm cụ thể. Hãy gọi luôn tool này!
            
            Tự động suy luận 'garmentType' từ tên sản phẩm user nhắc đến (ví dụ: áo, sơ mi, khoác -> 'top'; quần, váy, chân váy -> 'bottom').

            CRITICAL: Only pass measurement values the user has EXPLICITLY stated.
            Use null for any measurement NOT mentioned by the user.
            Do NOT guess height, weight, or body measurements.
            """)
    public String consultSizeTool(
            @P("Chiều cao (cm)") String heightCmStr,
            @P("Cân nặng (kg)") String weightKgStr,
            @P("Vòng ngực (cm), null nếu không có") String chestCmStr,
            @P("Vòng eo (cm), null nếu không có") String waistCmStr,
            @P("Vòng hông (cm), null nếu không có") String hipCmStr,
            @P("Loại đồ: 'top' (áo) hoặc 'bottom' (quần/váy)") String garmentType) {
        return consultSize(
                parseIntegerSafe(heightCmStr),
                parseIntegerSafe(weightKgStr),
                parseIntegerSafe(chestCmStr),
                parseIntegerSafe(waistCmStr),
                parseIntegerSafe(hipCmStr),
                garmentType
        );
    }

    public String consultSize(
            Integer heightCm,
            Integer weightKg,
            Integer chestCm,
            Integer waistCm,
            Integer hipCm,
            String garmentType) {
        try {
            log.info("Tool: consultSize(h={}, w={}, c={}, w={}, h={}, type={})", 
                heightCm, weightKg, chestCm, waistCm, hipCm, garmentType);
            SizeAdvisorService.Measurements m = new SizeAdvisorService.Measurements(
                    heightCm, weightKg, chestCm, waistCm, hipCm);

            List<String> missing = m.missingFields();
            if (!m.hasMinimumData()) {
                if (collector() != null) collector().addMissingFields(missing);
                return "Cần thêm thông tin: " + String.join(", ", missing) + " để tư vấn size chính xác.";
            }

            SizeAdvisorService.GarmentType type = "bottom".equalsIgnoreCase(garmentType)
                    ? SizeAdvisorService.GarmentType.BOTTOM
                    : SizeAdvisorService.GarmentType.TOP;

            SizeFitAdvisoryService.SizeFitAdvice advice =
                    sizeFitAdvisoryService.advise(m, type, garmentType, preferenceProfile());
            if (collector() != null) collector().setSizeRecommendation(advice.recommendedSize());

            String reply = "Goi y size " + advice.recommendedSize() + ". " + advice.rationale();
            if (advice.followUpPrompt() != null && !advice.followUpPrompt().isBlank()) {
                reply += " " + advice.followUpPrompt();
            }
            return reply;
        } catch (Throwable ex) {
            log.error("consultSize failed", ex);
            return "Mình gặp chút trục trặc khi tính toán size. Bạn thử cung cấp lại số đo nhé!";
        }
    }

    // ========== OUTFIT SUGGESTION ==========

    @Tool("""
            Gợi ý outfit/trang phục theo mùa, dịp, phong cách.
            Gọi khi người dùng hỏi gợi ý outfit đi làm, đi tiệc, mùa hè, mùa đông.
            VD: 'gợi ý đồ đi làm mùa hè', 'outfit đi tiệc cuối năm'.
            """)
    public String suggestOutfit(
            @P("Mùa hoặc dịp: 'he', 'dong', 'thu', 'xuan', 'di_lam', 'di_tiec', 'du_lich'") String occasion,
            @P("Phong cách (tùy chọn): 'thanh_lich', 'casual', 'sporty'") String style) {
        try {
            log.info("Tool: suggestOutfit(occasion={}, style={})", occasion, style);
            String targetGender = normalizeText(preferenceProfile().getTargetGender());
            List<String> queries = filterOutfitQueriesByGender(
                    outfitRuleEngine.buildQueries(occasion, style),
                    targetGender);

            for (String query : queries) {
                // Giữ flow tool-calling hiện tại để collector tiếp tục gom suggestions cho FE render.
                searchProducts(query, null, null, null, null);
            }

            StringBuilder reply = new StringBuilder("Mình đã phối sẵn vài lựa chọn phù hợp cho ");
            reply.append(humanizeOccasion(occasion));
            if (style != null && !style.isBlank()) {
                reply.append(" theo phong cách ").append(humanizeStyle(style));
            }
            reply.append(". Bạn xem các card bên dưới, nếu muốn mình sẽ chốt giúp 1 combo dễ mặc nhất.");
            return reply.toString();
        } catch (Throwable ex) {
            log.error("suggestOutfit failed", ex);
            return "Mình chưa thể gợi ý trang phục lúc này. Bạn thử lại sau nhé!";
        }
    }

    private List<String> filterOutfitQueriesByGender(List<String> queries, String targetGender) {
        if (queries == null || queries.isEmpty() || targetGender == null || targetGender.isBlank()) {
            return queries == null ? List.of() : queries;
        }

        List<String> filtered = queries.stream()
                .filter(query -> isQueryCompatibleWithGender(query, targetGender))
                .toList();

        // Nếu rule set hiện tại không còn query nào sau khi lọc, giữ query gốc để không làm
        // bot rơi vào trạng thái không gợi ý được outfit.
        return filtered.isEmpty() ? queries : filtered;
    }

    private boolean isQueryCompatibleWithGender(String query, String targetGender) {
        String normalizedQuery = normalizeText(query);
        if ("male".equals(targetGender)) {
            return !containsAny(normalizedQuery, "vay", "dam", "chan vay", "blouse", "ao kieu");
        }
        if ("female".equals(targetGender)) {
            return !containsAny(normalizedQuery, "ao so mi nam", "quan chino", "oxford", "polo nam");
        }
        return true;
    }

    // ========== KNOWLEDGE ==========

    @Tool("""
            Tìm kiếm thông tin trong knowledge base của shop (chính sách, FAQ, hướng dẫn).
            Gọi khi người dùng hỏi về: đổi trả, bảo hành, giao hàng, thanh toán,
            hướng dẫn size, cách bảo quản, chính sách khuyến mãi.
            KHÔNG gọi cho câu hỏi về sản phẩm cụ thể, đơn hàng, hoặc khuyến mãi đang chạy.
            """)
    public String searchKnowledge(
            @P("Câu hỏi hoặc từ khóa tìm kiếm") String query) {
        return searchKnowledgeInternal(query, false);
    }

    @Tool("""
            Tìm playbook tư vấn bán hàng, style guide, objection handling và ngôn ngữ chốt đơn mềm.
            Gọi khi người dùng hỏi kiểu: mẫu nào dễ mặc hơn, nên chọn phương án an toàn hay nổi bật,
            giá hơi cao có lựa chọn mềm hơn không, nên phối thế nào, nên chốt mẫu nào, đi làm/đi chơi/đi tiệc nên ưu tiên kiểu gì.
            Ưu tiên dùng tool này cho câu hỏi mang tính tư vấn phong cách và sales, không phải policy thuần.
            """)
    public String searchSalesGuidance(
            @P("Câu hỏi hoặc tình huống cần tư vấn theo góc nhìn sales/stylist") String query) {
        return searchKnowledgeInternal(query, true);
    }

    private String searchKnowledgeInternal(String query, boolean prioritizeSalesSources) {
        try {
            log.info("Tool: searchKnowledgeInternal(query={}, prioritizeSalesSources={})", query, prioritizeSalesSources);
            if (query == null || query.isBlank()) {
                return "Bạn nói rõ thêm giúp mình câu hỏi cần tra cứu, ví dụ: đổi trả, giao hàng, bảo hành hoặc hướng dẫn đặt hàng.";
            }
            List<KnowledgeBaseService.SearchResult> results = knowledgeBaseService.search(query);
            if (prioritizeSalesSources) {
                results = prioritizeSalesKnowledge(results);
            }

            if (results.isEmpty()) {
                return "Không tìm thấy thông tin liên quan trong kiến thức của shop. "
                        + "Bạn có thể liên hệ CSKH để được hỗ trợ trực tiếp.";
            }

            StringBuilder answer = new StringBuilder();
            for (var result : results) {
                answer.append(result.content()).append("\n");
                answer.append("[Nguồn: ").append(result.title())
                        .append(" (").append(result.source()).append(")]\n\n");

                if (collector() != null) {
                    collector().addKnowledgeSource(result.title(), result.source(), result.score());
                }
            }

            return answer.toString().trim();
        } catch (Throwable ex) {
            log.error("searchKnowledge failed", ex);
            markToolFailure();
            return "Mình chưa thể tìm kiếm thông tin lúc này. Bạn thử lại sau nhé!";
        }
    }

    private List<KnowledgeBaseService.SearchResult> prioritizeSalesKnowledge(List<KnowledgeBaseService.SearchResult> results) {
        if (results == null || results.isEmpty()) {
            return List.of();
        }

        return results.stream()
                .sorted(Comparator.comparingDouble((KnowledgeBaseService.SearchResult result) ->
                        scoreKnowledgeSourcePriority(result.source(), result.title(), result.score())).reversed())
                .limit(4)
                .toList();
    }

    private double scoreKnowledgeSourcePriority(String source, String title, double baseScore) {
        String sourceKey = normalizeText(stringValue(source) + " " + stringValue(title));
        double bonus = 0.0d;

        if (sourceKey.contains("sales playbook")) {
            bonus += 0.6d;
        }
        if (sourceKey.contains("style guide")) {
            bonus += 0.45d;
        }
        if (sourceKey.contains("sales objections")) {
            bonus += 0.5d;
        }
        if (sourceKey.contains("faq") || sourceKey.contains("policy")) {
            bonus -= 0.1d;
        }

        return baseScore + bonus;
    }

    // ========== WISHLIST TOOL ==========

    @Tool("""
            Lấy danh sách sản phẩm yêu thích (wishlist) của user và đề xuất tư vấn.

            GỌI KHI:
            - User hỏi "wishlist của tôi", "sản phẩm tôi đã lưu", "đồ yêu thích của tôi"
            - Intent: wishlist_recommendation
            - User muốn tư vấn dựa trên sản phẩm đã quan tâm trước đó

            KHÔNG GỌI KHI:
            - User tìm kiếm sản phẩm mới (dùng searchProducts)
            - User là guest (chưa đăng nhập)

            CRITICAL:
            - Chỉ tư vấn dựa trên sản phẩm có trong danh sách trả về.
            - Không bịa thêm sản phẩm ngoài danh sách.
            - Nếu danh sách rỗng, thông báo wishlist chưa có sản phẩm và hỏi nhu cầu mới.
            """)
    public String getWishlistRecommendations(
            @P("User ID, lấy từ context phiên chat. Bắt buộc.") String userId) {
        try {
            log.info("Tool: getWishlistRecommendations(userId={})", userId);
            if (userId == null || userId.isBlank() || userId.startsWith("guest-")) {
                return "Bạn cần đăng nhập để xem danh sách yêu thích nhé.";
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> page = executeResilient("product-service", () -> webClient.get()
                    .uri(productServiceUrl + "/api/v1/wishlists?page=0&size=10")
                    .header("X-User-Id", userId)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(TOOL_TIMEOUT)
                    .block());

            List<ChatResponse.ProductSuggestion> products = mapProductPage(page);
            if (collector() != null) collector().addProducts(products);

            if (products.isEmpty()) {
                return "Danh sách yêu thích của bạn hiện đang trống. Bạn muốn mình gợi ý một số sản phẩm phù hợp không?";
            }

            long outOfStockCount = products.stream()
                    .filter(product -> safeList(product.getAvailableSizes()).isEmpty())
                    .count();

            StringBuilder reply = new StringBuilder("Mình đã mở lại wishlist và chọn sẵn những mẫu đáng chú ý cho bạn.");
            if (outOfStockCount > 0) {
                reply.append(" Hiện có ").append(outOfStockCount)
                        .append(" mẫu tạm hết size hoặc khó chốt nhanh, nên mình ưu tiên những lựa chọn dễ mua hơn.");
            }
            reply.append(" Bạn xem các card bên dưới, nếu cần mình sẽ gợi ý outfit hoặc chọn giúp mẫu nên mua trước.");
            return reply.toString();
        } catch (Throwable ex) {
            log.error("getWishlistRecommendations failed", ex);
            markToolFailure();
            return "Mình chưa thể tải danh sách yêu thích lúc này. Bạn thử lại sau nhé!";
        }
    }

    // ========== LOYALTY TOOL ==========

    @Tool("""
            Kiểm tra thông tin điểm thưởng và quyền lợi thành viên (loyalty) của user.

            GỌI KHI:
            - User hỏi về "điểm tích lũy", "điểm thưởng", "hạng thành viên", "quyền lợi thành viên"
            - User hỏi "tôi đang ở hạng gì", "tôi còn bao nhiêu điểm"
            - Intent: loyalty_benefit
            - User muốn dùng điểm để giảm giá

            KHÔNG GỌI KHI:
            - User hỏi về khuyến mãi coupon thông thường (dùng getActivePromotions hoặc validateCoupon)
            - User là guest

            CRITICAL:
            - Trả về đúng số điểm, tên hạng, % giảm giá từ dữ liệu thật.
            - Không suy diễn quyền lợi ngoài dữ liệu trả về.
            """)
    public String getLoyaltyBenefits(
            @P("User ID, lấy từ context phiên chat. Bắt buộc.") String userId) {
        try {
            log.info("Tool: getLoyaltyBenefits(userId={})", userId);
            if (userId == null || userId.isBlank() || userId.startsWith("guest-")) {
                return "Bạn cần đăng nhập để kiểm tra điểm thưởng và quyền lợi thành viên nhé.";
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> wallet = executeResilient("promotion-service", () -> webClient.get()
                    .uri(promotionServiceUrl + "/api/v1/promotions/loyalty/wallet")
                    .header("X-User-Id", userId)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(TOOL_TIMEOUT)
                    .block());

            if (wallet == null) return "Không tìm thấy thông tin tài khoản điểm thưởng của bạn.";

            String points       = stringValue(wallet.get("currentPoints"));
            String tierName     = stringValue(wallet.get("tierName"));
            String discount     = stringValue(wallet.get("tierDiscountPercent"));
            String pointToVnd   = stringValue(wallet.get("pointToVnd"));
            String totalSpending = stringValue(wallet.get("totalSpending"));

            StringBuilder result = new StringBuilder();
            result.append("Thông tin thành viên của bạn:\n");
            result.append("- Hạng thành viên: ").append(tierName.isBlank() ? "Chưa xếp hạng" : tierName).append("\n");
            result.append("- Điểm tích lũy hiện tại: ").append(points).append(" điểm\n");
            if (!discount.isBlank() && !discount.equals("0")) {
                result.append("- Ưu đãi hạng: Giảm ").append(discount).append("% mỗi đơn hàng\n");
            }
            if (!pointToVnd.isBlank() && !pointToVnd.equals("0")) {
                result.append("- Quy đổi: 1 điểm = ").append(pointToVnd).append(" VND\n");
            }
            if (!totalSpending.isBlank()) {
                result.append("- Tổng chi tiêu: ").append(totalSpending).append(" VND\n");
            }
            return result.toString().trim();
        } catch (Throwable ex) {
            log.error("getLoyaltyBenefits failed", ex);
            markToolFailure();
            return "Mình chưa thể kiểm tra thông tin điểm thưởng lúc này. Bạn thử lại sau nhé!";
        }
    }

    // ========== REVIEW TOOL ==========

    @Tool("""
            Lấy đánh giá và rating của sản phẩm từ khách hàng đã mua.

            GỌI KHI:
            - User hỏi "sản phẩm X có review không", "đánh giá của sản phẩm X", "rating của X"
            - User muốn biết chất lượng thực tế trước khi mua
            - Intent: cần thông tin chất lượng sản phẩm cụ thể

            KHÔNG GỌI KHI:
            - User chưa cung cấp product ID hoặc tên sản phẩm cụ thể
            - User hỏi chung về chất lượng (không có sản phẩm cụ thể)

            CRITICAL:
            - Phải có productId mới gọi tool này.
            - Trả về đúng điểm rating, số lượng review từ dữ liệu thật.
            - Không bịa nhận xét hoặc rating.
            """)
    public String getProductReviews(
            @P("ID sản phẩm cần xem đánh giá. Bắt buộc.") String productId) {
        try {
            log.info("Tool: getProductReviews(productId={})", productId);
            if (productId == null || productId.isBlank()) {
                return "Bạn gửi giúp mình mã sản phẩm hoặc chọn đúng card sản phẩm để mình xem đánh giá chính xác nhé.";
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> stats = executeResilient("review-service", () -> webClient.get()
                    .uri(reviewServiceUrl + "/api/v1/reviews/product/" + productId + "/stats")
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(TOOL_TIMEOUT)
                    .block());

            if (stats == null) return "Sản phẩm này chưa có đánh giá nào.";

            String avgRating  = stringValue(stats.get("averageRating"));
            String totalCount = stringValue(stats.get("totalReviews"));

            if (totalCount.equals("0") || totalCount.isBlank()) {
                return "Sản phẩm này chưa có đánh giá nào từ khách hàng. Bạn có thể là người đầu tiên review nhé!";
            }

            StringBuilder result = new StringBuilder();
            result.append("Đánh giá sản phẩm:\n");
            result.append("- Điểm trung bình: ⭐ ").append(avgRating).append("/5\n");
            result.append("- Số lượng đánh giá: ").append(totalCount).append(" review\n");

            // Star distribution if available
            @SuppressWarnings("unchecked")
            Map<String, Object> dist = (Map<String, Object>) stats.get("starDistribution");
            if (dist != null && !dist.isEmpty()) {
                result.append("- Phân bổ: ");
                for (int star = 5; star >= 1; star--) {
                    Object count = dist.get(String.valueOf(star));
                    if (count != null) {
                        result.append(star).append("★:").append(count).append(" ");
                    }
                }
                result.append("\n");
            }
            return result.toString().trim();
        } catch (Throwable ex) {
            log.error("getProductReviews failed", ex);
            return "Mình chưa thể tải đánh giá sản phẩm lúc này. Bạn thử lại sau nhé!";
        }
    }

    // ========== COMPARE TOOL ==========

    @Tool("""
            So sánh 2 sản phẩm về giá, size, màu sắc để giúp user lựa chọn.
            GỌI NGAY LẬP TỨC khi user liệt kê 2 sản phẩm (đặc biệt sau khi bạn đã hỏi họ muốn so sánh sản phẩm nào).
            Ví dụ: "áo sơ mi trắng và quần jean" -> gọi compareProducts.
            KHÔNG CẦN gọi searchProducts trước, tool này tự động tìm kiếm.
            """)
    public String compareProducts(
            @P("Tên hoặc từ khóa sản phẩm thứ nhất") String productA,
            @P("Tên hoặc từ khóa sản phẩm thứ hai") String productB) {
        if (productA == null || productA.isBlank() || productB == null || productB.isBlank()) {
            return "Bạn cho mình biết tên 2 sản phẩm muốn so sánh nhé. VD: 'So sánh áo thun Uniqlo và áo thun Zara'";
        }
        try {
            log.info("Tool: compareProducts(A={}, B={})", productA, productB);
            List<ChatResponse.ProductSuggestion> resultsA = executeSearch(productA, null, null);
            List<ChatResponse.ProductSuggestion> resultsB = executeSearch(productB, null, null);

            ChatResponse.ProductSuggestion a = resultsA.isEmpty() ? null : resultsA.get(0);
            ChatResponse.ProductSuggestion b = resultsB.isEmpty() ? null : resultsB.get(0);

            if (a == null && b == null) {
                return "Mình không tìm thấy cả 2 sản phẩm '" + productA + "' và '" + productB + "' trong hệ thống.";
            }
            if (a == null) {
                return "Mình không tìm thấy sản phẩm '" + productA + "' trong hệ thống. Bạn kiểm tra lại tên nhé.";
            }
            if (b == null) {
                return "Mình không tìm thấy sản phẩm '" + productB + "' trong hệ thống. Bạn kiểm tra lại tên nhé.";
            }

            // Add both to collector for frontend rendering
            if (collector() != null) {
                collector().addProducts(List.of(a, b));
            }

            StringBuilder result = new StringBuilder("So sánh 2 sản phẩm:\n\n");
            result.append("1️⃣ ").append(a.getName()).append("\n");
            result.append("   Giá: ").append(a.getPrice()).append("\n");
            result.append("   Size còn: ").append(String.join(", ", safeList(a.getAvailableSizes()))).append("\n");
            result.append("   Màu: ").append(String.join(", ", safeList(a.getAvailableColors()))).append("\n");

            result.append("\n2️⃣ ").append(b.getName()).append("\n");
            result.append("   Giá: ").append(b.getPrice()).append("\n");
            result.append("   Size còn: ").append(String.join(", ", safeList(b.getAvailableSizes()))).append("\n");
            result.append("   Màu: ").append(String.join(", ", safeList(b.getAvailableColors()))).append("\n");

            return result.toString().trim();
        } catch (Throwable ex) {
            log.error("compareProducts failed", ex);
            return "Mình chưa thể so sánh sản phẩm lúc này. Bạn thử lại sau nhé!";
        }
    }

    // ========== SAVE PREFERENCE TOOL ==========

    @Tool("""
            Lưu sở thích mua sắm của user vào profile để cá nhân hóa tư vấn lần sau.

            GỌI KHI:
            - User xác nhận rõ sở thích: "tôi thích màu đen", "tôi hay mặc size M", "ngân sách của tôi khoảng 500k"
            - User cung cấp thông tin về phong cách: "tôi thích style minimal", "tôi thích oversized"
            - Phát hiện preference mới từ hành vi chat có thể dùng lâu dài

            KHÔNG GỌI KHI:
            - User chỉ hỏi thông tin tạm thời (VD: tìm áo màu đen cho buổi này)
            - Preference chỉ liên quan đến 1 lần mua, không mang tính lâu dài
            - Thông tin nhạy cảm hoặc không liên quan mua sắm

            CRITICAL:
            - Chỉ lưu: size, màu yêu thích, màu không thích, phong cách, ngân sách, category yêu thích, fit, thương hiệu quan tâm.
            - Không lưu thông tin cá nhân nhạy cảm (địa chỉ, số điện thoại, v.v.).
            """)
    public String saveUserPreference(
            @P("Loại sở thích: 'size', 'color', 'style', 'budget', 'category', 'fit', 'brand'") String preferenceType,
            @P("Giá trị sở thích. VD: size='M', color='đen', style='minimal', budget='500000'") String preferenceValue) {
        try {
            log.info("Tool: saveUserPreference(type={}, value={})", preferenceType, preferenceValue);
            if (preferenceType == null || preferenceType.isBlank() || preferenceValue == null || preferenceValue.isBlank()) {
                return "Không đủ thông tin để lưu sở thích.";
            }

            ChatSession.PreferenceProfile profile = preferenceProfile();
            if (profile == null) {
                log.warn("saveUserPreference called but no preferenceProfile in context");
                return "Không thể lưu sở thích lúc này.";
            }

            String type = preferenceType.toLowerCase().trim();
            String value = preferenceValue.trim();

            switch (type) {
                case "size" -> profile.getPreferredSizes().add(value.toUpperCase());
                case "color" -> profile.getPreferredColors().add(value);
                case "style" -> profile.setStyle(value);
                case "budget" -> profile.setBudget(value);
                case "category" -> profile.getPreferredCategories().add(value);
                case "fit" -> profile.getFocusTags().add("fit:" + value);
                case "brand" -> profile.getFocusTags().add("brand:" + value);
                default -> {
                    log.debug("Unknown preference type '{}', skipping save", type);
                    return "Mình chưa hỗ trợ lưu loại sở thích '" + preferenceType + "' này.";
                }
            }

            log.info("Saved user preference: {}={}", type, value);
            return "Đã lưu sở thích của bạn: " + type + " = " + value + ". Mình sẽ ưu tiên gợi ý phù hợp hơn trong các lần tới!";
        } catch (Throwable ex) {
            log.error("saveUserPreference failed", ex);
            return "Mình chưa thể lưu sở thích lúc này.";
        }
    }

    // ========== PRIVATE HELPERS ==========

    @SuppressWarnings("unchecked")
    private List<ChatResponse.ProductSuggestion> mapProductPage(Map<String, Object> payload) {
        if (payload == null) return List.of();
        List<ChatResponse.ProductSuggestion> suggestions = new ArrayList<>();

        Object content = payload.get("content");
        if (!(content instanceof List<?> products)) return List.of();

        for (Object productObj : products) {
            if (!(productObj instanceof Map<?, ?> product)) continue;

            String productId = stringValue(product.get("id"));
            String name = stringValue(product.get("name"));
            String category = stringValue(product.get("categoryName"));
            BigDecimal minPrice = null;
            String imageUrl = "";
            String link = "";
            Set<String> availableSizes = new LinkedHashSet<>();
            Set<String> availableColors = new LinkedHashSet<>();

            Object variants = product.get("variants");
            if (variants instanceof List<?> variantList) {
                for (Object variantObj : variantList) {
                    if (!(variantObj instanceof Map<?, ?> variant)) continue;

                    String colorName = stringValue(variant.get("colorName"));
                    if (!colorName.isBlank()) availableColors.add(colorName);

                    BigDecimal price = toBigDecimal(variant.get("price"));
                    if (price != null && (minPrice == null || price.compareTo(minPrice) < 0)) {
                        minPrice = price;
                    }
                    // Không dùng variant.get("productUrl") vì DB có thể chứa link ngoài (Zara, H&M...)
                    if (imageUrl.isBlank()) {
                        Object images = variant.get("images");
                        if (images instanceof List<?> imgList && !imgList.isEmpty()) {
                            Object firstImg = imgList.get(0);
                            if (firstImg instanceof Map<?, ?> imgMap) {
                                imageUrl = stringValue(imgMap.get("imageUrl"));
                            }
                        }
                    }
                    Object sizes = variant.get("sizes");
                    if (sizes instanceof List<?> sizeList) {
                        for (Object sizeObj : sizeList) {
                            if (!(sizeObj instanceof Map<?, ?> sizeItem)) continue;
                            int quantity = toInt(sizeItem.get("quantity"));
                            String status = stringValue(sizeItem.get("status"));
                            if (quantity > 0 && !"hết hàng".equalsIgnoreCase(status)) {
                                availableSizes.add(stringValue(sizeItem.get("sizeName")).toUpperCase(Locale.ROOT));
                            }
                        }
                    }
                }
            }

            // Luôn dùng link nội bộ frontend, không lấy link ngoài từ DB
            link = "/products/" + productId;

            suggestions.add(ChatResponse.ProductSuggestion.builder()
                    .productId(productId)
                    .name(name)
                    .category(category)
                    .categoryGender(stringValue(product.get("categoryGender")))
                    .imageUrl(imageUrl)
                    .link(link)
                    .price(formatMoney(minPrice))
                    .availableSizes(new ArrayList<>(availableSizes))
                    .availableColors(new ArrayList<>(availableColors))
                    .reason("Kết quả tìm kiếm")
                    .build());
        }
        return suggestions;
    }

    @SuppressWarnings("unchecked")
    private ChatResponse.ProductSuggestion mapProductDetail(Map<String, Object> payload) {
        if (payload == null || payload.isEmpty()) {
            return null;
        }

        String productId = stringValue(payload.get("id"));
        String name = stringValue(payload.get("name"));
        String category = stringValue(payload.get("categoryName"));
        String categoryGender = stringValue(payload.get("categoryGender"));
        BigDecimal minPrice = null;
        String imageUrl = "";
        Set<String> availableSizes = new LinkedHashSet<>();
        Set<String> availableColors = new LinkedHashSet<>();

        Object variants = payload.get("variants");
        if (variants instanceof List<?> variantList) {
            for (Object variantObj : variantList) {
                if (!(variantObj instanceof Map<?, ?> variant)) continue;

                String colorName = stringValue(variant.get("colorName"));
                if (!colorName.isBlank()) {
                    availableColors.add(colorName);
                }

                BigDecimal price = toBigDecimal(variant.get("price"));
                if (price != null && (minPrice == null || price.compareTo(minPrice) < 0)) {
                    minPrice = price;
                }

                if (imageUrl.isBlank()) {
                    Object images = variant.get("images");
                    if (images instanceof List<?> imgList && !imgList.isEmpty()) {
                        Object firstImg = imgList.get(0);
                        if (firstImg instanceof Map<?, ?> imgMap) {
                            imageUrl = stringValue(imgMap.get("imageUrl"));
                        }
                    }
                }

                Object sizes = variant.get("sizes");
                if (sizes instanceof List<?> sizeList) {
                    for (Object sizeObj : sizeList) {
                        if (!(sizeObj instanceof Map<?, ?> sizeItem)) continue;
                        int quantity = toInt(sizeItem.get("quantity"));
                        String status = stringValue(sizeItem.get("status"));
                        if (quantity > 0 && !"hết hàng".equalsIgnoreCase(status)) {
                            availableSizes.add(stringValue(sizeItem.get("sizeName")).toUpperCase(Locale.ROOT));
                        }
                    }
                }
            }
        }

        return ChatResponse.ProductSuggestion.builder()
                .productId(productId)
                .name(name)
                .category(category)
                .categoryGender(categoryGender)
                .imageUrl(imageUrl)
                .link(productId.isBlank() ? "" : "/products/" + productId)
                .price(formatMoney(minPrice))
                .availableSizes(new ArrayList<>(availableSizes))
                .availableColors(new ArrayList<>(availableColors))
                .reason("Chi tiết sản phẩm được người dùng chọn")
                .build();
    }

    @SuppressWarnings("unchecked")
    private List<ChatResponse.PromotionSuggestion> mapPromotions(Object payload) {
        List<ChatResponse.PromotionSuggestion> promos = new ArrayList<>();
        if (!(payload instanceof List<?> list)) return promos;

        for (Object obj : list) {
            if (!(obj instanceof Map<?, ?> item)) continue;
            promos.add(ChatResponse.PromotionSuggestion.builder()
                    .code(stringValue(item.get("code")))
                    .discountType(stringValue(item.get("discountType")))
                    .discountValue(stringValue(item.get("discountValue")))
                    .minOrderAmount(stringValue(item.get("minOrderAmount")))
                    .endDate(stringValue(item.get("endDate")))
                    .note("Khuyến mãi đang hiệu lực")
                    .build());
        }
        return promos;
    }

    private String formatMoney(BigDecimal value) {
        if (value == null) return "";
        DecimalFormat format = (DecimalFormat) DecimalFormat.getInstance(Locale.forLanguageTag("vi-VN"));
        format.setMaximumFractionDigits(0);
        format.setGroupingUsed(true);
        return format.format(value.setScale(0, RoundingMode.HALF_UP)) + " đ";
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private String joinOrFallback(List<String> values, String fallback) {
        if (values == null || values.isEmpty()) {
            return fallback;
        }
        return String.join(", ", values);
    }

    private int toInt(Object value) {
        if (value == null) return 0;
        if (value instanceof Number n) return n.intValue();
        try { return Integer.parseInt(String.valueOf(value)); } catch (Exception e) { return 0; }
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) return null;
        if (value instanceof BigDecimal bd) return bd;
        if (value instanceof Number n) return BigDecimal.valueOf(n.doubleValue());
        try { return new BigDecimal(String.valueOf(value)); } catch (Exception e) { return null; }
    }

    private Long parseLongSafe(String value) {
        if (value == null || value.trim().isEmpty() || "null".equalsIgnoreCase(value.trim())) {
            return null;
        }
        try {
            return (long) Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Integer parseIntegerSafe(String value) {
        if (value == null || value.trim().isEmpty() || "null".equalsIgnoreCase(value.trim())) {
            return null;
        }
        try {
            return (int) Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}

