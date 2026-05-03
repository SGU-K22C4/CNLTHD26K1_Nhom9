package com.fashion.chatbotservice.repository;

import com.fashion.chatbotservice.model.ChatSession;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface ChatSessionRepository extends MongoRepository<ChatSession, String> {
    Optional<ChatSession> findBySessionId(String sessionId);

    List<ChatSession> findTop5ByUserIdOrderByStartedAtDesc(String userId);
}
