package com.fashion.chatbotservice.service.impl;

import com.fashion.chatbotservice.model.KnowledgeDocument;
import com.fashion.chatbotservice.model.KnowledgeGraphEdge;
import com.fashion.chatbotservice.model.KnowledgeGraphNode;
import com.fashion.chatbotservice.repository.KnowledgeGraphEdgeRepository;
import com.fashion.chatbotservice.repository.KnowledgeGraphNodeRepository;
import com.fashion.chatbotservice.service.GraphRagService;
import com.fashion.chatbotservice.util.VietnameseNormalizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Lightweight GraphRAG implementation built on top of MongoDB collections.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GraphRagServiceImpl implements GraphRagService {

    private static final String NODE_CHUNK = "CHUNK";
    private static final String NODE_TOPIC = "TOPIC";
    private static final String NODE_KEYWORD = "KEYWORD";

    private static final String REL_TOPIC_HAS_CHUNK = "TOPIC_HAS_CHUNK";
    private static final String REL_KEYWORD_MENTIONS_CHUNK = "KEYWORD_MENTIONS_CHUNK";
    private static final String REL_ADJACENT = "ADJACENT";
    private static final String REL_RELATED = "RELATED";

    private static final String CHUNK_NODE_PREFIX = "chunk:";
    private static final String TOPIC_NODE_PREFIX = "topic:";
    private static final String KEYWORD_NODE_PREFIX = "keyword:";

    private static final Set<String> STOP_WORDS = Set.of(
            "la", "va", "voi", "cho", "cua", "duoc", "nhung", "khi", "thi", "nay", "kia",
            "minh", "ban", "toi", "anh", "chi", "em", "shop", "fashion", "store", "co", "khong",
            "mot", "hai", "ba", "bon", "nam", "neu", "de", "den", "tu", "tren", "duoi", "theo",
            "hay", "nhe", "nha", "a", "ah", "oi", "giup", "ho", "xin", "cam", "on"
    );

    private final KnowledgeGraphNodeRepository nodeRepository;
    private final KnowledgeGraphEdgeRepository edgeRepository;

    @Value("${chatbot.knowledge.graphrag.enabled:true}")
    private boolean enabled;

    @Value("${chatbot.knowledge.graphrag.max-keywords-per-chunk:8}")
    private int maxKeywordsPerChunk;

    @Value("${chatbot.knowledge.graphrag.max-hits:12}")
    private int maxHits;

    @Value("${chatbot.knowledge.graphrag.related-threshold:0.25}")
    private double relatedThreshold;

    @Value("${chatbot.knowledge.graphrag.adjacent-boost:0.25}")
    private double adjacentBoost;

    @Value("${chatbot.knowledge.graphrag.related-boost:0.35}")
    private double relatedBoost;

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void rebuildGraph(List<KnowledgeDocument> documents) {
        if (!enabled) {
            log.info("GraphRAG disabled, skipping graph rebuild");
            return;
        }

        if (documents == null || documents.isEmpty()) {
            nodeRepository.deleteAll();
            edgeRepository.deleteAll();
            log.info("GraphRAG graph reset because knowledge document set is empty");
            return;
        }

        Instant now = Instant.now();
        Map<String, KnowledgeGraphNode> nodeByNodeId = new LinkedHashMap<>();
        List<KnowledgeGraphEdge> edges = new ArrayList<>();
        Set<String> edgeKeys = new HashSet<>();

        Map<String, List<KnowledgeDocument>> docsBySource = new LinkedHashMap<>();
        for (KnowledgeDocument doc : documents) {
            if (doc == null || isBlank(doc.getId())) continue;
            docsBySource
                    .computeIfAbsent(safeSource(doc.getSource()), ignored -> new ArrayList<>())
                    .add(doc);
        }

        Map<String, Set<String>> keywordsByChunkNode = new HashMap<>();
        Map<String, List<String>> chunkNodesByTopic = new LinkedHashMap<>();

        for (List<KnowledgeDocument> sourceDocs : docsBySource.values()) {
            for (KnowledgeDocument doc : sourceDocs) {
                String chunkNodeId = chunkNodeId(doc.getId());
                String chunkLabel = chunkLabel(doc);
                nodeByNodeId.put(chunkNodeId, buildNode(chunkNodeId, NODE_CHUNK, chunkLabel, normalize(chunkLabel), now));

                String normalizedTopic = normalizeTopic(doc.getTopic());
                String topicNodeId = topicNodeId(normalizedTopic);
                nodeByNodeId.put(topicNodeId, buildNode(topicNodeId, NODE_TOPIC, normalizedTopic, normalizedTopic, now));
                addEdge(edges, edgeKeys, topicNodeId, chunkNodeId, REL_TOPIC_HAS_CHUNK, 1.0, now);

                Map<String, Double> keywordWeights = extractKeywordWeights(
                        doc.getTitle() + " " + doc.getTopic() + " " + doc.getContent(),
                        Math.max(2, maxKeywordsPerChunk)
                );

                Set<String> keywordSet = new LinkedHashSet<>(keywordWeights.keySet());
                keywordsByChunkNode.put(chunkNodeId, keywordSet);
                chunkNodesByTopic.computeIfAbsent(normalizedTopic, ignored -> new ArrayList<>()).add(chunkNodeId);

                for (Map.Entry<String, Double> entry : keywordWeights.entrySet()) {
                    String keyword = entry.getKey();
                    String keywordNodeId = keywordNodeId(keyword);
                    nodeByNodeId.put(keywordNodeId, buildNode(keywordNodeId, NODE_KEYWORD, keyword, keyword, now));
                    addEdge(edges, edgeKeys, keywordNodeId, chunkNodeId,
                            REL_KEYWORD_MENTIONS_CHUNK, entry.getValue(), now);
                }
            }
        }

        // Keep local context around the current chunk.
        for (List<KnowledgeDocument> sourceDocs : docsBySource.values()) {
            for (int i = 0; i < sourceDocs.size() - 1; i++) {
                String currentChunkNode = chunkNodeId(sourceDocs.get(i).getId());
                String nextChunkNode = chunkNodeId(sourceDocs.get(i + 1).getId());

                addEdge(edges, edgeKeys, currentChunkNode, nextChunkNode, REL_ADJACENT, 0.65, now);
                addEdge(edges, edgeKeys, nextChunkNode, currentChunkNode, REL_ADJACENT, 0.65, now);
            }
        }

        // Link semantically related chunks in the same topic.
        for (List<String> topicChunkNodes : chunkNodesByTopic.values()) {
            for (int i = 0; i < topicChunkNodes.size(); i++) {
                for (int j = i + 1; j < topicChunkNodes.size(); j++) {
                    String leftChunkNode = topicChunkNodes.get(i);
                    String rightChunkNode = topicChunkNodes.get(j);

                    Set<String> leftKeywords = keywordsByChunkNode.getOrDefault(leftChunkNode, Collections.emptySet());
                    Set<String> rightKeywords = keywordsByChunkNode.getOrDefault(rightChunkNode, Collections.emptySet());
                    double overlap = jaccard(leftKeywords, rightKeywords);

                    if (overlap < relatedThreshold) continue;

                    double weight = Math.min(1.0, 0.45 + overlap * 0.55);
                    addEdge(edges, edgeKeys, leftChunkNode, rightChunkNode, REL_RELATED, weight, now);
                    addEdge(edges, edgeKeys, rightChunkNode, leftChunkNode, REL_RELATED, weight, now);
                }
            }
        }

        nodeRepository.deleteAll();
        edgeRepository.deleteAll();
        nodeRepository.saveAll(nodeByNodeId.values());
        edgeRepository.saveAll(edges);

        log.info("GraphRAG rebuilt: {} nodes, {} edges, {} chunks",
                nodeByNodeId.size(), edges.size(), docsBySource.values().stream().mapToInt(List::size).sum());
    }

    @Override
    public List<GraphHit> retrieve(String query, int topK) {
        if (!enabled || isBlank(query)) return List.of();

        Set<String> queryTerms = extractQueryTerms(query);
        if (queryTerms.isEmpty()) return List.of();

        Set<String> topicTerms = buildTopicTerms(query);

        Map<String, Double> seedNodeScores = new HashMap<>();
        addSeedScores(seedNodeScores, nodeRepository.findByNodeTypeAndNormalizedNameIn(NODE_KEYWORD, queryTerms), 1.0);
        addSeedScores(seedNodeScores, nodeRepository.findByNodeTypeAndNormalizedNameIn(NODE_TOPIC, topicTerms), 1.15);

        if (seedNodeScores.isEmpty()) return List.of();

        Map<String, Double> chunkScores = new HashMap<>();
        List<KnowledgeGraphEdge> firstHopEdges = edgeRepository.findByFromNodeIdIn(seedNodeScores.keySet());

        for (KnowledgeGraphEdge edge : firstHopEdges) {
            if (edge == null || isBlank(edge.getFromNodeId()) || isBlank(edge.getToNodeId())) continue;
            if (!isChunkNode(edge.getToNodeId())) continue;

            double seedScore = seedNodeScores.getOrDefault(edge.getFromNodeId(), 0.0);
            if (seedScore <= 0) continue;

            String chunkId = toChunkId(edge.getToNodeId());
            chunkScores.merge(chunkId, seedScore * edge.getWeight(), Double::sum);
        }

        if (chunkScores.isEmpty()) return List.of();

        // Expand one hop from already matched chunks.
        Map<String, Double> baseChunkScores = new HashMap<>(chunkScores);
        List<String> sourceChunkNodeIds = baseChunkScores.keySet().stream().map(this::chunkNodeId).toList();
        List<KnowledgeGraphEdge> secondHopEdges = edgeRepository.findByFromNodeIdIn(sourceChunkNodeIds);

        for (KnowledgeGraphEdge edge : secondHopEdges) {
            if (edge == null || !isChunkNode(edge.getFromNodeId()) || !isChunkNode(edge.getToNodeId())) continue;

            String fromChunkId = toChunkId(edge.getFromNodeId());
            String toChunkId = toChunkId(edge.getToNodeId());
            double fromScore = baseChunkScores.getOrDefault(fromChunkId, 0.0);
            if (fromScore <= 0 || isBlank(edge.getRelationType())) continue;

            double boost = switch (edge.getRelationType()) {
                case REL_RELATED -> relatedBoost;
                case REL_ADJACENT -> adjacentBoost;
                default -> 0.0;
            };
            if (boost <= 0) continue;

            chunkScores.merge(toChunkId, fromScore * edge.getWeight() * boost, Double::sum);
        }

        double maxScore = chunkScores.values().stream().max(Double::compareTo).orElse(0.0);
        if (maxScore <= 0) return List.of();

        int effectiveTopK = Math.max(1, topK > 0 ? topK : maxHits);

        return chunkScores.entrySet().stream()
                .map(entry -> new GraphHit(entry.getKey(), entry.getValue() / maxScore))
                .sorted((a, b) -> Double.compare(b.score(), a.score()))
                .limit(effectiveTopK)
                .toList();
    }

    private void addSeedScores(Map<String, Double> seedScores, List<KnowledgeGraphNode> nodes, double score) {
        if (nodes == null || nodes.isEmpty()) return;
        for (KnowledgeGraphNode node : nodes) {
            if (node == null || isBlank(node.getNodeId())) continue;
            seedScores.merge(node.getNodeId(), score, Math::max);
        }
    }

    private KnowledgeGraphNode buildNode(String nodeId, String nodeType, String displayName, String normalizedName, Instant now) {
        return KnowledgeGraphNode.builder()
                .nodeId(nodeId)
                .nodeType(nodeType)
                .displayName(displayName)
                .normalizedName(normalizedName)
                .updatedAt(now)
                .build();
    }

    private void addEdge(List<KnowledgeGraphEdge> edges,
                         Set<String> edgeKeys,
                         String fromNodeId,
                         String toNodeId,
                         String relationType,
                         double weight,
                         Instant now) {
        if (isBlank(fromNodeId) || isBlank(toNodeId) || Objects.equals(fromNodeId, toNodeId)) return;

        String edgeKey = fromNodeId + "|" + relationType + "|" + toNodeId;
        if (!edgeKeys.add(edgeKey)) return;

        edges.add(KnowledgeGraphEdge.builder()
                .fromNodeId(fromNodeId)
                .toNodeId(toNodeId)
                .relationType(relationType)
                .weight(Math.max(0.0, Math.min(weight, 1.0)))
                .updatedAt(now)
                .build());
    }

    private Map<String, Double> extractKeywordWeights(String text, int maxKeywords) {
        String normalized = normalize(text);
        if (normalized.isBlank()) return Map.of();

        Map<String, Integer> frequencies = new HashMap<>();
        for (String token : normalized.split("\\s+")) {
            if (token.length() < 2 || STOP_WORDS.contains(token)) continue;
            frequencies.merge(token, 1, Integer::sum);
        }
        if (frequencies.isEmpty()) return Map.of();

        int maxFrequency = frequencies.values().stream().max(Integer::compareTo).orElse(1);
        int limit = Math.max(1, maxKeywords);

        return frequencies.entrySet().stream()
                .sorted((a, b) -> {
                    int byFreq = Integer.compare(b.getValue(), a.getValue());
                    if (byFreq != 0) return byFreq;
                    return a.getKey().compareTo(b.getKey());
                })
                .limit(limit)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> 0.55 + 0.45 * ((double) entry.getValue() / maxFrequency),
                        (left, right) -> left,
                        LinkedHashMap::new
                ));
    }

    private Set<String> extractQueryTerms(String query) {
        return extractKeywordWeights(query, 12).keySet();
    }

    private Set<String> buildTopicTerms(String query) {
        String normalizedQuery = normalize(query);
        if (normalizedQuery.isBlank()) return Set.of();

        List<String> tokens = Arrays.stream(normalizedQuery.split("\\s+"))
                .filter(token -> token.length() >= 2 && !STOP_WORDS.contains(token))
                .toList();

        Set<String> terms = new LinkedHashSet<>(tokens);
        for (int i = 0; i < tokens.size() - 1; i++) {
            terms.add(tokens.get(i) + "_" + tokens.get(i + 1));
        }
        for (int i = 0; i < tokens.size() - 2; i++) {
            terms.add(tokens.get(i) + "_" + tokens.get(i + 1) + "_" + tokens.get(i + 2));
        }

        terms.add(normalizedQuery.replace(' ', '_'));
        terms.removeIf(String::isBlank);
        return terms;
    }

    private double jaccard(Collection<String> left, Collection<String> right) {
        if (left == null || right == null || left.isEmpty() || right.isEmpty()) return 0.0;
        Set<String> leftSet = new HashSet<>(left);
        Set<String> rightSet = new HashSet<>(right);

        Set<String> intersection = new HashSet<>(leftSet);
        intersection.retainAll(rightSet);

        Set<String> union = new HashSet<>(leftSet);
        union.addAll(rightSet);

        if (union.isEmpty()) return 0.0;
        return (double) intersection.size() / union.size();
    }

    private String chunkLabel(KnowledgeDocument doc) {
        if (!isBlank(doc.getTitle())) return doc.getTitle();
        if (!isBlank(doc.getTopic())) return doc.getTopic();
        return safeSource(doc.getSource());
    }

    private String safeSource(String source) {
        return isBlank(source) ? "unknown" : source.trim().toLowerCase(Locale.ROOT);
    }

    private String chunkNodeId(String chunkId) {
        return CHUNK_NODE_PREFIX + chunkId;
    }

    private String topicNodeId(String normalizedTopic) {
        return TOPIC_NODE_PREFIX + normalizedTopic;
    }

    private String keywordNodeId(String keyword) {
        return KEYWORD_NODE_PREFIX + keyword;
    }

    private boolean isChunkNode(String nodeId) {
        return nodeId != null && nodeId.startsWith(CHUNK_NODE_PREFIX);
    }

    private String toChunkId(String chunkNodeId) {
        return chunkNodeId.substring(CHUNK_NODE_PREFIX.length());
    }

    private String normalizeTopic(String topic) {
        String normalized = normalize(topic);
        if (normalized.isBlank()) return "general";
        return normalized.replace(' ', '_');
    }

    private String normalize(String value) {
        return VietnameseNormalizer.normalize(value);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
