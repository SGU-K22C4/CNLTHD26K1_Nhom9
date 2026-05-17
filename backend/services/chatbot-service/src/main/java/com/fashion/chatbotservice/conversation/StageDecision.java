package com.fashion.chatbotservice.conversation;

import lombok.Builder;

@Builder
public record StageDecision(
        SalesStage stage,
        RecommendationReadiness readiness,
        ClarifyingQuestion clarifyingQuestion,
        boolean shouldAskClarifyingQuestion,
        boolean shouldRecommend) {
}
