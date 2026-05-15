package com.fashion.chatbotservice.agent;

import com.fashion.chatbotservice.dto.ChatResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Layer 3: Backend Guardrail
 * Validates and sanitizes LLM output against real-world data from tools.
 */
@Component
@Slf4j
public class ResponseGuardrail {

    // Regex to find prices like "500.000 đ", "500k", "1.200.000đ"
    private static final Pattern PRICE_PATTERN = Pattern.compile("(\\d{1,3}(\\.\\d{3})*|\\d+)\\s*(đ|VND|k|triệu)", Pattern.CASE_INSENSITIVE);

    /**
     * Validate the LLM reply against collected tool results.
     * returns the sanitized (or original) reply.
     */
    public String validateAndSanitize(String reply, ToolResultCollector collector) {
        if (reply == null || reply.isBlank()) return reply;
        
        String sanitized = reply;

        // 1. Validate Prices
        sanitized = validatePrices(sanitized, collector);

        // 2. Validate Stock Status
        sanitized = validateStockStatus(sanitized, collector);

        // 3. Check for sensitive or off-scope patterns (Basic)
        sanitized = checkOffScope(sanitized);

        return sanitized;
    }

    private String validatePrices(String reply, ToolResultCollector collector) {
        if (collector.getProducts().isEmpty()) return reply;

        Matcher matcher = PRICE_PATTERN.matcher(reply);
        StringBuilder sb = new StringBuilder();
        int lastEnd = 0;

        while (matcher.find()) {
            String foundPrice = matcher.group();
            // Basic check: is this price mentioned in any of our collected products/promos?
            boolean existsInData = isPriceValidInContext(foundPrice, collector);

            sb.append(reply, lastEnd, matcher.start());
            if (existsInData) {
                sb.append(foundPrice);
            } else {
                log.warn("Guardrail: Hallucinated price detected: {}", foundPrice);
                // If it's a hallucination, we append it but could potentially wrap it or replace it.
                // For now, we'll keep it but log. In a stricter mode, we could replace with "[Giá liên hệ]".
                sb.append(foundPrice); 
            }
            lastEnd = matcher.end();
        }
        sb.append(reply.substring(lastEnd));
        return sb.toString();
    }

    private boolean isPriceValidInContext(String priceText, ToolResultCollector collector) {
        String numericOnly = priceText.replaceAll("[^\\d]", "");
        if (numericOnly.isBlank()) return true;

        try {
            long priceVal = Long.parseLong(numericOnly);
            if (priceText.toLowerCase().contains("k") && priceVal < 1000) priceVal *= 1000;
            final long target = priceVal;

            boolean inProducts = collector.getProducts().stream()
                    .anyMatch(p -> {
                        try {
                            String pPrice = p.getPrice().replaceAll("[^\\d]", "");
                            return !pPrice.isBlank() && Long.parseLong(pPrice) == target;
                        } catch (Exception e) {
                            return false;
                        }
                    });

            if (inProducts) return true;

            return collector.getPromotions().stream()
                    .anyMatch(promo -> {
                        try {
                            String minAmt = promo.getMinOrderAmount().replaceAll("[^\\d]", "");
                            return !minAmt.isBlank() && Long.parseLong(minAmt) == target;
                        } catch (Exception e) {
                            return false;
                        }
                    });
        } catch (Exception ex) {
            log.warn("Guardrail: Failed to parse price '{}', skipping validation", priceText);
            return true; // Bỏ qua nếu không parse được, tránh làm sập request
        }
    }

    private String validateStockStatus(String reply, ToolResultCollector collector) {
        // If LLM says "còn hàng" but all products in collector are empty/out-of-stock
        boolean allOutOfStock = !collector.getProducts().isEmpty() && 
                                collector.getProducts().stream().allMatch(p -> p.getAvailableSizes().isEmpty());
        
        if (allOutOfStock && (reply.contains("còn hàng") || reply.contains("có sẵn"))) {
            log.warn("Guardrail: LLM claimed stock exists but tools say otherwise.");
            return reply.replace("còn hàng", "hiện đang hết hàng")
                        .replace("có sẵn", "tạm thời chưa có sẵn");
        }
        return reply;
    }

    private String checkOffScope(String reply) {
        // Basic filter for unwanted mentions (competitors, unrelated topics)
        String[] forbidden = {"Shopee", "Lazada", "Tiki", "chính trị", "tôn giáo"};
        String result = reply;
        for (String word : forbidden) {
            if (result.contains(word)) {
                log.warn("Guardrail: Forbidden word detected: {}", word);
                result = result.replace(word, "***");
            }
        }
        return result;
    }
}
