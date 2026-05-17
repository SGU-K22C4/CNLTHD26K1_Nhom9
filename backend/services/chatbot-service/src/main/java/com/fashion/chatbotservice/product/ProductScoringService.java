package com.fashion.chatbotservice.product;

import com.fashion.chatbotservice.dto.ChatResponse;
import com.fashion.chatbotservice.model.ChatSession;

import java.util.List;

public interface ProductScoringService {

    ScoreResult score(ChatResponse.ProductSuggestion suggestion,
                      ProductMetadataProfile metadata,
                      ChatSession.PreferenceProfile profile,
                      String search,
                      Long minPrice,
                      Long maxPrice,
                      String color,
                      String size);

    record ScoreResult(double score, List<String> reasons) {
    }
}
