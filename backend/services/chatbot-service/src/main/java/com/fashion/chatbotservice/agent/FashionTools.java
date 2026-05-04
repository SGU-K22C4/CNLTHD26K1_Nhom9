package com.fashion.chatbotservice.agent;

import com.fashion.chatbotservice.dto.ChatResponse;
import com.fashion.chatbotservice.model.ChatSession;
import com.fashion.chatbotservice.service.KnowledgeBaseService;
import com.fashion.chatbotservice.service.OutfitRuleEngine;
import com.fashion.chatbotservice.service.SizeAdvisorService;
import com.fashion.chatbotservice.util.VietnameseNormalizer;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
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

    @Value("${chatbot.product-service-url:http://localhost:8080}")
    private String productServiceUrl;

    @Value("${chatbot.promotion-service-url:http://localhost:8080}")
    private String promotionServiceUrl;

    @Value("${chatbot.order-service-url:http://localhost:8080}")
    private String orderServiceUrl;

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

    public FashionTools(WebClient webClient,
                        SizeAdvisorService sizeAdvisorService,
                        OutfitRuleEngine outfitRuleEngine,
                        KnowledgeBaseService knowledgeBaseService) {
        this.webClient = webClient;
        this.sizeAdvisorService = sizeAdvisorService;
        this.outfitRuleEngine = outfitRuleEngine;
        this.knowledgeBaseService = knowledgeBaseService;
    }

    public void setCollector(ToolResultCollector collector) {
        this.collectorHolder.set(collector);
    }

    public void clearCollector() {
        this.collectorHolder.remove();
        this.preferenceHolder.remove();
    }

    public void setPreferenceProfile(ChatSession.PreferenceProfile profile) {
        this.preferenceHolder.set(profile);
    }

    private ToolResultCollector collector() {
        return collectorHolder.get();
    }

    private ChatSession.PreferenceProfile preferenceProfile() {
        return preferenceHolder.get();
    }

    // ========== PRODUCT TOOLS ==========

    @Tool("""
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
    public String searchProducts(
            @P("Từ khóa tìm kiếm: CHỈ tên loại đồ (VD: 'quần jean', 'áo thun', 'váy đầm'). KHÔNG gộp màu/giá/giới tính") String search,
            @P("Giá tối thiểu (VND), null nếu không giới hạn. VD: 300k=300000") Long minPrice,
            @P("Giá tối đa (VND), null nếu không giới hạn. VD: 500k=500000, 2 triệu=2000000") Long maxPrice,
            @P("Màu sắc sản phẩm nếu user có đề cập (VD: 'đen', 'trắng', 'xanh'). null nếu không đề cập.") String color,
            @P("Size nếu user có đề cập (VD: 'S', 'M', 'L', 'XL'). null nếu không đề cập.") String size) {
        return searchProductsInternal(search, minPrice, maxPrice, color, size, true);
    }

    @Tool("""
            Tìm kiếm sản phẩm theo TỪ KHÓA CỤ THỂ, KHÔNG fallback rút gọn.
            Dùng khi user hỏi có mẫu X hay không, hoặc muốn kiểm tra tên sản phẩm cụ thể.

            CRITICAL:
            - Không tự động rút gọn từ khóa.
            - Nếu không có kết quả, phải trả lời rõ ràng là không có sản phẩm đó trong hệ thống.
            """)
    public String searchProductsStrict(
            @P("Từ khóa tìm kiếm: càng gần với tên sản phẩm càng tốt") String search,
            @P("Giá tối thiểu (VND), null nếu không giới hạn") Long minPrice,
            @P("Giá tối đa (VND), null nếu không giới hạn") Long maxPrice,
            @P("Màu sắc nếu user đề cập (VD: 'đen', 'trắng'). null nếu không đề cập.") String color,
            @P("Size nếu user có đề cập (VD: 'S', 'M', 'L', 'XL'). null nếu không đề cập.") String size) {
        return searchProductsInternal(search, minPrice, maxPrice, color, size, false);
    }

    @Tool("""
            Duyệt danh sách sản phẩm để tư vấn khi user không nêu rõ từ khóa cụ thể.
            Dùng cho các câu như: "tư vấn áo khoác", "gợi ý đồ đi làm", "có mẫu nào phù hợp".

            CRITICAL:
            - Không yêu cầu từ khóa bắt buộc. Hãy dùng filter giá/size/màu nếu có.
            - Nếu không có sản phẩm phù hợp, phải nói rõ là không có.
            """)
    public String browseProducts(
            @P("Giá tối thiểu (VND), null nếu không giới hạn") Long minPrice,
            @P("Giá tối đa (VND), null nếu không giới hạn") Long maxPrice,
            @P("Màu sắc nếu user có đề cập (VD: 'đen', 'trắng'). null nếu không đề cập.") String color,
            @P("Size nếu user có đề cập (VD: 'S', 'M', 'L', 'XL'). null nếu không đề cập.") String size) {
        try {
            List<ChatResponse.ProductSuggestion> suggestions = executeBrowse(minPrice, maxPrice, 12);
            suggestions = applyPersonalization(suggestions, preferenceProfile(), minPrice, maxPrice);
            suggestions = applySizeFilter(suggestions, size);
            suggestions = diversifySuggestionsByCategory(suggestions, 12);

            if (collector() != null) collector().addProducts(suggestions);

            if (suggestions.isEmpty()) {
                return "Hiện chưa có sản phẩm phù hợp với yêu cầu này.";
            }

            StringBuilder result = new StringBuilder("Mình tìm thấy " + suggestions.size()
                    + " sản phẩm phù hợp để bạn tham khảo:\n");
            for (var s : suggestions) {
                List<String> colors = safeList(s.getAvailableColors());
                List<String> sizes = safeList(s.getAvailableSizes());

                result.append("- ").append(s.getName())
                        .append(" | Giá: ").append(s.getPrice())
                        .append(" | Size: ").append(String.join(", ", sizes))
                        .append(" | Màu: ").append(String.join(", ", colors))
                        .append("\n");
            }
            return result.toString();
        } catch (Exception ex) {
            log.warn("browseProducts failed: {}", ex.getMessage());
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
                String groupLabel = resolveGroupLabel(normalizedHint);
                return "Các loại " + groupLabel + " hiện có: " + String.join(", ", filteredTypes)
                        + ". Bạn muốn mình tư vấn loại nào ạ?";
            }

            return formatGroupedTypeList(filteredTypes);
        } catch (Exception ex) {
            log.warn("listProductTypes failed: {}", ex.getMessage());
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

            suggestions = applyPersonalization(suggestions, preferenceProfile(), minPrice, maxPrice);
            suggestions = applySizeFilter(suggestions, size);
            suggestions = diversifySuggestionsByCategory(suggestions, 12);
            if (collector() != null) collector().addProducts(suggestions);

            if (suggestions.isEmpty()) {
                if (allowFallback) {
                    if (size != null && !size.isBlank()) {
                        return "Hiện chưa có sản phẩm phù hợp với size " + size + ". Bạn muốn mình gợi ý mẫu khác không ạ?";
                    }
                    return "Không tìm thấy sản phẩm phù hợp sau khi đã tìm theo từ khóa và quét danh mục hiện có. "
                            + "Bạn có thể mô tả thêm kiểu dáng/màu/chất liệu để mình tìm lại.";
                }
                if (size != null && !size.isBlank()) {
                    return "Hiện chưa có sản phẩm khớp với từ khóa bạn cung cấp và size " + size + ".";
                }
                return "Hiện chưa có sản phẩm khớp với từ khóa bạn cung cấp trong hệ thống.";
            }

            // Post-filter: highlight color matches if user specified color
            StringBuilder result = new StringBuilder("Tìm thấy " + suggestions.size() + " sản phẩm:\n");
            for (var s : suggestions) {
                List<String> colors = safeList(s.getAvailableColors());
                List<String> sizes = safeList(s.getAvailableSizes());
                boolean colorMatch = (color != null && !color.isBlank())
                        && colors.stream()
                                .anyMatch(c -> normalizeText(c).contains(normalizeText(color)));

                result.append("- ").append(s.getName())
                        .append(" | Giá: ").append(s.getPrice())
                        .append(" | Size: ").append(String.join(", ", sizes))
                        .append(" | Màu: ").append(String.join(", ", colors));
                if (colorMatch) result.append(" ✓ (có màu ").append(color).append(")");
                result.append("\n");
            }
            return result.toString();
        } catch (Exception ex) {
            log.warn("searchProducts failed: {}", ex.getMessage());
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

        Map<String, Object> payload = webClient.get()
                .uri(uri.toString())
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(Map.class)
                .timeout(TOOL_TIMEOUT)
                .block();

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

    private List<ChatResponse.ProductSuggestion> applyPersonalization(
            List<ChatResponse.ProductSuggestion> suggestions,
            ChatSession.PreferenceProfile profile,
            Long minPrice,
            Long maxPrice) {
        if (suggestions == null || suggestions.isEmpty() || profile == null) return suggestions;

        List<ScoredSuggestion> scored = new ArrayList<>();
        Long preferredMaxPrice = (maxPrice != null) ? maxPrice : parseBudget(profile.getBudget());

        int index = 0;
        for (ChatResponse.ProductSuggestion s : suggestions) {
            double score = 0.0;
            List<String> reasons = new ArrayList<>();

            boolean sizeMatch = hasAnyMatch(profile.getPreferredSizes(), s.getAvailableSizes());
            if (sizeMatch) {
                score += 1.8;
                reasons.add("Hợp size bạn hay mặc");
            }

            boolean colorMatch = hasAnyMatch(profile.getPreferredColors(), s.getAvailableColors());
            if (colorMatch) {
                score += 2.2;
                reasons.add("Phù hợp màu bạn thích");
            }

            boolean categoryMatch = hasCategoryMatch(profile.getPreferredCategories(), s.getCategory());
            if (categoryMatch) {
                score += 1.2;
                reasons.add("Đúng loại đồ bạn quan tâm");
            }

            Long productPrice = parsePriceToLong(s.getPrice());
            boolean budgetMatch = preferredMaxPrice != null && productPrice != null && productPrice <= preferredMaxPrice;
            if (budgetMatch) {
                score += 0.8;
                reasons.add("Phù hợp ngân sách");
            }

            if (!reasons.isEmpty()) {
                s.setReason(String.join(" | ", reasons));
            }

            scored.add(new ScoredSuggestion(s, score, index++));
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

    private List<ChatResponse.ProductSuggestion> applySizeFilter(
            List<ChatResponse.ProductSuggestion> suggestions,
            String size) {
        if (suggestions == null || suggestions.isEmpty() || size == null || size.isBlank()) {
            return suggestions;
        }
        String normalizedSize = normalizeText(size);
        return suggestions.stream()
                .filter(s -> safeList(s.getAvailableSizes()).stream()
                        .anyMatch(avail -> normalizeText(avail).equals(normalizedSize)))
                .toList();
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

        return webClient.get()
                .uri(uri.toString())
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(Map.class)
                .timeout(TOOL_TIMEOUT)
                .block();
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
                types.addAll(extractTypeLabels(product.getName(), product.getCategory()));
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

        String targetGroup = resolveGroupLabel(groupHint);
        List<String> filtered = new ArrayList<>();
        for (String type : allTypes) {
            if (resolveGroupLabel(type).equals(targetGroup)) {
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

        Map<String, List<ChatResponse.ProductSuggestion>> byCategory = new LinkedHashMap<>();
        for (ChatResponse.ProductSuggestion suggestion : suggestions) {
            String key = suggestion.getCategory() == null || suggestion.getCategory().isBlank()
                    ? "Khác"
                    : suggestion.getCategory();
            byCategory.computeIfAbsent(key, ignored -> new ArrayList<>()).add(suggestion);
        }

        List<ChatResponse.ProductSuggestion> diversified = new ArrayList<>();
        int target = Math.min(maxResults, suggestions.size());
        int index = 0;

        while (diversified.size() < target) {
            boolean added = false;
            for (List<ChatResponse.ProductSuggestion> bucket : byCategory.values()) {
                if (index < bucket.size()) {
                    diversified.add(bucket.get(index));
                    added = true;
                    if (diversified.size() >= target) break;
                }
            }
            if (!added) break;
            index++;
        }

        return diversified.isEmpty() ? suggestions : diversified;
    }

    private List<String> safeList(List<String> values) {
        return values == null ? List.of() : values;
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
            @SuppressWarnings("unchecked")
            Map<String, Object> order = webClient.get()
                    .uri(orderServiceUrl + "/api/v1/orders/by-number/{number}", orderNumber)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(TOOL_TIMEOUT)
                    .block();

            if (order == null) return "Không tìm thấy đơn hàng " + orderNumber;

            String status = stringValue(order.get("status"));
            String totalAmount = stringValue(order.get("totalAmount"));

            return "Đơn hàng " + orderNumber + ": trạng thái " + status + ", tổng tiền " + totalAmount + " VND.";
        } catch (Exception ex) {
            log.warn("checkOrderByNumber failed: {}", ex.getMessage());
            return "Dịch vụ đơn hàng tạm thời không phản hồi. Vui lòng thử lại sau.";
        }
    }

    // ========== PROMOTION TOOLS ==========

    @Tool("""
            Kiểm tra mã giảm giá / coupon có hợp lệ với đơn hàng không.
            Gọi khi người dùng hỏi về mã giảm giá, coupon, voucher.
            VD: 'mã SALE20 dùng được không', 'coupon cho đơn 1 triệu'.

            CRITICAL: Do NOT invent or guess the Coupon Code or Order Amount.
            If the user has not explicitly provided BOTH the coupon code AND the order amount,
            you MUST ask them to provide the missing information before calling this tool.
            """)
    public String validateCoupon(
            @P("Mã coupon") String code,
            @P("Giá trị đơn hàng (VND)") Long orderAmount) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> result = webClient.post()
                    .uri(promotionServiceUrl + "/api/v1/promotions/validate?code={code}&orderAmount={amount}",
                            code, orderAmount)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(TOOL_TIMEOUT)
                    .block();

            if (result == null) return "Không thể kiểm tra mã " + code;

            boolean valid = Boolean.TRUE.equals(result.get("valid"));
            if (valid) {
                return "Mã " + code + " hợp lệ! Giảm " + result.get("discountAmount") + " VND cho đơn " + orderAmount + " VND.";
            } else {
                String reason = stringValue(result.get("message"));
                return "Mã " + code + " không hợp lệ: " + (reason.isBlank() ? "không đạt điều kiện" : reason);
            }
        } catch (Exception ex) {
            log.warn("validateCoupon failed: {}", ex.getMessage());
            return "Dịch vụ khuyến mãi tạm thời không phản hồi. Vui lòng thử lại sau.";
        }
    }

    @Tool("""
            Lấy danh sách khuyến mãi đang hiệu lực.
            Gọi khi người dùng hỏi về chương trình khuyến mãi, giảm giá, deal.
            """)
    public String getActivePromotions() {
        try {
            @SuppressWarnings("unchecked")
            Object payload = webClient.get()
                    .uri(promotionServiceUrl + "/api/v1/promotions/active")
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .bodyToMono(Object.class)
                    .timeout(TOOL_TIMEOUT)
                    .block();

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
        } catch (Exception ex) {
            log.warn("getActivePromotions failed: {}", ex.getMessage());
            return "Dịch vụ khuyến mãi tạm thời không phản hồi. Vui lòng thử lại sau.";
        }
    }

    // ========== SIZE CONSULTATION ==========

    @Tool("""
            Tư vấn size thời trang dựa trên số đo cơ thể.
            Gọi khi người dùng cung cấp chiều cao, cân nặng, vòng ngực/eo/hông
            và muốn biết nên mặc size gì.

            CRITICAL: Only pass measurement values the user has EXPLICITLY stated.
            Use null for any measurement NOT mentioned by the user.
            Do NOT guess height, weight, or body measurements.
            """)
    public String consultSize(
            @P("Chiều cao (cm)") Integer heightCm,
            @P("Cân nặng (kg)") Integer weightKg,
            @P("Vòng ngực (cm), null nếu không có") Integer chestCm,
            @P("Vòng eo (cm), null nếu không có") Integer waistCm,
            @P("Vòng hông (cm), null nếu không có") Integer hipCm,
            @P("Loại đồ: 'top' (áo) hoặc 'bottom' (quần/váy)") String garmentType) {

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

        SizeAdvisorService.SizeResult result = sizeAdvisorService.suggest(m, type);
        if (collector() != null) collector().setSizeRecommendation(result.recommendedSize());

        return "Gợi ý size " + result.recommendedSize() + ". " + result.note();
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

        List<String> queries = outfitRuleEngine.buildQueries(occasion, style);

        StringBuilder allResults = new StringBuilder();

        for (String query : queries) {
            String toolResult = searchProducts(query, null, null, null, null);
            allResults.append(toolResult).append("\n");
        }

        return "Gợi ý outfit cho " + occasion
                + (style != null ? " (phong cách " + style + ")" : "")
                + ":\n" + allResults;
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

        List<KnowledgeBaseService.SearchResult> results = knowledgeBaseService.search(query);

        if (results.isEmpty()) {
            return "Không tìm thấy thông tin liên quan trong knowledge base. "
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
}

