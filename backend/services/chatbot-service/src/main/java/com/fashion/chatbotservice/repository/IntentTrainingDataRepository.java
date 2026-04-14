package com.fashion.chatbotservice.repository;

import com.fashion.chatbotservice.model.IntentTrainingData;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface IntentTrainingDataRepository extends MongoRepository<IntentTrainingData, String> {
    Optional<IntentTrainingData> findByIntentName(String intentName);
}
