package com.fashion.chatbotservice.service;

import com.fashion.chatbotservice.dto.ChatResponse;
import com.fashion.chatbotservice.model.ChatSession;

import java.util.List;

public interface ProductRecommendationService {

    List<ChatResponse.ProductSuggestion> rankSuggestions(
            List<ChatResponse.ProductSuggestion> suggestions,
            ChatSession.PreferenceProfile profile,
            String search,
            Long minPrice,
            Long maxPrice,
            String color,
            String size);

    List<ChatResponse.ProductSuggestion> diversifySuggestionsByCategory(
            List<ChatResponse.ProductSuggestion> suggestions,
            int maxResults);
}
