package com.fashion.chatbotservice.conversation.impl;

import com.fashion.chatbotservice.conversation.SalesStage;
import com.fashion.chatbotservice.conversation.StageDecision;
import com.fashion.chatbotservice.model.ChatSession;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConversationStateServiceImplTest {

    private final ConversationStateServiceImpl service =
            new ConversationStateServiceImpl(new SlotFillingServiceImpl());

    @Test
    void shouldMoveToRecommendingWhenEnoughSlotsArePresent() {
        ChatSession.PreferenceProfile profile = ChatSession.PreferenceProfile.empty();

        service.refreshState(profile, "Minh can ao so mi di lam vibe lich su");

        assertEquals(SalesStage.RECOMMENDING, profile.getSalesStage());
    }

    @Test
    void shouldAskClarifyingQuestionForBroadConsultativeTurn() {
        ChatSession.PreferenceProfile profile = ChatSession.PreferenceProfile.empty();
        service.refreshState(profile, "Goi y do cho minh");

        assertTrue(service.shouldAskClarifyingQuestion(profile, "Goi y do cho minh"));
        assertEquals("occasion", service.pickNextQuestionSlot(profile));
    }

    @Test
    void shouldReturnRecommendationDecisionWhenEnoughSlotsPresent() {
        ChatSession.PreferenceProfile profile = ChatSession.PreferenceProfile.empty();
        service.refreshState(profile, "Minh can ao so mi di lam vibe lich su");

        StageDecision decision = service.evaluateStageDecision(profile, "Minh can ao so mi di lam vibe lich su");

        assertEquals(SalesStage.RECOMMENDING, decision.stage());
        assertTrue(decision.shouldRecommend());
        assertTrue(decision.readiness().ready());
    }

    @Test
    void shouldAskMeasurementsWhenUserNeedsFitAdviceWithoutBodyContext() {
        ChatSession.PreferenceProfile profile = ChatSession.PreferenceProfile.empty();
        service.refreshState(profile, "Minh muon tu van fit cho ao so mi");

        StageDecision decision = service.evaluateStageDecision(profile, "Minh muon tu van fit cho ao so mi");

        assertTrue(decision.shouldAskClarifyingQuestion());
        assertEquals("measurements", decision.clarifyingQuestion().slotName());
    }
}
