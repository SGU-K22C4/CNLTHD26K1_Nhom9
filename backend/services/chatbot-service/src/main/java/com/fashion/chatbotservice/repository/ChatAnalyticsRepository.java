package com.fashion.chatbotservice.repository;

import com.fashion.chatbotservice.model.ChatAnalyticsDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ChatAnalyticsRepository extends MongoRepository<ChatAnalyticsDocument, String> {
}
