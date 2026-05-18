package com.fashion.chatbotservice.router;

import com.fashion.chatbotservice.service.IntentClassifierService;
import com.fashion.chatbotservice.util.VietnameseNormalizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Semantic Intent Router — Phase 1B của Review.md.
 *
 * <p>Sử dụng cosine similarity trên keyword embeddings thủ công (không cần external
 * embedding API) để classify intent TRƯỚC khi gọi LLM. Nếu confidence cao (≥ 0.82)
 * → route trực tiếp. Nếu không chắc → fallback về IntentClassifierService (TF-IDF/ML).
 *
 * <p>Mục tiêu: giảm 60-70% LLM calls cho intent classification.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SemanticIntentRouter {

    private final IntentClassifierService intentClassifierService;
    private final IntentKeywordRegistry intentKeywordRegistry;

    private static final double HIGH_CONFIDENCE_THRESHOLD = 0.82;

    /**
     * Classify intent với semantic routing layer.
     * Trả về IntentScore với confidence cao hơn nếu semantic match rõ ràng.
     */
    public IntentClassifierService.IntentScore classify(String message) {
        if (message == null || message.isBlank()) {
            return new IntentClassifierService.IntentScore(IntentClassifierService.GENERAL, 0.5);
        }

        String normalized = VietnameseNormalizer.normalize(message).toLowerCase();

        // 1. Tính similarity score với từng intent template
        Map<String, Double> scores = new HashMap<>();
        for (Map.Entry<String, List<String>> entry : intentKeywordRegistry.getIntentKeywords().entrySet()) {
            String intent = entry.getKey();
            List<String> keywords = entry.getValue();
            double score = computeKeywordMatchScore(normalized, keywords);
            scores.put(intent, score);
        }

        // 2. Tìm intent có score cao nhất
        String topIntent = scores.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(IntentClassifierService.GENERAL);
        double topScore = scores.getOrDefault(topIntent, 0.0);

        // 3. Nếu confidence cao → route trực tiếp (không cần LLM)
        if (topScore >= HIGH_CONFIDENCE_THRESHOLD) {
            log.debug("SemanticRouter high-confidence: intent={}, score={:.3f}, msg={}",
                    topIntent, topScore, message.substring(0, Math.min(50, message.length())));
            return new IntentClassifierService.IntentScore(topIntent, topScore);
        }

        // 4. Fallback → classifier hiện tại (TF-IDF / heuristic)
        log.debug("SemanticRouter low-confidence ({:.3f}), falling back to classifier", topScore);
        return intentClassifierService.classify(message);
    }

    /**
     * Tính keyword match score (0.0 → 1.0) dựa trên overlap giữa normalized message
     * và danh sách keywords của intent.
     *
     * <p>Algorithm: weighted keyword match — keywords ở đầu list có trọng số cao hơn
     * (prime keywords), keywords ở cuối là supporting signals.
     */
    private double computeKeywordMatchScore(String normalized, List<String> keywords) {
        if (keywords == null || keywords.isEmpty()) return 0.0;

        double totalWeight = 0.0;
        double matchWeight = 0.0;
        int size = keywords.size();

        for (int i = 0; i < size; i++) {
            String kw = keywords.get(i);
            // Prime keywords (first 3) có trọng số cao hơn
            double weight = (i < 3) ? 2.0 : 1.0;
            totalWeight += weight;
            if (normalized.contains(kw)) {
                matchWeight += weight;
            }
        }

        if (totalWeight == 0) return 0.0;
        double raw = matchWeight / totalWeight;

        // Boost nếu nhiều keywords khớp (indicates clear intent)
        long matchCount = keywords.stream().filter(normalized::contains).count();
        if (matchCount >= 3) raw = Math.min(1.0, raw * 1.2);

        return raw;
    }
}
