package com.fashion.chatbotservice.service.impl;

import com.fashion.chatbotservice.model.KnowledgeDocument;
import com.fashion.chatbotservice.repository.KnowledgeDocumentRepository;
import com.fashion.chatbotservice.service.GraphRagService;
import com.fashion.chatbotservice.service.KnowledgeBaseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.regex.Pattern;

/**
 * Hybrid retrieval:
 * 1) Lexical scoring (keyword overlap)
 * 2) GraphRAG scoring (topic/keyword/chunk traversal)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class KnowledgeBaseServiceImpl implements KnowledgeBaseService {

    private static final Pattern NON_ALPHA = Pattern.compile("[^a-z0-9\\s]");

    private final KnowledgeDocumentRepository repository;
    private final GraphRagService graphRagService;
    private final PostRAGReranker postRAGReranker;

    @Value("${chatbot.knowledge.top-k:4}")
    private int topK;

    @Value("${chatbot.knowledge.min-score:0.3}")
    private double minScore;

    @Value("${chatbot.knowledge.graphrag.enabled:true}")
    private boolean graphEnabled;

    @Value("${chatbot.knowledge.graphrag.merge-weight:0.45}")
    private double graphMergeWeight;

    @Override
    @Cacheable(value = "knowledgeBase", key = "#query", unless = "#result == null || #result.isEmpty()")
    public List<SearchResult> search(String query) {
        if (query == null || query.isBlank()) return List.of();

        try {
            List<KnowledgeDocument> allDocs = repository.findAll();
            Set<String> queryTokens = tokenize(normalize(query));

            if (queryTokens.isEmpty() || allDocs.isEmpty()) return List.of();

            Map<String, Double> graphScores = computeGraphScores(query);
            boolean hasGraphSignal = !graphScores.isEmpty();
            double effectiveMinScore = hasGraphSignal ? Math.max(0.15, minScore * 0.75) : minScore;

            double safeGraphWeight = clamp(graphMergeWeight, 0.0, 1.0);
            double lexicalWeight = 1.0 - safeGraphWeight;

            List<SearchResult> rawResults = allDocs.stream()
                    .map(doc -> {
                        double lexicalScore = scoreDocument(doc, queryTokens);
                        double graphScore = doc.getId() == null
                                ? 0.0
                                : graphScores.getOrDefault(doc.getId(), 0.0);
                        double finalScore = combineScores(lexicalScore, graphScore, lexicalWeight, safeGraphWeight);

                        return new SearchResult(
                                doc.getContent(),
                                doc.getTitle(),
                                doc.getSource(),
                                doc.getTopic(),
                                finalScore
                        );
                    })
                    .filter(result -> result.score() >= effectiveMinScore)
                    .sorted(Comparator.comparingDouble(SearchResult::score).reversed())
                    .limit(topK * 3) // retrieve extra for re-ranker to work with
                    .toList();

            // Post-RAG Self-Reflection: re-rank, dedup and truncate for LLM quality
            List<SearchResult> reranked = postRAGReranker.rerank(rawResults, query, topK);
            log.debug("PostRAG: {} raw → {} after rerank", rawResults.size(), reranked.size());
            return reranked;
        } catch (Exception ex) {
            log.warn("Knowledge search failed: {}", ex.getMessage());
            return List.of();
        }
    }

    private Map<String, Double> computeGraphScores(String query) {
        if (!graphEnabled || !graphRagService.isEnabled()) return Map.of();

        try {
            return graphRagService.retrieve(query, topK * 3).stream()
                    .collect(Collectors.toMap(
                            GraphRagService.GraphHit::chunkId,
                            GraphRagService.GraphHit::score,
                            Math::max
                    ));
        } catch (Exception ex) {
            log.warn("GraphRAG retrieval failed, fallback to lexical only: {}", ex.getMessage());
            return Map.of();
        }
    }

    private double combineScores(double lexicalScore, double graphScore, double lexicalWeight, double graphWeight) {
        if (graphScore <= 0) return lexicalScore;
        if (lexicalScore <= 0) return graphScore * 0.85;

        double combined = (lexicalScore * lexicalWeight) + (graphScore * graphWeight) + 0.05;
        return Math.min(1.0, combined);
    }

    /**
     * Scoring: tỷ lệ overlap giữa query tokens và document tokens.
     * Bonus nếu match ở title/topic.
     */
    private double scoreDocument(KnowledgeDocument doc, Set<String> queryTokens) {
        Set<String> contentTokens = tokenize(normalize(doc.getContent()));
        Set<String> titleTokens = tokenize(normalize(doc.getTitle()));
        Set<String> topicTokens = tokenize(normalize(doc.getTopic()));

        long contentOverlap = queryTokens.stream().filter(contentTokens::contains).count();
        long titleOverlap = queryTokens.stream().filter(titleTokens::contains).count();
        long topicOverlap = queryTokens.stream().filter(topicTokens::contains).count();

        if (contentOverlap == 0 && titleOverlap == 0 && topicOverlap == 0) return 0.0;

        int maxTokens = Math.max(queryTokens.size(), 1);
        double contentScore = (double) contentOverlap / maxTokens;
        double titleBonus = (double) titleOverlap / maxTokens * 0.3;
        double topicBonus = topicOverlap > 0 ? 0.15 : 0.0;

        return Math.min(1.0, contentScore + titleBonus + topicBonus);
    }

    private Set<String> tokenize(String text) {
        if (text == null || text.isBlank()) return Set.of();
        String[] parts = text.split("\\s+");
        Set<String> tokens = new HashSet<>();
        for (String token : parts) {
            if (token.length() >= 2) tokens.add(token);
        }
        return tokens;
    }

    private String normalize(String text) {
        if (text == null) return "";
        String lower = text.toLowerCase();
        String normalized = Normalizer.normalize(lower, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return NON_ALPHA.matcher(normalized).replaceAll(" ").replaceAll("\\s+", " ").trim();
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
