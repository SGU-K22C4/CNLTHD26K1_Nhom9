package com.fashion.chatbotservice.styling;

import com.fashion.chatbotservice.model.ChatSession;
import com.fashion.chatbotservice.product.ProductMetadataProfile;

public interface BodyShapeAdvisorService {

    BodyShapeAdvice advise(ChatSession.PreferenceProfile profile, ProductMetadataProfile metadata);
}
