package com.fashion.chatbotservice.sales;

import com.fashion.chatbotservice.dto.ChatResponse;

import java.util.List;

public interface CompareEngine {

    CompareResult compare(List<ChatResponse.ProductSuggestion> suggestions);

    record CompareResult(
            ChatResponse.ProductSuggestion saferChoice,
            ChatResponse.ProductSuggestion moreStylishChoice,
            ChatResponse.ProductSuggestion easierToStyleChoice,
            ChatResponse.ProductSuggestion betterValueChoice,
            String saferReason,
            String stylishReason,
            String valueReason,
            String easyStyleReason,
            String finalRecommendation) {
    }
}
