package com.fashion.chatbotservice.service.impl;

import com.fashion.chatbotservice.model.IntentTrainingData;
import com.fashion.chatbotservice.repository.IntentTrainingDataRepository;
import com.fashion.chatbotservice.service.IntentClassifierService;
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
public class IntentClassifierServiceImpl implements IntentClassifierService {

    private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^a-z0-9\\s]");

    private final IntentTrainingDataRepository intentTrainingDataRepository;

    @Override
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

            if (best.confidence() < 0.24d) {
                return new IntentScore(GENERAL, Math.max(0.35d, best.confidence()));
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

        // 0. Out-of-Domain — ưu tiên CAO NHẤT, chặn trước khi tốn LLM token
        if (isOutOfDomain(normalizedMessage)) {
            return new IntentScore(OUT_OF_DOMAIN, 0.95d);
        }

        // 1. Chính sách / FAQ / Knowledge
        if (containsAnyWord(normalizedMessage,
                "freeship", "ship", "van chuyen", "giao hang", "phi ship",
                "doi tra", "tra hang", "hoan tien", "bao hanh", "chinh sach",
                "thanh toan", "phuong thuc", "cach dat hang", "lien he",
                "cua hang", "dia chi", "gio mo cua", "ho tro", "khieu nai")) {
            return new IntentScore(ASK_POLICY, 0.92d);
        }

        // 2. Đơn hàng — kiểm tra, theo dõi đơn
        if (containsAnyWord(normalizedMessage,
                "don hang", "kiem tra don", "theo doi don", "trang thai don",
                "don cua toi", "da dat", "giao den dau", "bao gio giao",
                "huy don", "xac nhan don", "ma don", "order")) {
            return new IntentScore(CHECK_ORDER, 0.91d);
        }

        // 3. Khuyến mãi — keyword rõ ràng
        if (containsAnyWord(normalizedMessage,
                "khuyen mai", "giam gia", "coupon", "ma giam", "voucher", "uu dai", "deal")) {
            return new IntentScore(ASK_PROMOTION, 0.9d);
        }

        // 4. Wishlist
        if (containsAnyWord(normalizedMessage,
                "wishlist", "yeu thich", "da luu", "sp da luu", "san pham da luu",
                "trong wishlist", "danh sach yeu thich")) {
            return new IntentScore(WISHLIST_RECOMMENDATION, 0.93d);
        }

        // 5. Loyalty / tri ân
        if (containsAnyWord(normalizedMessage,
                "diem thuong", "diem tich luy", "loyalty", "hang thanh vien",
                "quyen loi thanh vien", "tri an", "member", "vip")) {
            return new IntentScore(LOYALTY_BENEFIT, 0.91d);
        }

        boolean hasFashionKeyword = containsAnyWord(normalizedMessage,
                "ao", "quan", "vay", "dam", "giay", "tui", "khoac", "so mi", "thun", "jean", "polo");
        boolean hasExplicitSizeSelection = normalizedMessage.matches(".*\\bsize\\s*(xs|s|m|l|xl|xxl|\\d{2})\\b.*");
        if (hasFashionKeyword && hasExplicitSizeSelection) {
            return new IntentScore(SEARCH_PRODUCT, 0.9d);
        }

        // 6. Size — CHỈ khi có keyword ĐẶC THÙ về số đo
        boolean hasSizeKeyword = containsAnyWord(normalizedMessage,
                "size", "so do", "vong nguc", "vong eo", "vong hong", "can nang", "chieu cao");
        boolean hasMeasurement = normalizedMessage.matches(".*\\d+\\s*(cm|kg|m\\d).*")
                || normalizedMessage.matches(".*\\b(1m\\d{2}|\\d{2,3}kg)\\b.*");
        if (hasSizeKeyword || hasMeasurement) {
            return new IntentScore(CONSULT_SIZE, 0.88d);
        }

        // 7. Outfit theo mùa / dịp
        if (containsAnyWord(normalizedMessage,
                "outfit", "phoi do", "mua he", "mua dong", "mua thu", "mua xuan", "trend", "di lam", "cong so", "di tiec", "su kien")) {
            return new IntentScore(CONSULT_SEASON, 0.86d);
        }

        // 8. Tìm sản phẩm — keyword các loại đồ thời trang
        if (containsAnyWord(normalizedMessage,
                "tim", "co ban", "san pham", "ao", "quan", "vay", "dam", "giay",
                "tui", "non", "mu", "khoac", "so mi", "thun", "jean", "con hang",
                "ton kho", "mau sac", "chat lieu")) {
            return new IntentScore(SEARCH_PRODUCT, 0.85d);
        }

        // 9. Chào hỏi / cảm ơn / tạm biệt
        if (containsAnyWord(normalizedMessage,
                "xin chao", "chao ban", "hello", "hi", "hey",
                "cam on", "thank", "thanks",
                "tam biet", "bye", "hen gap lai",
                "ban la ai", "ban ten gi", "giup toi", "giup minh")) {
            return new IntentScore(GREETING, 0.83d);
        }

        return null;
    }

    /**
     * Detect out-of-domain queries: coding, math, prompt injection, politics, off-topic.
     * Returns true if the message is clearly NOT about fashion/shopping/orders.
     */
    private boolean isOutOfDomain(String normalizedMessage) {
        // Prompt injection patterns (highest priority)
        if (containsAnyWord(normalizedMessage,
                "ignore previous", "system prompt", "jailbreak",
                "forget your instructions", "new role", "pretend you are",
                "ignore all", "bypass", "override instructions")) {
            return true;
        }

        // Coding / Programming — use specific phrases to avoid false positives with "mã code", "dress code"
        if (containsAnyWord(normalizedMessage,
                "viet code", "lap trinh", "phan mem", "fix bug",
                "python", "javascript", "debug", "compile",
                "algorithm", "programming", "html", "css", "sql")) {
            return true;
        }

        // Math / Science (academic)
        if (containsAnyWord(normalizedMessage,
                "phuong trinh", "tich phan", "dao ham", "toan hoc",
                "vat ly", "hoa hoc", "sinh hoc", "cong thuc toan")) {
            return true;
        }

        // Political / Sensitive
        if (containsAnyWord(normalizedMessage,
                "chinh tri", "ton giao", "bat hop phap",
                "ma tuy", "vu khi", "khung bo")) {
            return true;
        }

        // General off-topic
        if (containsAnyWord(normalizedMessage,
                "bong da", "world cup", "nau an", "cong thuc nau",
                "trieu chung", "thuoc", "bac si", "kham benh",
                "lyrics", "loi bai hat")) {
            return true;
        }

        return false;
    }

    /**
     * Check if text contains any of the keywords as WHOLE WORDS (not substrings).
     */
    private boolean containsAnyWord(String text, String... keywords) {
        for (String keyword : keywords) {
            String regex = "(?:^|\\s)" + Pattern.quote(keyword) + "(?:\\s|$)";
            if (Pattern.compile(regex).matcher(text).find()) {
                return true;
            }
        }
        return false;
    }

    @Override
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
                    .intentName(WISHLIST_RECOMMENDATION)
                    .examples(List.of(
                        "trong wishlist cua toi co gi",
                        "san pham toi da luu",
                        "wishlist cua toi",
                        "goi y theo wishlist"
                    ))
                    .responseTemplate("Mình sẽ mở wishlist và chọn giúp bạn các mẫu đáng chú ý.")
                    .createdAt(Instant.now())
                    .build(),
                IntentTrainingData.builder()
                    .intentName(LOYALTY_BENEFIT)
                    .examples(List.of(
                        "toi con bao nhieu diem",
                        "hang thanh vien cua toi",
                        "quyen loi loyalty",
                        "diem thuong cua toi"
                    ))
                    .responseTemplate("Mình sẽ kiểm tra điểm thưởng và quyền lợi thành viên cho bạn.")
                    .createdAt(Instant.now())
                    .build(),
                IntentTrainingData.builder()
                    .intentName(SEARCH_PRODUCT)
                    .examples(List.of(
                        "tim ao khoac trang",
                        "co ban quan jean khong",
                        "san pham moi",
                        "dam du tiec mau den",
                        "ao thun nu gia duoi 300k"
                    ))
                    .responseTemplate("Mình sẽ tìm sản phẩm phù hợp cho bạn ngay.")
                    .createdAt(Instant.now())
                    .build(),
                IntentTrainingData.builder()
                    .intentName(CHECK_ORDER)
                    .examples(List.of(
                        "kiem tra don hang",
                        "don hang cua toi",
                        "trang thai don",
                        "don giao chua",
                        "don ORD-123 den dau roi",
                        "theo doi don hang",
                        "bao gio giao hang"
                    ))
                    .responseTemplate("Mình sẽ kiểm tra đơn hàng cho bạn ngay.")
                    .createdAt(Instant.now())
                    .build(),
                IntentTrainingData.builder()
                    .intentName(GREETING)
                    .examples(List.of(
                        "xin chao",
                        "hello",
                        "chao ban",
                        "ban la ai",
                        "cam on",
                        "tam biet",
                        "giup toi"
                    ))
                    .responseTemplate("Xin chào! Mình là trợ lý thời trang AI.")
                    .createdAt(Instant.now())
                    .build(),
                IntentTrainingData.builder()
                    .intentName(GENERAL)
                    .examples(List.of(
                        "giup toi chon do",
                        "tu van mua sam",
                        "mua gi dep"
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
        defaults.put(CONSULT_SIZE, List.of("tu van size", "size nao vua", "so do co the"));
        defaults.put(CONSULT_SEASON, List.of("goi y theo mua", "phoi do", "outfit di lam", "trend"));
        defaults.put(ASK_PROMOTION, List.of("khuyen mai", "giam gia", "coupon", "deal"));
        defaults.put(WISHLIST_RECOMMENDATION, List.of("wishlist cua toi", "trong wishlist co gi", "san pham da luu", "do yeu thich"));
        defaults.put(LOYALTY_BENEFIT, List.of("diem thuong", "hang thanh vien", "loyalty", "quyen loi vip"));
        defaults.put(SEARCH_PRODUCT, List.of("tim ao", "co ban quan", "san pham", "dam du tiec", "ao khoac"));
        defaults.put(CHECK_ORDER, List.of("kiem tra don", "don hang", "trang thai don", "giao hang", "theo doi don"));
        defaults.put(GREETING, List.of("xin chao", "hello", "cam on", "ban la ai", "tam biet"));
        defaults.put(GENERAL, List.of("giup toi chon do", "tu van mua sam"));
        return defaults;
    }

    private double scoreIntent(List<String> examples, Set<String> messageTokens) {
        double best = 0.0d;
        for (String example : examples) {
            Set<String> exampleTokens = tokenize(normalize(example));
            if (exampleTokens.isEmpty()) continue;
            long overlap = exampleTokens.stream().filter(messageTokens::contains).count();
            double score = (double) overlap / (double) Math.max(exampleTokens.size(), messageTokens.size());
            best = Math.max(best, score);
        }
        return best;
    }

    private String normalize(String text) {
        if (text == null) return "";
        String lower = text.toLowerCase(Locale.ROOT);
        // Vietnamese đ/Đ is NOT decomposed by NFD — must convert manually BEFORE stripping
        lower = lower.replace('\u0111', 'd').replace('\u0110', 'd');
        String normalized = Normalizer.normalize(lower, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return NON_ALPHANUMERIC.matcher(normalized).replaceAll(" ").replaceAll("\\s+", " ").trim();
    }

    private Set<String> tokenize(String normalizedText) {
        if (normalizedText == null || normalizedText.isBlank()) return Set.of();
        String[] parts = normalizedText.split("\\s+");
        Set<String> tokens = new HashSet<>();
        for (String token : parts) {
            if (token.length() >= 2) tokens.add(token);
        }
        return tokens;
    }
}
