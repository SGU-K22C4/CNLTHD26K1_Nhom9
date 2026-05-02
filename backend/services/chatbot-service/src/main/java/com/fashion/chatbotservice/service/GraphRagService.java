package com.fashion.chatbotservice.service;

import com.fashion.chatbotservice.model.KnowledgeDocument;

import java.util.List;

/**
 * GraphRAG abstraction for building and querying the knowledge graph.
 */
public interface GraphRagService {

    record GraphHit(String chunkId, double score) {}

    boolean isEnabled();

    void rebuildGraph(List<KnowledgeDocument> documents);

    List<GraphHit> retrieve(String query, int topK);
}
