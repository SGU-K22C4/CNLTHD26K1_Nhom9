package com.fashion.chatbotservice.service;

import com.fashion.chatbotservice.model.ChatSession;

public interface SizeFitAdvisoryService {

    record SizeFitAdvice(
            String recommendedSize,
            String rationale,
            String followUpPrompt
    ) {
    }

    SizeFitAdvice advise(
            SizeAdvisorService.Measurements measurements,
            SizeAdvisorService.GarmentType garmentType,
            String garmentContext,
            ChatSession.PreferenceProfile profile
    );
}
