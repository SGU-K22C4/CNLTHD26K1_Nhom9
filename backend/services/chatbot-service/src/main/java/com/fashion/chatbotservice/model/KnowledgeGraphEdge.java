package com.fashion.chatbotservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * Directed weighted edge in the knowledge graph.
 */
@Document(collection = "knowledge_graph_edges")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeGraphEdge {

    @Id
    private String id;

    @Indexed
    private String fromNodeId;

    @Indexed
    private String toNodeId;

    @Indexed
    private String relationType;

    /** Weight in range [0, 1]. */
    private double weight;

    private Instant updatedAt;
}
