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

    // ── Product keywords
    private static final List<String> PRODUCT_KEYWORDS = List.of(
            "ao thun", "ao so mi", "ao khoac", "ao hoodie", "ao polo", "ao len",
            "quan jean", "quan tay", "quan short", "quan kaki",
            "vay", "dam", "chan vay",
            "giay", "dep", "tui", "non", "mu", "that lung", "dong ho"
    );

    // ── Color keywords mapped to canonical colors
    private static final Map<String, List<String>> COLOR_ALIASES = new LinkedHashMap<>() {{
        put("den", List.of("đen"));
        put("trang", List.of("trắng"));
        put("xanh", List.of("xanh"));
        put("navy", List.of("navy", "xanh navy"));
        put("do", List.of("đỏ"));
        put("hong", List.of("hồng"));
        put("vang", List.of("vàng"));
        put("be", List.of("be", "kem"));
        put("kem", List.of("kem", "be"));
        put("xam", List.of("xám"));
        put("ghi", List.of("xám ghi"));
        put("nau", List.of("nâu"));
        put("toi mau", List.of("đen", "xám", "navy"));
        put("sang mau", List.of("trắng", "be", "kem"));
        put("trung tinh", List.of("đen", "trắng", "xám", "be", "navy"));
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
        for (Map.Entry<String, List<String>> entry : COLOR_ALIASES.entrySet()) {
            if (normalized.contains(entry.getKey())) {
                colors.addAll(entry.getValue());
            }
        }
        return new ArrayList<>(colors);
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
