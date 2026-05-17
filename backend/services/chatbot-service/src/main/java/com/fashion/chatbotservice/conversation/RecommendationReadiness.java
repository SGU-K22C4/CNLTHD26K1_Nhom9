package com.fashion.chatbotservice.conversation;

import lombok.Builder;

import java.util.List;

@Builder
public record RecommendationReadiness(
        boolean ready,
        int filledCoreSlots,
        List<String> missingPrioritySlots) {
}
