package com.fashion.chatbotservice.repository;

import com.fashion.chatbotservice.model.KnowledgeDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface KnowledgeDocumentRepository extends MongoRepository<KnowledgeDocument, String> {
    List<KnowledgeDocument> findBySource(String source);
    void deleteBySource(String source);
}
