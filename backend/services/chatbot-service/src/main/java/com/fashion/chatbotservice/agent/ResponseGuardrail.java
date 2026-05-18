package com.fashion.chatbotservice.agent;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Backend guardrail runs after the orchestration path has already chosen a
 * reply.
 * The purpose here is not to make the text "nicer", but to remove claims that
 * cannot be grounded in real tool results before the response leaves the
 * service.
 *
 * <p>
 * Changes vs previous:
 * <ul>
 * <li>validateProductReferences: thêm {@link #BOLD_PRODUCT_PATTERN} để bắt
 * tên sản phẩm in đậm (**...**) — fix false negative khi LLM không dùng dấu
 * ngoặc.</li>
 * <li>isValidName: dùng partial match thay vì exact equals.</li>
 * </ul>
 */
@Component
@Slf4j
public class ResponseGuardrail {

    private static final Pattern PRICE_PATTERN = Pattern.compile("(\\d{1,3}(\\.\\d{3})*|\\d+)\\s*(đ|d|vnd|k|triệu|tr)",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern PROMO_CODE_PATTERN = Pattern.compile("\\b[A-Z0-9_-]{4,}\\b");
    /** Match tên sản phẩm trong dấu ngoặc kép thẳng, kép cong, hoặc ngoặc đơn. */
    private static final Pattern QUOTED_PRODUCT_PATTERN = Pattern.compile(
            "[\"\\u201c\\u201d'\\u2018\\u2019]([^\"\\u201c\\u201d'\\u2018\\u2019]{4,80})[\"\\u201c\\u201d'\\u2018\\u2019]");

    /**
     * Match tên sản phẩm in đậm (**...**) hoặc in nghiêng (*...*) trong markdown.
     * LLM thường dùng **Tên Sản Phẩm** mà không dùng dấu ngoặc → QUOTED_PRODUCT_PATTERN bỏ sót.
     */
    private static final Pattern BOLD_PRODUCT_PATTERN = Pattern.compile("\\*{1,2}([^*]{4,80})\\*{1,2}");

    private static final String FALLBACK_PRICE_TEXT = "giá trên card sản phẩm";
    private static final String SOFT_TOOL_FAILURE_REPLY = "Mình chưa kiểm tra được dữ liệu chính xác lúc này. Bạn thử lại sau ít phút hoặc xem trực tiếp trên card sản phẩm giúp mình nhé.";
    private static final String SOFT_POLICY_REPLY = "Mình chưa kiểm tra được nguồn chính sách chính xác lúc này. Bạn giúp mình hỏi lại sau ít phút hoặc liên hệ CSKH để xác nhận nhanh nhé.";

    private static final List<String> STOCK_CLAIMS = List.of(
            "còn hàng", "có sẵn", "con hang", "co san", "available", "in stock");
    private static final List<String> POLICY_KEYWORDS = List.of(
            "đổi trả", "doi tra", "giao hàng", "giao hang", "chính sách", "chinh sach",
            "bảo hành", "bao hanh", "thanh toán", "thanh toan", "refund", "return", "ship", "policy");

    public String validateAndSanitize(String reply, ToolResultCollector collector) {
        if (reply == null || reply.isBlank()) {
            return reply;
        }

        String sanitized = reply;
        sanitized = enforceToolFailureSafety(sanitized, collector);
        sanitized = validatePrices(sanitized, collector);
        sanitized = validateProductReferences(sanitized, collector);
        sanitized = validatePromotionCodes(sanitized, collector);
        sanitized = validateStockStatus(sanitized, collector);
        sanitized = validatePolicyGrounding(sanitized, collector);
        sanitized = checkOffScope(sanitized);
        return sanitized;
    }

    private String enforceToolFailureSafety(String reply, ToolResultCollector collector) {
        if (collector == null || !collector.hasToolFailure()) {
            return reply;
        }

        if (containsAny(reply, "chưa thể", "không thể", "tạm thời", "thử lại sau", "mình chưa kiểm tra")) {
            return reply;
        }

        collector.addGuardrailViolation("tool_failure_softened");
        return SOFT_TOOL_FAILURE_REPLY;
    }

    private String validatePrices(String reply, ToolResultCollector collector) {
        if (collector == null || (collector.getProducts().isEmpty() && collector.getPromotions().isEmpty())) {
            return reply;
        }

        Matcher matcher = PRICE_PATTERN.matcher(reply);
        StringBuilder result = new StringBuilder();
        int lastEnd = 0;

        while (matcher.find()) {
            String foundPrice = matcher.group();
            boolean existsInData = isPriceValidInContext(foundPrice, collector);

            result.append(reply, lastEnd, matcher.start());
            if (existsInData) {
                result.append(foundPrice);
            } else {
                log.warn("Guardrail blocked hallucinated price: {}", foundPrice);
                collector.addGuardrailViolation("hallucinated_price");
                result.append(FALLBACK_PRICE_TEXT);
            }
            lastEnd = matcher.end();
        }

        result.append(reply.substring(lastEnd));
        return result.toString();
    }

    private boolean isPriceValidInContext(String priceText, ToolResultCollector collector) {
        String numericOnly = priceText.replaceAll("[^\\d]", "");
        if (numericOnly.isBlank()) {
            return true;
        }

        try {
            long priceValue = Long.parseLong(numericOnly);
            if (priceText.toLowerCase(Locale.ROOT).contains("k") && priceValue < 1000) {
                priceValue *= 1000;
            }
            final long target = priceValue;

            boolean inProducts = collector.getProducts().stream()
                    .anyMatch(product -> matchesNumericPrice(product != null ? product.getPrice() : null, target));
            if (inProducts) {
                return true;
            }

            return collector.getPromotions().stream()
                    .anyMatch(promotion -> matchesNumericPrice(promotion != null ? promotion.getMinOrderAmount() : null,
                            target));
        } catch (Exception ex) {
            log.warn("Guardrail skipped unparsable price '{}': {}", priceText, ex.getMessage());
            return true;
        }
    }

    private boolean matchesNumericPrice(String priceText, long target) {
        if (priceText == null || priceText.isBlank()) {
            return false;
        }
        String numeric = priceText.replaceAll("[^\\d]", "");
        if (numeric.isBlank()) {
            return false;
        }
        try {
            return Long.parseLong(numeric) == target;
        } catch (Exception ex) {
            return false;
        }
    }

    /**
     * Kiểm tra tên sản phẩm được đề cập trong reply có tồn tại trong tool results
     * không.
     *
     * <p>
     * Scan 2 pass:
     * <ol>
     * <li>Quoted names: "áo sơ mi Oxford" hoặc 'áo sơ mi Oxford'</li>
     * <li>Bold/italic markdown: **Áo Sơ Mi Oxford** — fix false negative</li>
     * </ol>
     */
    private String validateProductReferences(String reply, ToolResultCollector collector) {
        if (collector == null || collector.getProducts().isEmpty()) {
            return reply;
        }

        Set<String> validNames = new LinkedHashSet<>();
        collector.getProducts().forEach(product -> {
            if (product != null && product.getName() != null && !product.getName().isBlank()) {
                validNames.add(normalize(product.getName()));
            }
        });

        // Pass 1: quoted product names
        String result = replaceInvalidProductRefs(reply, QUOTED_PRODUCT_PATTERN, 1, validNames, collector);

        // Pass 2: bold/italic markdown product names
        result = replaceInvalidProductRefs(result, BOLD_PRODUCT_PATTERN, 1, validNames, collector);

        return result;
    }

    /**
     * Helper: scan reply với pattern, thay tên không hợp lệ bằng "sản phẩm này".
     */
    private String replaceInvalidProductRefs(String reply, Pattern pattern, int captureGroup,
            Set<String> validNames, ToolResultCollector collector) {
        Matcher matcher = pattern.matcher(reply);
        StringBuffer buffer = new StringBuffer();
        boolean changed = false;

        while (matcher.find()) {
            String candidate = matcher.group(captureGroup);
            String normalizedCandidate = normalize(candidate);
            if (looksLikeProductReference(normalizedCandidate) && !isValidName(normalizedCandidate, validNames)) {
                matcher.appendReplacement(buffer, Matcher.quoteReplacement("sản phẩm này"));
                collector.addGuardrailViolation("hallucinated_product_name");
                log.warn("Guardrail blocked hallucinated product reference: '{}'", candidate);
                changed = true;
            } else {
                matcher.appendReplacement(buffer, Matcher.quoteReplacement(matcher.group()));
            }
        }

        if (!changed)
            return reply;
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    /**
     * Partial match: valid nếu tên thật chứa candidate hoặc ngược lại.
     * VD: "Áo Oxford Classic" match khi validNames chứa "oxford classic".
     */
    private boolean isValidName(String normalizedCandidate, Set<String> validNames) {
        if (validNames.contains(normalizedCandidate))
            return true;
        return validNames.stream().anyMatch(v -> v.contains(normalizedCandidate) || normalizedCandidate.contains(v));
    }

    private String validatePromotionCodes(String reply, ToolResultCollector collector) {
        if (collector == null) {
            return reply;
        }

        Set<String> validCodes = new LinkedHashSet<>();
        collector.getPromotions().forEach(promotion -> {
            if (promotion != null && promotion.getCode() != null && !promotion.getCode().isBlank()) {
                validCodes.add(promotion.getCode().trim().toUpperCase(Locale.ROOT));
            }
        });

        Matcher matcher = PROMO_CODE_PATTERN.matcher(reply);
        StringBuffer buffer = new StringBuffer();
        boolean changed = false;

        while (matcher.find()) {
            String code = matcher.group();
            if (!looksLikePromotionCode(code)) {
                continue;
            }
            if (!validCodes.contains(code.toUpperCase(Locale.ROOT))) {
                matcher.appendReplacement(buffer, Matcher.quoteReplacement("mã ưu đãi phù hợp"));
                collector.addGuardrailViolation("hallucinated_promotion_code");
                changed = true;
            }
        }

        if (!changed) {
            return reply;
        }

        matcher.appendTail(buffer);
        return buffer.toString();
    }

    private String validateStockStatus(String reply, ToolResultCollector collector) {
        if (collector == null || collector.getProducts().isEmpty()) {
            return reply;
        }

        boolean allOutOfStock = collector.getProducts().stream()
                .allMatch(product -> product == null || product.getAvailableSizes() == null
                        || product.getAvailableSizes().isEmpty());

        if (allOutOfStock && containsAny(reply, STOCK_CLAIMS)) {
            collector.addGuardrailViolation("invalid_stock_claim");
            return reply.replace("còn hàng", "hiện đang hết hàng")
                    .replace("có sẵn", "tạm thời chưa có sẵn")
                    .replace("con hang", "hien dang het hang")
                    .replace("co san", "tam thoi chua co san");
        }
        return reply;
    }

    private String validatePolicyGrounding(String reply, ToolResultCollector collector) {
        if (collector == null || !collector.getKnowledgeSources().isEmpty()) {
            return reply;
        }
        if (!containsAny(reply, POLICY_KEYWORDS)) {
            return reply;
        }
        collector.addGuardrailViolation("policy_without_source");
        return SOFT_POLICY_REPLY;
    }

    private String checkOffScope(String reply) {
        String[] forbidden = { "Shopee", "Lazada", "Tiki", "chính trị", "tôn giáo" };
        String result = reply;
        for (String word : forbidden) {
            if (result.contains(word)) {
                log.warn("Guardrail masked forbidden word: {}", word);
                result = result.replace(word, "***");
            }
        }
        return result;
    }

    private boolean looksLikeProductReference(String normalizedText) {
        return containsAny(normalizedText,
                "ao", "quan", "vay", "dam",
                "dress", "shirt", "jean", "jacket", "blazer", "skirt");
    }

    private boolean looksLikePromotionCode(String code) {
        if (code == null || code.length() < 4) {
            return false;
        }
        String upper = code.toUpperCase(Locale.ROOT);
        return upper.matches(".*\\d.*")
                || upper.startsWith("SALE")
                || upper.startsWith("PROMO")
                || upper.startsWith("CODE")
                || upper.startsWith("VIP");
    }

    private boolean containsAny(String text, List<String> patterns) {
        if (patterns == null || patterns.isEmpty()) {
            return false;
        }
        return containsAny(text, patterns.toArray(String[]::new));
    }

    private boolean containsAny(String text, String... patterns) {
        String normalizedText = normalize(text);
        for (String pattern : patterns) {
            if (normalizedText.contains(normalize(pattern))) {
                return true;
            }
        }
        return false;
    }

    private String normalize(String text) {
        return text == null ? "" : text.trim().toLowerCase(Locale.ROOT);
    }
}
