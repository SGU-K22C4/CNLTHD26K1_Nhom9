package com.fashion.chatbotservice.dto;

import lombok.Data;

import java.util.List;

@Data
public class ChatRequest {
    private String message;
    private String sessionId;
    private UserPreferences preferences;

    @Data
    public static class UserPreferences {
        private String tone;
        private String style;
        private List<String> focus;
        private String budget;
    }
}
