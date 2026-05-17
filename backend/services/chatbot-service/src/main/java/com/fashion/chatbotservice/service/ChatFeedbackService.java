package com.fashion.chatbotservice.service;

import com.fashion.chatbotservice.dto.ChatFeedbackEventRequest;

public interface ChatFeedbackService {

    void recordEvent(String userId, ChatFeedbackEventRequest request);
}
