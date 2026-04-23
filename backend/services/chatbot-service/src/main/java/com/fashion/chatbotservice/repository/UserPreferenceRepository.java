package com.fashion.chatbotservice.repository;

import com.fashion.chatbotservice.model.UserPreferenceDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * MongoDB repository for persistent user preference profiles.
 */
@Repository
public interface UserPreferenceRepository extends MongoRepository<UserPreferenceDocument, String> {

    Optional<UserPreferenceDocument> findByUserId(String userId);
}
