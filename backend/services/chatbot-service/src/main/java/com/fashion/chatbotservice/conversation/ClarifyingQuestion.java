package com.fashion.chatbotservice.conversation;

import lombok.Builder;

@Builder
public record ClarifyingQuestion(
        String slotName,
        String question,
        String reason,
        int priority) {
}
