package com.fashion.chatbotservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Document(collection = "chat_sessions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatSession {

    @Id
    private String id;

    @Indexed(unique = true)
    private String sessionId;

    @Indexed
    private String userId;

    private Instant startedAt;
    private Instant endedAt;

    @Builder.Default
    private List<ChatMessage> messages = new ArrayList<>();

    @Builder.Default
    private PreferenceProfile preferenceProfile = PreferenceProfile.empty();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChatMessage {
        private String messageId;
        private Sender sender;
        private String content;
        private IntentMeta intent;
        private Instant createdAt;

        /** Product suggestions attached to BOT messages (persisted for session restore) */
        @Builder.Default
        private List<ProductSuggestionSnapshot> suggestions = new ArrayList<>();

        /** Promotion suggestions attached to BOT messages */
        @Builder.Default
        private List<PromotionSuggestionSnapshot> promotions = new ArrayList<>();
    }

    /** Lightweight snapshot of a product suggestion, stored in MongoDB */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductSuggestionSnapshot {
        private String productId;
        private String name;
        private String category;
        private String imageUrl;
        private String link;
        private String price;
        private List<String> availableSizes;
        private List<String> availableColors;
    }

    /** Lightweight snapshot of a promotion suggestion */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PromotionSuggestionSnapshot {
        private String code;
        private String discountType;
        private String discountValue;
        private String minOrderAmount;
        private String endDate;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IntentMeta {
        private String intentName;
        private double confidence;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PreferenceProfile {
        private String preferredTone;
        private String style;

        @Builder.Default
        private Set<String> focusTags = new LinkedHashSet<>();

        private String budget;

        @Builder.Default
        private Set<String> preferredSizes = new LinkedHashSet<>();

        @Builder.Default
        private Set<String> preferredColors = new LinkedHashSet<>();

        @Builder.Default
        private Set<String> preferredCategories = new LinkedHashSet<>();

        /** Lưu số đo cơ thể gần nhất để dùng cho câu tiếp theo (context memory) */
        private Integer lastHeightCm;
        private Integer lastWeightKg;
        private Integer lastChestCm;
        private Integer lastWaistCm;
        private Integer lastHipCm;

        public static PreferenceProfile empty() {
            return PreferenceProfile.builder().build();
        }
    }

    public enum Sender {
        USER,
        BOT
    }
}
