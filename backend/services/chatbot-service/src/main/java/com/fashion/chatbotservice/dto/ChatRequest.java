package com.fashion.chatbotservice.dto;

import lombok.Data;

import java.util.List;

@Data
public class ChatRequest {
    private String message;
    private String sessionId;

    /**
     * True when the first turn of a fresh session is being sent. The backend uses
     * this to avoid treating a cold start as if it already had stable context.
     */
    private Boolean coldStart;

    private UserPreferences preferences;
    private SelectedProductContext selectedProductContext;

    @Data
    public static class UserPreferences {
        private String tone;
        private String style;
        private List<String> focus;
        private String budget;
    }

    @Data
    public static class SelectedProductContext {
        private String productId;
        private String productName;
        private String category;
        private String categoryGender;
        private String price;
        private String link;
        private String sourceMessageId;
    }
}
