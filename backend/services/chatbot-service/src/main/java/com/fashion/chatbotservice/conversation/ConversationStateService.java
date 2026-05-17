package com.fashion.chatbotservice.conversation;

import com.fashion.chatbotservice.model.ChatSession;

public interface ConversationStateService {

    void refreshState(ChatSession.PreferenceProfile profile, String message);

    StageDecision evaluateStageDecision(ChatSession.PreferenceProfile profile, String message);

    boolean shouldAskClarifyingQuestion(ChatSession.PreferenceProfile profile, String message);

    String pickNextQuestionSlot(ChatSession.PreferenceProfile profile);
}
