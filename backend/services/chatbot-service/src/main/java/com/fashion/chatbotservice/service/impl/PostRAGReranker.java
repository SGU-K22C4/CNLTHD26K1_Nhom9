package com.fashion.chatbotservice.service.impl;

import com.fashion.chatbotservice.service.KnowledgeBaseService;
import com.fashion.chatbotservice.util.VietnameseNormalizer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Post-RAG Self-Reflection & Re-ranking Layer.
 *
 * <p>Sau khi GraphRAG + Lexical retrieval trả về danh sách chunk candidates,
 * layer này thực hiện thêm 3 bước cải thiện chất lượng context gửi lên LLM:
 *
 * <ol>
 *   <li><b>Relevance Re-scoring</b>: Tính lại điểm relevance dựa trên:
 *       <ul>
 *         <li>Exact phrase match (cụm từ khóa quan trọng, không bị cắt)</li>
 *         <li>Query coverage (bao nhiêu % query tokens được cover trong chunk)</li>
 *         <li>Source priority boost (sales playbook > style guide > policy)</li>
 *       </ul>
 *   </li>
 *   <li><b>Diversity Filter</b>: Loại bỏ duplicate/near-duplicate content
 *       để tránh LLM bị "confused" bởi context lặp lại.</li>
 *   <li><b>Context Truncation</b>: Giữ lại top-K chunk relevant nhất
 *       với total token budget hợp lý để không waste LLM context window.</li>
 * </ol>
 */
@Component
@Slf4j
public class PostRAGReranker {

    private static final Pattern NON_ALPHA = Pattern.compile("[^a-z0-9\\s]");

    // Source priority score bonuses
    private static final Map<String, Double> SOURCE_PRIORITY = Map.of(
            "sales-playbook.md",         0.30,
            "sales-consulting-playbook.md", 0.25,
            "style-guide.md",            0.20,
            "product-style-mapping.md",  0.18,
            "sales-objections.md",       0.15,
            "customer-objection-cases.md", 0.12,
            "product-hero-list.md",      0.10,
            "faq.md",                    0.05,
            "policy.md",                 0.0
    );

    // Jaccard similarity threshold for dedup (0-1, higher = more strict dedup)
    private static final double DEDUP_THRESHOLD = 0.60;

    // Max chars per chunk to include in final context (prevent token bloat)
    private static final int MAX_CHUNK_CHARS = 800;

