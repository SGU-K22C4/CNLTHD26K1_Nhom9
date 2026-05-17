package com.fashion.chatbotservice.sales;

import com.fashion.chatbotservice.model.ChatSession;

public interface ClosingEngine {

    String buildSoftClose(ChatSession.PreferenceProfile profile, int suggestionCount);

    String buildDecisionClose(ChatSession.PreferenceProfile profile, CompareEngine.CompareResult compareResult);
}
