package com.fashion.chatbotservice.agent;

import com.fashion.chatbotservice.dto.ChatResponse;

import java.util.ArrayList;
import java.util.List;

/**
 * Thu thập kết quả có cấu trúc từ các tool calls trong quá trình agent suy luận.
 * Mỗi request tạo 1 instance mới (request-scoped).
 *
 * Tool trả về text cho LLM (để tổng hợp câu trả lời),
 * đồng thời lưu structured data vào collector (để frontend render).
 */
public class ToolResultCollector {

    private final List<ChatResponse.ProductSuggestion> products = new ArrayList<>();
    private final List<ChatResponse.PromotionSuggestion> promotions = new ArrayList<>();
    private final List<String> missingFields = new ArrayList<>();
    private final List<KnowledgeSource> knowledgeSources = new ArrayList<>();
    private String sizeRecommendation;

    public void addProducts(List<ChatResponse.ProductSuggestion> items) {
        if (items != null) products.addAll(items);
    }

    public void addPromotions(List<ChatResponse.PromotionSuggestion> items) {
        if (items != null) promotions.addAll(items);
    }

    public void addMissingFields(List<String> fields) {
        if (fields != null) missingFields.addAll(fields);
    }

    public void addKnowledgeSource(String title, String source, double score) {
        knowledgeSources.add(new KnowledgeSource(title, source, score));
    }

    public void setSizeRecommendation(String size) {
        this.sizeRecommendation = size;
    }

    public List<ChatResponse.ProductSuggestion> getProducts() { return products; }
    public List<ChatResponse.PromotionSuggestion> getPromotions() { return promotions; }
    public List<String> getMissingFields() { return missingFields; }
    public List<KnowledgeSource> getKnowledgeSources() { return knowledgeSources; }
    public String getSizeRecommendation() { return sizeRecommendation; }

    public record KnowledgeSource(String title, String source, double score) {}
}
