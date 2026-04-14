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

        public static PreferenceProfile empty() {
            return PreferenceProfile.builder().build();
        }
    }

    public enum Sender {
        USER,
        BOT
    }
}
