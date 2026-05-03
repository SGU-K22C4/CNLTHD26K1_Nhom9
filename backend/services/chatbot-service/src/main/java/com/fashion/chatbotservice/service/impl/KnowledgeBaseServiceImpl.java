package com.fashion.chatbotservice.service.impl;

import com.fashion.chatbotservice.model.KnowledgeDocument;
import com.fashion.chatbotservice.repository.KnowledgeDocumentRepository;
import com.fashion.chatbotservice.service.KnowledgeBaseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Tìm kiếm thông tin trong Knowledge Base bằng keyword matching.
 * Pha 1: text-based scoring. Pha 2 sẽ chuyển sang embedding + semantic search.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class KnowledgeBaseServiceImpl implements KnowledgeBaseService {

    private static final Pattern NON_ALPHA = Pattern.compile("[^a-z0-9\\s]");

    private final KnowledgeDocumentRepository repository;

    @Value("${chatbot.knowledge.top-k:4}")
    private int topK;

    @Value("${chatbot.knowledge.min-score:0.3}")
    private double minScore;

    @Override
    public List<SearchResult> search(String query) {
        if (query == null || query.isBlank()) return List.of();

        try {
            List<KnowledgeDocument> allDocs = repository.findAll();
            Set<String> queryTokens = tokenize(normalize(query));

            if (queryTokens.isEmpty()) return List.of();

            return allDocs.stream()
                    .map(doc -> {
                        double score = scoreDocument(doc, queryTokens);
                        return new SearchResult(doc.getContent(), doc.getTitle(), doc.getSource(), doc.getTopic(), score);
                    })
                    .filter(result -> result.score() >= minScore)
                    .sorted(Comparator.comparingDouble(SearchResult::score).reversed())
                    .limit(topK)
                    .toList();
        } catch (Exception ex) {
            log.warn("Knowledge search failed: {}", ex.getMessage());
            return List.of();
        }
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
}
