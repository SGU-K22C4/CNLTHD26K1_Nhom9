package com.fashion.chatbotservice.agent;

import com.fashion.chatbotservice.dto.ChatResponse;
import com.fashion.chatbotservice.service.KnowledgeBaseService;
import com.fashion.chatbotservice.service.OutfitRuleEngine;
import com.fashion.chatbotservice.service.SizeAdvisorService;
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

    /**
     * ThreadLocal để đảm bảo thread-safety khi nhiều request đồng thời.
     * Mỗi thread (request) sẽ có collector riêng, tránh race condition.
     */
    private final ThreadLocal<ToolResultCollector> collectorHolder = new ThreadLocal<>();

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
    }

    private ToolResultCollector collector() {
        return collectorHolder.get();
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
            @P("Màu sắc sản phẩm nếu user có đề cập (VD: 'đen', 'trắng', 'xanh'). null nếu không đề cập.") String color) {
        try {
            // Try search with original keyword first
            List<ChatResponse.ProductSuggestion> suggestions = executeSearch(search, minPrice, maxPrice);

            // If no results, try progressively shorter keywords (fallback)
            if (suggestions.isEmpty() && search != null) {
                for (String fallback : generateFallbackKeywords(search)) {
                    suggestions = executeSearch(fallback, minPrice, maxPrice);
                    if (!suggestions.isEmpty()) {
                        log.info("Fallback search succeeded with keyword: '{}'", fallback);
                        break;
                    }
                }
            }

            if (collector() != null) collector().addProducts(suggestions);

            if (suggestions.isEmpty()) {
                return "Không tìm thấy sản phẩm nào phù hợp. Bạn có thể thử từ khóa khác (VD: 'áo thun', 'quần jean', 'váy').";
            }

            // Post-filter: highlight color matches if user specified color
            StringBuilder result = new StringBuilder("Tìm thấy " + suggestions.size() + " sản phẩm:\n");
            for (var s : suggestions) {
                boolean colorMatch = (color != null && !color.isBlank())
                        && s.getAvailableColors().stream()
                                .anyMatch(c -> c.toLowerCase(Locale.ROOT).contains(color.toLowerCase(Locale.ROOT)));

                result.append("- ").append(s.getName())
                        .append(" | Giá: ").append(s.getPrice())
                        .append(" | Size: ").append(String.join(", ", s.getAvailableSizes()))
                        .append(" | Màu: ").append(String.join(", ", s.getAvailableColors()));
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
            String toolResult = searchProducts(query, null, null, null);
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
