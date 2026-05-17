package com.fashion.chatbotservice.agent;

import com.fashion.chatbotservice.dto.ChatResponse;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Thu thap ket qua co cau truc tu tool calls trong qua trinh agent suy luan.
 * Phase 1 them dedupe de frontend khong bi lap product/promo hoac missing field.
 */
public class ToolResultCollector {

    private final List<ChatResponse.ProductSuggestion> products = new ArrayList<>();
    private final List<ChatResponse.PromotionSuggestion> promotions = new ArrayList<>();
    private final List<String> missingFields = new ArrayList<>();
    private final List<KnowledgeSource> knowledgeSources = new ArrayList<>();
    private final List<String> guardrailViolations = new ArrayList<>();
    private final Set<String> productKeys = new LinkedHashSet<>();
    private final Set<String> promotionKeys = new LinkedHashSet<>();
    private final Set<String> missingFieldKeys = new LinkedHashSet<>();
    private final Set<String> knowledgeKeys = new LinkedHashSet<>();
    private final Set<String> guardrailKeys = new LinkedHashSet<>();
    private String sizeRecommendation;
    private boolean toolFailure;

    public void addProducts(List<ChatResponse.ProductSuggestion> items) {
        if (items == null) return;
        for (ChatResponse.ProductSuggestion item : items) {
            if (item == null) continue;
            if (productKeys.add(productKey(item))) {
                products.add(item);
            }
        }
    }

    public void addPromotions(List<ChatResponse.PromotionSuggestion> items) {
        if (items == null) return;
        for (ChatResponse.PromotionSuggestion item : items) {
            if (item == null) continue;
            if (promotionKeys.add(promotionKey(item))) {
                promotions.add(item);
            }
        }
    }

    public void addMissingFields(List<String> fields) {
        if (fields == null) return;
        for (String field : fields) {
            if (field == null || field.isBlank()) continue;
            String normalized = field.trim().toLowerCase(Locale.ROOT);
            if (missingFieldKeys.add(normalized)) {
                missingFields.add(field.trim());
            }
        }
    }

    public void addKnowledgeSource(String title, String source, double score) {
        String key = (title == null ? "" : title.trim().toLowerCase(Locale.ROOT))
                + "|"
                + (source == null ? "" : source.trim().toLowerCase(Locale.ROOT));
        if (knowledgeKeys.add(key)) {
            knowledgeSources.add(new KnowledgeSource(title, source, score));
        }
    }

    public void setSizeRecommendation(String size) {
        this.sizeRecommendation = size;
    }

    public void addGuardrailViolation(String code) {
        if (code == null || code.isBlank()) return;
        String normalized = code.trim().toLowerCase(Locale.ROOT);
        if (guardrailKeys.add(normalized)) {
            guardrailViolations.add(code.trim());
        }
    }

    public void markToolFailure() {
        this.toolFailure = true;
    }

    public List<ChatResponse.ProductSuggestion> getProducts() { return products; }
    public List<ChatResponse.PromotionSuggestion> getPromotions() { return promotions; }
    public List<String> getMissingFields() { return missingFields; }
    public List<KnowledgeSource> getKnowledgeSources() { return knowledgeSources; }
    public List<String> getGuardrailViolations() { return guardrailViolations; }
    public String getSizeRecommendation() { return sizeRecommendation; }
    public boolean hasToolFailure() { return toolFailure; }

    private String productKey(ChatResponse.ProductSuggestion item) {
        if (item.getProductId() != null && !item.getProductId().isBlank()) {
            return item.getProductId().trim();
        }
        return ((item.getName() == null ? "" : item.getName().trim())
                + "|"
                + (item.getPrice() == null ? "" : item.getPrice().trim()))
                .toLowerCase(Locale.ROOT);
    }

    private String promotionKey(ChatResponse.PromotionSuggestion item) {
        if (item.getCode() != null && !item.getCode().isBlank()) {
            return item.getCode().trim().toLowerCase(Locale.ROOT);
        }
        return ((item.getDiscountType() == null ? "" : item.getDiscountType().trim())
                + "|"
                + (item.getDiscountValue() == null ? "" : item.getDiscountValue().trim())
                + "|"
                + (item.getEndDate() == null ? "" : item.getEndDate().trim()))
                .toLowerCase(Locale.ROOT);
    }

    public record KnowledgeSource(String title, String source, double score) {}
}
