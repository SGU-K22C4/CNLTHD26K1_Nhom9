package com.fashion.chatbotservice.conversation.impl;

import com.fashion.chatbotservice.model.ChatSession;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SlotFillingServiceImplTest {

    private final SlotFillingServiceImpl service = new SlotFillingServiceImpl();

    @Test
    void shouldExtractCoreSlotsFromConsultativeMessage() {
        ChatSession.PreferenceProfile profile = ChatSession.PreferenceProfile.empty();
        profile.setBudget("1tr-1tr6");

        service.mergeSlots(profile, "Mình cần áo sơ mi đi làm, vibe tối giản, cao 163 nặng 55");

        assertEquals("office", profile.getStylingSlots().getOccasion());
        assertEquals("ao so mi", profile.getStylingSlots().getProductType());
        assertEquals("minimal", profile.getStylingSlots().getStyleVibe());
        assertEquals("1tr-1tr6", profile.getStylingSlots().getBudget());
        assertTrue(profile.getSlotConfidence() >= 0.6d);
        assertTrue(service.evaluateReadiness(profile).ready());
        assertFalse(service.evaluateReadiness(profile).missingPrioritySlots().contains("occasion"));
    }
}
