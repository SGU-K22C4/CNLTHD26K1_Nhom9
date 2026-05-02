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
 * Node in lightweight knowledge graph for GraphRAG retrieval.
 */
@Document(collection = "knowledge_graph_nodes")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeGraphNode {

    @Id
    private String id;

    /**
     * Stable business id, for example: chunk:<knowledgeDocId>, topic:<topic>, keyword:<token>.
     */
    @Indexed(unique = true)
    private String nodeId;

    /** Node type: CHUNK, TOPIC, KEYWORD. */
    @Indexed
    private String nodeType;

    /** Display label used for debugging and observability. */
    private String displayName;

    /** Normalized label for exact lookup. */
    @Indexed
    private String normalizedName;

    private Instant updatedAt;
}
