package com.fashion.chatbotservice.product;

import com.fashion.chatbotservice.dto.ChatResponse;

public interface ProductMetadataEnrichmentService {

    ProductMetadataProfile enrich(ChatResponse.ProductSuggestion suggestion);
}
