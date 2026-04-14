package com.fashion.chatbotservice.dto;

import com.fashion.chatbotservice.model.ChatSession;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class SessionResponse {
    private String sessionId;
    private String userId;
    private List<ChatSession.ChatMessage> messages;
    private ChatSession.PreferenceProfile profile;
}
