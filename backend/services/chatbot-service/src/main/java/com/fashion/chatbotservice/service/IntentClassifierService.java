package com.fashion.chatbotservice.service;

import com.fashion.chatbotservice.model.IntentTrainingData;
import com.fashion.chatbotservice.repository.IntentTrainingDataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("null")
public class IntentClassifierService {

    public static final String CONSULT_SIZE = "CONSULT_SIZE";
    public static final String CONSULT_SEASON = "CONSULT_SEASON";
    public static final String ASK_PROMOTION = "ASK_PROMOTION";
    public static final String GENERAL = "GENERAL";

    private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^a-z0-9\\s]");

    private final IntentTrainingDataRepository intentTrainingDataRepository;

    public IntentScore classify(String message) {
        try {
            String normalizedMessage = normalize(message);
            IntentScore heuristic = classifyByHeuristics(normalizedMessage);
            if (heuristic != null) {
                return heuristic;
            }

            Map<String, List<String>> trainingSet = loadTrainingSet();
            Set<String> messageTokens = tokenize(normalizedMessage);
            if (messageTokens.isEmpty()) {
            return new IntentScore(GENERAL, 0.3d);
            }

            IntentScore best = trainingSet.entrySet().stream()
                .map(entry -> new IntentScore(entry.getKey(), scoreIntent(entry.getValue(), messageTokens)))
                .max(Comparator.comparingDouble(IntentScore::confidence))
                .orElse(new IntentScore(GENERAL, 0.3d));

            if (best.confidence < 0.24d) {
            return new IntentScore(GENERAL, Math.max(0.35d, best.confidence));
            }

            return best;
        } catch (Exception ex) {
            log.warn("Intent classification fallback because MongoDB is unavailable: {}", ex.getMessage());
            return new IntentScore(GENERAL, 0.35d);
        }
    }

    private IntentScore classifyByHeuristics(String normalizedMessage) {
        if (normalizedMessage == null || normalizedMessage.isBlank()) {
            return null;
        }

        if (containsAny(normalizedMessage,
                "khuyen mai", "giam gia", "coupon", "ma giam", "voucher", "uu dai", "deal")) {
            return new IntentScore(ASK_PROMOTION, 0.9d);
        }

        if (containsAny(normalizedMessage,
                "size", "so do", "v1", "v2", "v3", "nguc", "eo", "hong", "cm", "kg", "can nang", "chieu cao")) {
            return new IntentScore(CONSULT_SIZE, 0.88d);
        }

        if (containsAny(normalizedMessage,
                "outfit", "phoi do", "mua he", "mua dong", "mua thu", "mua xuan", "trend", "di lam", "cong so", "di tiec", "su kien")) {
            return new IntentScore(CONSULT_SEASON, 0.86d);
        }

        return null;
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    public void bootstrapDefaultIntentsIfNeeded() {
        try {
            if (intentTrainingDataRepository.count() > 0) {
            return;
            }

            List<IntentTrainingData> defaults = List.of(
                IntentTrainingData.builder()
                    .intentName(CONSULT_SIZE)
                    .examples(List.of(
                        "toi muon tu van size",
                        "ao nay size nao vua",
                        "toi cao 170 nang 65 thi mac size gi",
                        "tu van so do vong nguc eo hong"
                    ))
                    .responseTemplate("Mình có thể tư vấn size theo số đo để chọn vừa vặn nhất.")
                    .createdAt(Instant.now())
                    .build(),
                IntentTrainingData.builder()
                    .intentName(CONSULT_SEASON)
                    .examples(List.of(
                        "goi y do mua he",
                        "tu van trang phuc mua dong",
                        "di tiec nen mac gi",
                        "goi y outfit di lam"
                    ))
                    .responseTemplate("Mình sẽ gợi ý outfit theo mùa và phong cách bạn muốn.")
                    .createdAt(Instant.now())
                    .build(),
                IntentTrainingData.builder()
                    .intentName(ASK_PROMOTION)
                    .examples(List.of(
                        "co khuyen mai nao khong",
                        "san pham dang giam gia",
                        "ma giam gia hien tai",
                        "deal hom nay"
                    ))
                    .responseTemplate("Mình sẽ kiểm tra khuyến mãi đang hiệu lực để tư vấn chính xác.")
                    .createdAt(Instant.now())
                    .build(),
                IntentTrainingData.builder()
                    .intentName(GENERAL)
                    .examples(List.of(
                        "xin chao",
                        "ban la ai",
                        "cam on",
                        "giup toi chon do"
                    ))
                    .responseTemplate("Mình là trợ lý mua sắm, luôn sẵn sàng hỗ trợ bạn.")
                    .createdAt(Instant.now())
                    .build()
            );

            intentTrainingDataRepository.saveAll(defaults);
        } catch (Exception ex) {
            log.warn("Skip bootstrap intents because MongoDB is unavailable: {}", ex.getMessage());
        }
    }

    private Map<String, List<String>> loadTrainingSet() {
        List<IntentTrainingData> allIntents;
        try {
            allIntents = intentTrainingDataRepository.findAll();
        } catch (Exception ex) {
            log.warn("Use default training set because MongoDB is unavailable: {}", ex.getMessage());
            return defaultTrainingSet();
        }

        if (allIntents.isEmpty()) {
            return defaultTrainingSet();
        }

        Map<String, List<String>> trainingSet = new HashMap<>();
        for (IntentTrainingData intent : allIntents) {
            String intentName = intent.getIntentName() == null ? GENERAL : intent.getIntentName().trim().toUpperCase(Locale.ROOT);
            List<String> examples = intent.getExamples() == null ? List.of() : intent.getExamples();
            trainingSet.computeIfAbsent(intentName, key -> new ArrayList<>()).addAll(examples);
        }
        return trainingSet;
    }

    private Map<String, List<String>> defaultTrainingSet() {
        Map<String, List<String>> defaults = new HashMap<>();
        defaults.put(CONSULT_SIZE, List.of(
                "tu van size",
                "size nao vua",
                "so do co the"
        ));
        defaults.put(CONSULT_SEASON, List.of(
                "goi y theo mua",
                "phoi do",
                "outfit di lam",
                "trend"
        ));
        defaults.put(ASK_PROMOTION, List.of(
                "khuyen mai",
                "giam gia",
                "coupon",
                "deal"
        ));
        defaults.put(GENERAL, List.of(
                "xin chao",
                "cam on",
                "tro giup"
        ));
        return defaults;
    }

    private double scoreIntent(List<String> examples, Set<String> messageTokens) {
        double best = 0.0d;
        for (String example : examples) {
            Set<String> exampleTokens = tokenize(normalize(example));
            if (exampleTokens.isEmpty()) {
                continue;
            }

            long overlap = exampleTokens.stream().filter(messageTokens::contains).count();
            double score = (double) overlap / (double) Math.max(exampleTokens.size(), messageTokens.size());
            best = Math.max(best, score);
        }
        return best;
    }

    private String normalize(String text) {
        if (text == null) {
            return "";
        }
        String lower = text.toLowerCase(Locale.ROOT);
        String normalized = Normalizer.normalize(lower, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return NON_ALPHANUMERIC.matcher(normalized).replaceAll(" ").replaceAll("\\s+", " ").trim();
    }

    private Set<String> tokenize(String normalizedText) {
        if (normalizedText == null || normalizedText.isBlank()) {
            return Set.of();
        }
        String[] parts = normalizedText.split("\\s+");
        Set<String> tokens = new HashSet<>();
        for (String token : parts) {
            if (token.length() >= 2) {
                tokens.add(token);
            }
        }
        return tokens;
    }

    public record IntentScore(String intent, double confidence) {
    }
}
