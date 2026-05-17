package com.fashion.chatbotservice.repository;

import com.fashion.chatbotservice.model.KnowledgeGraphEdge;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Collection;
import java.util.List;

public interface KnowledgeGraphEdgeRepository extends MongoRepository<KnowledgeGraphEdge, String> {

    List<KnowledgeGraphEdge> findByFromNodeIdIn(Collection<String> fromNodeIds);
}
