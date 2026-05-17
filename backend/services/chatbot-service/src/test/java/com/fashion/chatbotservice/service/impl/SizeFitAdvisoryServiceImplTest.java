package com.fashion.chatbotservice.service.impl;

import com.fashion.chatbotservice.model.ChatSession;
import com.fashion.chatbotservice.service.SizeAdvisorService;
import com.fashion.chatbotservice.service.SizeFitAdvisoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SizeFitAdvisoryServiceImplTest {

    private SizeFitAdvisoryService service;

    @BeforeEach
    void setUp() {
        service = new SizeFitAdvisoryServiceImpl(new SizeAdvisorServiceImpl());
    }

    @Test
    void shouldSizeUpShirtWhenChestIsFull() {
        SizeAdvisorService.Measurements measurements = new SizeAdvisorService.Measurements(163, 55, 90, 68, 92);

        SizeFitAdvisoryService.SizeFitAdvice advice = service.advise(
                measurements,
                SizeAdvisorService.GarmentType.TOP,
                "ao so mi Zara",
                ChatSession.PreferenceProfile.empty());

        assertEquals("L", advice.recommendedSize());
        assertTrue(advice.rationale().toLowerCase().contains("vai") || advice.rationale().toLowerCase().contains("nguc"));
    }

    @Test
    void shouldSuggestLayeringAllowanceForBlazer() {
        SizeAdvisorService.Measurements measurements = new SizeAdvisorService.Measurements(165, 56, 86, 68, 92);

        SizeFitAdvisoryService.SizeFitAdvice advice = service.advise(
                measurements,
                SizeAdvisorService.GarmentType.TOP,
                "blazer fitted",
                ChatSession.PreferenceProfile.empty());

        assertTrue(advice.recommendedSize().matches("M|L"));
        assertTrue(advice.followUpPrompt().toLowerCase().contains("vai"));
    }
}
