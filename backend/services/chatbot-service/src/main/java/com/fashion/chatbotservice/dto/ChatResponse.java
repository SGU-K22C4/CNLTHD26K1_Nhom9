package com.fashion.chatbotservice.dto;

import com.fashion.chatbotservice.model.ChatSession;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
public class ChatResponse {
    private String sessionId;
    private String intent;
    private double confidence;
    private String reply;

    @Builder.Default
    private List<String> missingFields = new ArrayList<>();

    @Builder.Default
    private List<ProductSuggestion> suggestions = new ArrayList<>();

    @Builder.Default
    private List<PromotionSuggestion> promotions = new ArrayList<>();

    private ChatSession.PreferenceProfile profile;
    private Instant createdAt;

    @Data
    @Builder
    public static class ProductSuggestion {
        private String productId;
        private String name;
        private String category;
        private String imageUrl;
        private String link;
        private String price;
        private List<String> availableSizes;
        private List<String> availableColors;
        private String reason;
    }

    @Data
    @Builder
    public static class PromotionSuggestion {
        private String code;
        private String discountType;
        private String discountValue;
        private String minOrderAmount;
        private String endDate;
        private String note;
    }
}
