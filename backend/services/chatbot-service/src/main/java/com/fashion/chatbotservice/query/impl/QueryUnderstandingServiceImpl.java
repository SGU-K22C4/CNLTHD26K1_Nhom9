package com.fashion.chatbotservice.query.impl;

import com.fashion.chatbotservice.model.ChatSession;
import com.fashion.chatbotservice.query.EnrichedQuery;
import com.fashion.chatbotservice.query.QueryUnderstandingService;
import com.fashion.chatbotservice.util.VietnameseNormalizer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Triển khai Query Understanding Service — Phase 2B.
 *
 * <p>Thực hiện:
 * <ol>
 *   <li>Trích xuất entities: products, colors, sizes, budget, occasion.</li>
 *   <li>Giải quyết context: nếu message thiếu product type → lấy từ session.</li>
 *   <li>Normalize query: tiếng Việt không dấu.</li>
 *   <li>Phát hiện refinement và comparison patterns.</li>
 * </ol>
 *
 * <p>Ví dụ:
 * <pre>
 * Input:  "Có mẫu tối màu ko?"
 * Output: { products: ["áo sơ mi"], colors: ["đen","xám","navy"],
 *           isRefinement: true }
 * </pre>
 */
@Service
@Slf4j
public class QueryUnderstandingServiceImpl implements QueryUnderstandingService {

    // ── Product keywords — must match actual DB category names (normalized, no-accent)
    private static final List<String> PRODUCT_KEYWORDS = List.of(
            // Male categories (DB exact names: Áo sơ mi, Áo phông, Quần Jeans, Áo khoac)
            "ao so mi", "so mi",
            "ao phong", "ao thun",
            "quan jeans", "quan jean", "jeans", "jean", "denim",
            "ao khoac", "jacket", "blazer", "bomber", "parka", "biker",
            "hoodie", "ao hoodie", "ao polo", "polo",
            // Female categories (DB exact names: Đầm, Chân váy, Áo sơ mi, Áo khoac)
            "dam", "vay", "chan vay", "jumpsuit",
            "ao kieu", "blouse",
            // General
            "giay", "dep", "tui xach", "non", "mu", "that lung", "dong ho",
            "ao len", "ao gi le", "gi le"
    );

