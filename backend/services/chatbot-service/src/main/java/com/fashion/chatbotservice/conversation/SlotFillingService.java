package com.fashion.chatbotservice.conversation;

import com.fashion.chatbotservice.model.ChatSession;

import java.util.List;

public interface SlotFillingService {

    void mergeSlots(ChatSession.PreferenceProfile profile, String message);

    List<String> findMissingPrioritySlots(ChatSession.PreferenceProfile profile);

    double estimateConfidence(ChatSession.PreferenceProfile profile);

    RecommendationReadiness evaluateReadiness(ChatSession.PreferenceProfile profile);
}
