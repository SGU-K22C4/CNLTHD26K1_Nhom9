package com.fashion.chatbotservice.repository;

import com.fashion.chatbotservice.model.KnowledgeGraphNode;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface KnowledgeGraphNodeRepository extends MongoRepository<KnowledgeGraphNode, String> {

    Optional<KnowledgeGraphNode> findByNodeId(String nodeId);

    List<KnowledgeGraphNode> findByNodeTypeAndNormalizedNameIn(String nodeType, Collection<String> normalizedNames);
}
