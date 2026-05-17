package com.fashion.chatbotservice.styling;

import com.fashion.chatbotservice.model.ChatSession;
import com.fashion.chatbotservice.product.ProductMetadataProfile;

public interface OccasionAdvisorService {

    OccasionAdvice advise(String search, ChatSession.PreferenceProfile profile, ProductMetadataProfile metadata);
}