    // ── Color keywords — normalized (no-accent) key → real DB colorName values
    // Sourced from: SELECT DISTINCT color_name FROM fashion_product_db.product_variants
    private static final Map<String, List<String>> COLOR_ALIASES = new LinkedHashMap<>() {{
        // Exact DB single-color mapping
        put("mau den",         List.of("Màu đen", "Màu đen/Trắng", "Đen", "Black"));
        put("den",             List.of("Màu đen", "Đen", "Black"));
        put("mau trang",       List.of("Màu trắng", "Màu trắng ngà", "Trắng", "White"));
        put("trang",           List.of("Màu trắng", "Trắng", "White"));
        put("mau nau",         List.of("Màu nâu", "Màu nâu đậm", "Màu nâu nhạt dịu", "Màu sôcôla", "Màu caramel"));
        put("nau",             List.of("Màu nâu", "Nâu vàng"));
        put("mau xanh duong",  List.of("Xanh dương", "Màu xanh dương đậm", "Xanh dương/Chàm", "Blue"));
        put("xanh duong",      List.of("Xanh dương", "Màu xanh dương đậm", "Blue"));
        put("xanh dam",        List.of("Xanh Đậm", "Màu xanh dương đậm", "Màu xanh hải quân đậm", "Màu chàm đậm"));
        put("xanh nhat",       List.of("Xanh nhạt", "Màu xanh nhạt", "Màu xanh dịu"));
        put("xanh hai quan",   List.of("Màu xanh hải quân đậm", "Navy", "Màu xanh mực"));
        put("navy",            List.of("Navy", "Màu xanh hải quân đậm", "Màu xanh mực"));
        put("xanh nuoc bien",  List.of("Màu xanh nước biển", "Màu xanh hải quân đậm"));
        put("xanh la",         List.of("Xanh lục", "Màu xanh lá cây đậm", "Màu xanh lá dịu", "Xanh lục nhạt"));
        put("xanh o liu",      List.of("Màu xanh ô liu", "Olive"));
        put("xanh co vit",     List.of("Màu xanh cổ vịt"));
        put("xanh cuu long",   List.of("Màu xanh cửu long"));
        put("xanh da troi",    List.of("Màu xanh da trời", "Màu xanh biển"));
        put("xanh",            List.of("Xanh dương", "Màu xanh nước biển", "Màu xanh dịu", "Xanh nhạt", "Xanh Đậm"));
        put("mau xam",         List.of("Màu xám", "Màu xám đá", "Màu xám đậm", "Màu xám ngọc trai", "Màu xám nhạt", "Màu xám than", "Gray"));
        put("xam",             List.of("Màu xám", "Gray"));
        put("ghi",             List.of("Màu xám ngọc trai", "Màu xám"));
        put("kem",             List.of("Kem", "Màu kem", "Màu trắng ngà"));
        put("be",              List.of("Màu be", "Màu be đậm", "Màu be nhạt", "Kem"));
        put("mau hong",        List.of("Màu hồng", "Màu hồng nhạt pha tím"));
        put("hong",            List.of("Màu hồng"));
        put("mau vang",        List.of("Màu vàng", "Màu vàng bơ", "Màu vàng bò dịu", "Màu vàng bò đậm", "Màu vàng cát"));
        put("vang",            List.of("Màu vàng", "Màu vàng bơ"));
        put("vang kaki",       List.of("Màu vàng kaki đậm", "Màu vàng kaki nhạt", "Nâu vàng"));
        put("kaki",            List.of("Kaki", "Màu vàng kaki nhạt", "Màu vàng kaki đậm"));
        put("do ruou",         List.of("Đỏ Rượu", "Màu đỏ rượu"));
        put("do",              List.of("Màu đỏ/Đen", "Màu cam"));
        put("cam",             List.of("Màu cam"));
        put("tim",             List.of("Màu tím cà"));
        put("than cui",        List.of("Than củi"));
        put("thuoc la",        List.of("Thuốc lá"));
        put("mau cham",        List.of("Nước biển", "sọc"));
        put("nhieu mau",       List.of("Nhiều màu"));
        // Abstract / descriptive color groups
        put("toi mau",         List.of("Màu đen", "Màu xám than", "Màu xám đậm", "Màu chàm đậm", "Than củi"));
        put("sang mau",        List.of("Màu trắng", "Kem", "Màu be nhạt", "Màu trắng ngà"));
        put("trung tinh",      List.of("Màu đen", "Màu trắng", "Màu xám", "Màu be", "Navy", "Kem"));
        put("pastel",          List.of("Màu hồng nhạt pha tím", "Màu xanh dịu", "Màu be nhạt", "Màu vàng bơ"));
    }};

    // ── Size patterns
    private static final List<String> SIZE_KEYWORDS = List.of("xs", "s", "m", "l", "xl", "xxl", "2xl", "3xl");

    // ── Occasion keywords
    private static final Map<String, String> OCCASION_KEYWORDS = new LinkedHashMap<>() {{
        put("di lam", "đi làm");
        put("van phong", "văn phòng");
        put("di tiec", "đi tiệc");
        put("du lich", "du lịch");
        put("di choi", "đi chơi");
        put("hang ngay", "hằng ngày");
        put("the thao", "thể thao");
        put("dao pho", "dạo phố");
    }};

    // ── Budget patterns
    private static final Pattern BUDGET_PATTERN = Pattern.compile(
            "(?:duoi|tu|khoang|tren|gia|budget|<|>)?\\s*(\\d+(?:[,.]\\d+)?)\\s*(k|trieu|tr|000)?",
            Pattern.CASE_INSENSITIVE);

    // ── Refinement signals
    private static final List<String> REFINEMENT_SIGNALS = List.of(
            "con", "the", "the nao", "mau khac", "mau gi", "size nao",
            "khong", "nua", "them", "ngoai ra", "hay la"
    );

    // ── Comparison signals
    private static final List<String> COMPARISON_SIGNALS = List.of(
            "so sanh", "hon", "va", "hay", "phan van", "chon cai nao",
            "cai nao tot hon", "nen chon", "khac nhau"
    );

