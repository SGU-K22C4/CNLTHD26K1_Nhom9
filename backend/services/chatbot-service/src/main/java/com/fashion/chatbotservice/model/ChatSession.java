package com.fashion.chatbotservice.model;

import com.fashion.chatbotservice.conversation.SalesStage;
import com.fashion.chatbotservice.conversation.StylingSlots;
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

        /** Product suggestions attached to bot messages for session restore. */
        @Builder.Default
        private List<ProductSuggestionSnapshot> suggestions = new ArrayList<>();

        /** Promotion suggestions attached to bot messages. */
        @Builder.Default
        private List<PromotionSuggestionSnapshot> promotions = new ArrayList<>();
    }

    /** Lightweight snapshot of a product suggestion stored in MongoDB. */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductSuggestionSnapshot {
        private String productId;
        private String name;
        private String category;
        private String categoryGender;
        private String imageUrl;
        private String link;
        private String price;
        private List<String> availableSizes;
        private List<String> availableColors;
    }

    /** Lightweight snapshot of a promotion suggestion. */
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

    /**
     * Stores the last product card the user explicitly clicked so follow-up
     * questions can bind to a concrete item instead of guessing from raw text.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SelectedProductContextSnapshot {
        private String productId;
        private String productName;
        private String category;
        private String categoryGender;
        private String price;
        private String link;
        private String sourceMessageId;
        private Instant selectedAt;
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

        @Builder.Default
        private Set<String> preferredOccasions = new LinkedHashSet<>();

        private String fitPreference;
        private String customerPersona;
        private String priceComfortZone;
        private String targetGender;

        /** Last body measurements captured in conversation for later size turns. */
        private Integer lastHeightCm;
        private Integer lastWeightKg;
        private Integer lastChestCm;
        private Integer lastWaistCm;
        private Integer lastHipCm;

        /** Product types clarified during cold start and reused in later turns. */
        @Builder.Default
        private Set<String> clarifiedProductTypes = new LinkedHashSet<>();

        /** Last product category the user was clearly querying. */
        private String lastProductCategoryQueried;
        private Instant lastProductQueryTime;

        /** Lightweight flow state to keep multi-turn sales conversations stable. */
        private String conversationFlow;

        /** Indicates what the bot is currently waiting for inside the active flow. */
        private String pendingQuestionType;

        /** Stores the temporary gender target being confirmed with the user. */
        private String pendingTargetGender;

        /** Timestamp used to expire stale conversation state. */
        private Instant conversationStateUpdatedAt;

        private SalesStage salesStage;
        private Instant stageEntryAt;
        private String lastAskedSlot;

        @Builder.Default
        private Set<String> askedSlots = new LinkedHashSet<>();

        private Double slotConfidence;
        private StylingSlots stylingSlots;

        private SelectedProductContextSnapshot selectedProductContext;

        public static PreferenceProfile empty() {
            return PreferenceProfile.builder().build();
        }
    }

    public enum Sender {
        USER,
        BOT
    }
}