    /**
     * Re-rank and filter the RAG results for better LLM context quality.
     *
     * @param rawResults  Original results from KnowledgeBaseService.search()
     * @param query       Original user query
     * @param topK        Maximum number of results to return
     * @return Re-ranked, deduplicated, truncated results
     */
    public List<KnowledgeBaseService.SearchResult> rerank(
            List<KnowledgeBaseService.SearchResult> rawResults,
            String query,
            int topK) {

        if (rawResults == null || rawResults.isEmpty()) return List.of();

        String normalizedQuery = normalize(query);
        Set<String> queryTokens = tokenize(normalizedQuery);
        List<String> queryPhrases = extractPhrases(normalizedQuery, 2, 3); // bigrams + trigrams

        // Step 1: Re-score each result
        List<ScoredResult> rescored = rawResults.stream()
                .map(result -> {
                    double baseScore = result.score();
                    double phraseBonus = computePhraseBonus(result, queryPhrases);
                    double coverageBonus = computeCoverageBonus(result, queryTokens);
                    double sourceBonus = getSourceBonus(result.source());
                    double finalScore = Math.min(1.0, baseScore + phraseBonus + coverageBonus + sourceBonus);

                    log.debug("PostRAG rescore [{}] base={} phrase={} cov={} src={} final={}", result.title(), String.format("%.2f",baseScore), String.format("%.2f",phraseBonus), String.format("%.2f",coverageBonus), String.format("%.2f",sourceBonus), String.format("%.2f",finalScore));

                    return new ScoredResult(result, finalScore);
                })
                .sorted(Comparator.comparingDouble(ScoredResult::score).reversed())
                .toList();

        // Step 2: Dedup — remove near-duplicate chunks
        List<ScoredResult> deduplicated = deduplicateByJaccard(rescored);

        // Step 3: Truncate content and return top-K
        return deduplicated.stream()
                .limit(Math.max(1, topK))
                .map(sr -> {
                    String truncatedContent = truncateContent(sr.result().content());
                    return new KnowledgeBaseService.SearchResult(
                            truncatedContent,
                            sr.result().title(),
                            sr.result().source(),
                            sr.result().topic(),
                            sr.score()
                    );
                })
                .toList();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Private helpers
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Bonus score if the chunk contains exact query phrase matches (bigram/trigram).
     * Exact phrase match is much more relevant than single-token overlap.
     */
    private double computePhraseBonus(KnowledgeBaseService.SearchResult result, List<String> queryPhrases) {
        if (queryPhrases.isEmpty()) return 0.0;
        String haystack = normalize(result.content() + " " + result.title());
        long matches = queryPhrases.stream().filter(haystack::contains).count();
        return Math.min(0.25, matches * 0.08);
    }

    /**
     * Coverage bonus: what fraction of query tokens appear in this chunk?
     * Higher coverage = chunk is more directly answering the question.
     */
    private double computeCoverageBonus(KnowledgeBaseService.SearchResult result, Set<String> queryTokens) {
        if (queryTokens.isEmpty()) return 0.0;
        String haystack = normalize(result.content() + " " + result.title());
        Set<String> haystackTokens = tokenize(haystack);
        long covered = queryTokens.stream().filter(haystackTokens::contains).count();
        double coverage = (double) covered / queryTokens.size();
        return Math.min(0.20, coverage * 0.20);
    }

    /** Source-specific priority boost based on content type. */
    private double getSourceBonus(String source) {
        if (source == null) return 0.0;
        String lowerSource = source.toLowerCase();
        return SOURCE_PRIORITY.entrySet().stream()
                .filter(e -> lowerSource.contains(e.getKey().replace(".md", "")))
                .mapToDouble(Map.Entry::getValue)
                .max()
                .orElse(0.0);
    }

    /**
     * Remove near-duplicate chunks based on Jaccard similarity of token sets.
     * If two chunks share > DEDUP_THRESHOLD of tokens, keep only the higher-scored one.
     */
    private List<ScoredResult> deduplicateByJaccard(List<ScoredResult> sorted) {
        List<ScoredResult> kept = new ArrayList<>();
        List<Set<String>> keptTokenSets = new ArrayList<>();

        for (ScoredResult candidate : sorted) {
            Set<String> candidateTokens = tokenize(normalize(candidate.result().content()));
            boolean isDuplicate = false;

            for (Set<String> keptTokens : keptTokenSets) {
                if (jaccard(candidateTokens, keptTokens) >= DEDUP_THRESHOLD) {
                    isDuplicate = true;
                    break;
                }
            }

            if (!isDuplicate) {
                kept.add(candidate);
                keptTokenSets.add(candidateTokens);
            }
        }
        return kept;
    }

    /** Extract n-grams (bigrams + trigrams) from normalized text for phrase matching. */
    private List<String> extractPhrases(String normalizedText, int minN, int maxN) {
        String[] tokens = normalizedText.split("\\s+");
        List<String> phrases = new ArrayList<>();
        for (int n = minN; n <= maxN; n++) {
            for (int i = 0; i <= tokens.length - n; i++) {
                String phrase = String.join(" ", Arrays.copyOfRange(tokens, i, i + n));
                if (phrase.length() >= 4) phrases.add(phrase); // skip too-short phrases
            }
        }
        return phrases;
    }

    /** Jaccard similarity between two token sets. */
    private double jaccard(Set<String> a, Set<String> b) {
        if (a.isEmpty() || b.isEmpty()) return 0.0;
        Set<String> intersection = new HashSet<>(a);
        intersection.retainAll(b);
        Set<String> union = new HashSet<>(a);
        union.addAll(b);
        return union.isEmpty() ? 0.0 : (double) intersection.size() / union.size();
    }

    /** Truncate long chunk content to MAX_CHUNK_CHARS to prevent token bloat. */
    private String truncateContent(String content) {
        if (content == null) return "";
        if (content.length() <= MAX_CHUNK_CHARS) return content;
        // Try to break at sentence boundary
        int breakPoint = content.lastIndexOf('.', MAX_CHUNK_CHARS);
        if (breakPoint > MAX_CHUNK_CHARS * 0.5) {
            return content.substring(0, breakPoint + 1).trim();
        }
        return content.substring(0, MAX_CHUNK_CHARS).trim() + "...";
    }

    private Set<String> tokenize(String text) {
        if (text == null || text.isBlank()) return Set.of();
        Set<String> tokens = new HashSet<>();
        for (String token : text.split("\\s+")) {
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

    private record ScoredResult(KnowledgeBaseService.SearchResult result, double score) {}
}