    @Override
    public EnrichedQuery understand(String query, ChatSession session) {
        if (query == null || query.isBlank()) {
            return EnrichedQuery.builder().normalizedQuery("").build();
        }

        String normalized = VietnameseNormalizer.normalize(query).toLowerCase();

        List<String> products  = extractProductMentions(normalized);
        List<String> colors    = extractColors(normalized);
        List<String> sizes     = extractSizes(normalized);
        double[] budget        = extractBudget(normalized);
        String occasion        = extractOccasion(normalized);
        boolean isRefinement   = detectRefinement(normalized);
        boolean isComparison   = detectComparison(normalized);

        // Context resolution: nếu thiếu product type → lấy từ session
        if (products.isEmpty() && session != null
                && session.getPreferenceProfile() != null
                && session.getPreferenceProfile().getLastProductCategoryQueried() != null) {
            products = List.of(session.getPreferenceProfile().getLastProductCategoryQueried());
            if (!isRefinement) isRefinement = true; // refinement nếu dùng context
        }

        EnrichedQuery enriched = EnrichedQuery.builder()
                .products(products)
                .colors(colors)
                .sizes(sizes)
                .minBudget(budget[0] > 0 ? budget[0] : null)
                .maxBudget(budget[1] > 0 ? budget[1] : null)
                .occasion(occasion)
                .normalizedQuery(normalized)
                .isRefinement(isRefinement)
                .isComparison(isComparison)
                .build();

        log.debug("EnrichedQuery: products={}, colors={}, sizes={}, budget={}-{}, occasion={}, refinement={}",
                products, colors, sizes, budget[0], budget[1], occasion, isRefinement);

        return enriched;
    }

    // ─────────────────────────────────────────────────────────────
    // Private helpers
    // ─────────────────────────────────────────────────────────────

    private List<String> extractProductMentions(String normalized) {
        List<String> found = new ArrayList<>();
        for (String kw : PRODUCT_KEYWORDS) {
            if (normalized.contains(kw)) found.add(kw);
        }
        return found;
    }

    private List<String> extractColors(String normalized) {
        Set<String> colors = new LinkedHashSet<>();
        // Iterate longest key first to prevent short key "xanh" stealing match from "xanh dam"
        List<String> sortedKeys = COLOR_ALIASES.keySet().stream()
                .sorted((a, b) -> Integer.compare(b.length(), a.length()))
                .toList();
        Set<String> usedPositions = new java.util.HashSet<>();
        for (String key : sortedKeys) {
            int idx = normalized.indexOf(key);
            if (idx < 0) continue;
            // Ensure it is a whole-word match (not substring of a longer color token already matched)
            boolean alreadyCovered = usedPositions.stream().anyMatch(pos -> {
                int[] p = parsePos(pos);
                return p[0] <= idx && idx + key.length() <= p[1];
            });
            if (!alreadyCovered) {
                usedPositions.add(idx + ":" + (idx + key.length()));
                colors.addAll(COLOR_ALIASES.get(key));
            }
        }
        return new ArrayList<>(colors);
    }

    private int[] parsePos(String pos) {
        String[] parts = pos.split(":");
        return new int[]{ Integer.parseInt(parts[0]), Integer.parseInt(parts[1]) };
    }

    private List<String> extractSizes(String normalized) {
        List<String> found = new ArrayList<>();
        for (String size : SIZE_KEYWORDS) {
            // Match as whole word: " m " or " m," etc.
            if (normalized.matches(".*\\b" + size + "\\b.*")) {
                found.add(size.toUpperCase());
            }
        }
        return found;
    }

    /**
     * Trả về [minBudget, maxBudget] (VND). 0 nếu không tìm thấy.
     */
    private double[] extractBudget(String normalized) {
        double min = 0, max = 0;
        Matcher m = BUDGET_PATTERN.matcher(normalized);
        List<Double> values = new ArrayList<>();
        while (m.find()) {
            try {
                String numStr = m.group(1).replace(",", "").replace(".", "");
                String unit   = m.group(2);
                double val    = Double.parseDouble(numStr);
                if (unit != null && (unit.startsWith("tr"))) val *= 1_000_000;
                else if (unit != null && unit.equals("k"))   val *= 1_000;
                else if (val <= 9999)                        val *= 1_000;
                if (val >= 10_000) values.add(val);
            } catch (NumberFormatException ignored) {}
        }
        if (values.size() == 1) max = values.get(0);
        else if (values.size() >= 2) {
            min = Math.min(values.get(0), values.get(1));
            max = Math.max(values.get(0), values.get(1));
        }
        return new double[]{min, max};
    }

    private String extractOccasion(String normalized) {
        for (Map.Entry<String, String> entry : OCCASION_KEYWORDS.entrySet()) {
            if (normalized.contains(entry.getKey())) return entry.getValue();
        }
        return null;
    }

    private boolean detectRefinement(String normalized) {
        return REFINEMENT_SIGNALS.stream().anyMatch(normalized::contains);
    }

    private boolean detectComparison(String normalized) {
        return COMPARISON_SIGNALS.stream().anyMatch(normalized::contains);
    }
}
