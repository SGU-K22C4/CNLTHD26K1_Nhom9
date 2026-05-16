package com.fashion.chatbotservice.service.impl;

import com.fashion.chatbotservice.dto.ChatResponse;
import com.fashion.chatbotservice.model.ChatSession;
import com.fashion.chatbotservice.service.ProductRecommendationService;
import com.fashion.chatbotservice.service.ProductTaxonomyService;
import com.fashion.chatbotservice.util.VietnameseNormalizer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ProductRecommendationServiceImpl implements ProductRecommendationService {

    private final ProductTaxonomyService productTaxonomyService;

    @Override
    public List<ChatResponse.ProductSuggestion> rankSuggestions(
            List<ChatResponse.ProductSuggestion> suggestions,
            ChatSession.PreferenceProfile profile,
            String search,
            Long minPrice,
            Long maxPrice,
            String color,
            String size) {
        if (suggestions == null || suggestions.isEmpty()) return suggestions;

        List<ScoredSuggestion> scored = new ArrayList<>();
        Long preferredMaxPrice = (maxPrice != null) ? maxPrice : parseBudget(profile == null ? null : profile.getBudget());
        List<String> searchTokens = buildSemanticTokens(search);
        String normalizedSearch = normalizeText(search);
        String inferredOccasion = productTaxonomyService.inferOccasionContext(normalizedSearch);
        boolean wantsSafeOption = normalizedSearch.contains("an toan")
                || normalizedSearch.contains("de mac")
                || normalizedSearch.contains("de phoi")
                || normalizedSearch.contains("basic");
        boolean wantsStatementOption = normalizedSearch.contains("noi bat")
                || normalizedSearch.contains("co diem nhan")
                || normalizedSearch.contains("statement");

        int index = 0;
        for (ChatResponse.ProductSuggestion suggestion : suggestions) {
            double score = 0.0d;
            List<String> reasons = new ArrayList<>();
            String haystack = normalizeText(stringValue(suggestion.getName()) + " " + stringValue(suggestion.getCategory()));
            Set<String> taxonomyLabels = productTaxonomyService.inferTaxonomyLabels(haystack);

            if (!searchTokens.isEmpty()) {
                int tokenHits = 0;
                for (String token : searchTokens) {
                    if (!token.isBlank() && haystack.contains(token)) {
                        tokenHits++;
                    }
                }
                if (tokenHits > 0) {
                    score += Math.min(3.2d, tokenHits * 1.2d);
                    reasons.add("Đúng nhu cầu đang tìm");
                }
            }

            if (!safeList(suggestion.getAvailableSizes()).isEmpty()) {
                score += 0.9d;
            } else {
                score -= 2.5d;
                reasons.add("Cần kiểm tra lại tồn size");
            }

            boolean explicitSizeMatch = size != null && !size.isBlank()
                    && safeList(suggestion.getAvailableSizes()).stream()
                    .anyMatch(avail -> normalizeText(avail).equals(normalizeText(size)));
            if (explicitSizeMatch) {
                score += 2.4d;
                reasons.add("Có đúng size bạn đang cần");
            } else if (profile != null && hasAnyMatch(profile.getPreferredSizes(), suggestion.getAvailableSizes())) {
                score += 1.4d;
                reasons.add("Hợp size bạn hay mặc");
            }

            boolean explicitColorMatch = color != null && !color.isBlank()
                    && safeList(suggestion.getAvailableColors()).stream()
                    .anyMatch(avail -> normalizeText(avail).contains(normalizeText(color)));
            if (explicitColorMatch) {
                score += 2.6d;
                reasons.add("Có đúng tông màu bạn ưu tiên");
            } else if (profile != null && hasAnyMatch(profile.getPreferredColors(), suggestion.getAvailableColors())) {
                score += 1.8d;
                reasons.add("Hợp màu bạn thường thích");
            }

            boolean categoryMatch = profile != null && hasCategoryMatch(profile.getPreferredCategories(), suggestion.getCategory());
            if (categoryMatch) {
                score += 1.4d;
                reasons.add("Đúng nhóm đồ bạn quan tâm");
            }

            if (profile != null
                    && profile.getLastProductCategoryQueried() != null
                    && !profile.getLastProductCategoryQueried().isBlank()
                    && haystack.contains(normalizeText(profile.getLastProductCategoryQueried()))) {
                score += 0.9d;
            }

            Long productPrice = parsePriceToLong(suggestion.getPrice());
            if (productPrice != null) {
                if (minPrice != null && productPrice >= minPrice) {
                    score += 0.5d;
                }
                if (preferredMaxPrice != null) {
                    if (productPrice <= preferredMaxPrice) {
                        score += 1.5d;
                        reasons.add("Nằm trong tầm giá dễ chốt");
                        score += budgetClosenessBonus(productPrice, preferredMaxPrice);
                    } else {
                        score -= 1.0d;
                    }
                }
            }

            double styleScore = scoreStyleFit(profile, haystack);
            if (styleScore > 0.0d) {
                score += styleScore;
                reasons.add("Hợp phong cách đang ưu tiên");
            }

            if (profile != null && profile.getFocusTags() != null) {
                score += scoreFocusTags(profile.getFocusTags(), haystack);
            }

            double occasionScore = scoreOccasionFit(inferredOccasion, taxonomyLabels);
            if (occasionScore > 0.0d) {
                score += occasionScore;
                reasons.add(buildOccasionReason(inferredOccasion));
            }
            if ((inferredOccasion == null || inferredOccasion.isBlank())
                    && profile != null
                    && profile.getPreferredOccasions() != null
                    && !profile.getPreferredOccasions().isEmpty()) {
                for (String preferredOccasion : profile.getPreferredOccasions()) {
                    double rememberedOccasionScore = scoreOccasionFit(normalizeText(preferredOccasion), taxonomyLabels);
                    if (rememberedOccasionScore > 0.0d) {
                        score += Math.min(1.1d, rememberedOccasionScore);
                        reasons.add("Há»£p dá»‹p máº·c báº¡n thÆ°á»ng chá»n");
                        break;
                    }
                }
            }

            if (wantsSafeOption) {
                double safetyScore = scoreSafetyFit(taxonomyLabels);
                if (safetyScore > 0.0d) {
                    score += safetyScore;
                    reasons.add("Dễ mặc và dễ phối");
                }
            }

            if (wantsStatementOption) {
                double statementScore = scoreStatementFit(taxonomyLabels);
                if (statementScore > 0.0d) {
                    score += statementScore;
                    reasons.add("Có điểm nhấn hơn trong outfit");
                }
            }

            suggestion.setReason(buildBusinessReason(reasons));
            scored.add(new ScoredSuggestion(suggestion, score, index++));
        }

        return scored.stream()
                .sorted(Comparator.comparingDouble(ScoredSuggestion::score).reversed()
                        .thenComparingInt(ScoredSuggestion::index))
                .map(ScoredSuggestion::suggestion)
                .toList();
    }

    @Override
    public List<ChatResponse.ProductSuggestion> diversifySuggestionsByCategory(
            List<ChatResponse.ProductSuggestion> suggestions,
            int maxResults) {
        if (suggestions == null || suggestions.isEmpty()) return suggestions;

        int target = Math.min(maxResults, suggestions.size());
        Map<String, Integer> categoryCounts = new LinkedHashMap<>();
        List<ChatResponse.ProductSuggestion> diversified = new ArrayList<>();
        List<ChatResponse.ProductSuggestion> overflow = new ArrayList<>();

        for (ChatResponse.ProductSuggestion suggestion : suggestions) {
            String key = suggestion.getCategory() == null || suggestion.getCategory().isBlank()
                    ? "Khác"
                    : suggestion.getCategory();
            int count = categoryCounts.getOrDefault(key, 0);
            if (count < 2) {
                diversified.add(suggestion);
                categoryCounts.put(key, count + 1);
            } else {
                overflow.add(suggestion);
            }
            if (diversified.size() >= target) {
                return diversified;
            }
        }

        for (ChatResponse.ProductSuggestion suggestion : overflow) {
            diversified.add(suggestion);
            if (diversified.size() >= target) {
                break;
            }
        }

        return diversified.isEmpty() ? suggestions : diversified;
    }

    private List<String> buildSemanticTokens(String search) {
        String normalized = normalizeText(search);
        if (normalized.isBlank()) return List.of();

        Set<String> tokens = new LinkedHashSet<>();
        for (String token : normalized.split("\\s+")) {
            if (token.length() < 2) continue;
            if (Set.of("tim", "kiem", "mau", "size", "cho", "minh", "toi", "voi", "gia", "duoi", "tren").contains(token)) {
                continue;
            }
            tokens.add(token);
        }
        return new ArrayList<>(tokens);
    }

    private boolean hasAnyMatch(Set<String> preferredValues, List<String> actualValues) {
        if (preferredValues == null || preferredValues.isEmpty() || actualValues == null || actualValues.isEmpty()) {
            return false;
        }
        for (String preferred : preferredValues) {
            String normalizedPreferred = normalizeText(preferred);
            for (String actual : actualValues) {
                if (normalizeText(actual).contains(normalizedPreferred)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean hasCategoryMatch(Set<String> preferredCategories, String category) {
        if (preferredCategories == null || preferredCategories.isEmpty() || category == null) return false;
        String normalizedCategory = normalizeText(category);
        for (String preferred : preferredCategories) {
            if (normalizedCategory.contains(normalizeText(preferred))) return true;
        }
        return false;
    }

    private Long parseBudget(String budget) {
        if (budget == null || budget.isBlank()) return null;
        String cleaned = budget.toLowerCase(Locale.ROOT)
                .replaceAll("[^0-9kmtrd.]", " ")
                .trim();
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("([0-9][0-9.,]*)\\s*(k|tr|trieu|d|dong)?")
                .matcher(cleaned);
        Long maxPrice = null;
        while (m.find()) {
            try {
                double value = Double.parseDouble(m.group(1).replace(".", "").replace(",", ""));
                String unit = m.group(2);
                if ("k".equals(unit)) value *= 1_000;
                else if ("tr".equals(unit) || "trieu".equals(unit)) value *= 1_000_000;
                long vnd = Math.round(value);
                if (maxPrice == null || vnd > maxPrice) maxPrice = vnd;
            } catch (NumberFormatException ignored) {
            }
        }
        return maxPrice;
    }

    private Long parsePriceToLong(String price) {
        if (price == null || price.isBlank()) return null;
        String digits = price.replaceAll("[^0-9]", "");
        if (digits.isBlank()) return null;
        try {
            return Long.parseLong(digits);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private double budgetClosenessBonus(Long productPrice, Long preferredMaxPrice) {
        if (productPrice == null || preferredMaxPrice == null || preferredMaxPrice <= 0) {
            return 0.0d;
        }
        double ratio = (double) productPrice / preferredMaxPrice;
        if (ratio >= 0.6d && ratio <= 1.0d) {
            return 0.45d;
        }
        if (ratio >= 0.4d) {
            return 0.2d;
        }
        return 0.0d;
    }

    private double scoreStyleFit(ChatSession.PreferenceProfile profile, String haystack) {
        if (profile == null || profile.getStyle() == null || profile.getStyle().isBlank()) {
            return 0.0d;
        }
        String style = normalizeText(profile.getStyle());
        if (style.contains("minimal") || style.contains("basic")) {
            return containsAny(haystack, "basic", "regular", "so mi", "ao thun", "midi", "tron") ? 0.9d : 0.0d;
        }
        if (style.contains("thanh lich") || style.contains("elegant") || style.contains("smart casual")) {
            return containsAny(haystack, "so mi", "blazer", "midi", "dam", "trouser", "chan vay") ? 1.0d : 0.0d;
        }
        if (style.contains("sporty") || style.contains("casual") || style.contains("relaxed")) {
            return containsAny(haystack, "ao thun", "jean", "short", "hoodie", "bomber", "oversize") ? 0.9d : 0.0d;
        }
        return 0.0d;
    }

    private double scoreFocusTags(Set<String> focusTags, String haystack) {
        double score = 0.0d;
        for (String tag : focusTags) {
            String normalizedTag = normalizeText(tag);
            if (normalizedTag.startsWith("fit:")) {
                String fitValue = normalizedTag.substring(4);
                if (!fitValue.isBlank() && haystack.contains(fitValue)) {
                    score += 0.5d;
                }
            }
        }
        return score;
    }

    private double scoreOccasionFit(String occasion, Set<String> taxonomyLabels) {
        if (occasion == null || occasion.isBlank() || taxonomyLabels == null || taxonomyLabels.isEmpty()) {
            return 0.0d;
        }
        if (taxonomyLabels.contains(occasion)) {
            return 1.4d;
        }
        if ("office".equals(occasion) && taxonomyLabels.contains("safe")) {
            return 0.8d;
        }
        if ("party".equals(occasion) && taxonomyLabels.contains("statement")) {
            return 1.0d;
        }
        if ("casual".equals(occasion) && taxonomyLabels.contains("safe")) {
            return 0.5d;
        }
        return 0.0d;
    }

    private double scoreSafetyFit(Set<String> taxonomyLabels) {
        if (taxonomyLabels == null || taxonomyLabels.isEmpty()) {
            return 0.0d;
        }
        return taxonomyLabels.contains("safe") ? 1.0d : 0.0d;
    }

    private double scoreStatementFit(Set<String> taxonomyLabels) {
        if (taxonomyLabels == null || taxonomyLabels.isEmpty()) {
            return 0.0d;
        }
        return taxonomyLabels.contains("statement") || taxonomyLabels.contains("party") ? 0.9d : 0.0d;
    }

    private String buildOccasionReason(String occasion) {
        if (occasion == null || occasion.isBlank()) {
            return "Hợp ngữ cảnh đang tìm";
        }
        return switch (occasion) {
            case "office" -> "Hợp đi làm và dễ chốt";
            case "party" -> "Hợp dịp cần lên outfit";
            case "casual" -> "Hợp mặc hằng ngày hoặc đi chơi";
            case "summer" -> "Hợp thời tiết nóng hoặc mùa hè";
            case "winter" -> "Hợp thời tiết mát hoặc mùa lạnh";
            default -> "Hợp ngữ cảnh đang tìm";
        };
    }

    private String buildBusinessReason(List<String> reasons) {
        if (reasons == null || reasons.isEmpty()) {
            return "Dễ cân nhắc cho nhu cầu hiện tại";
        }
        return reasons.stream()
                .distinct()
                .limit(2)
                .reduce((left, right) -> left + " | " + right)
                .orElse("Dễ cân nhắc cho nhu cầu hiện tại");
    }

    private String normalizeText(String value) {
        return VietnameseNormalizer.normalize(value == null ? "" : value);
    }

    private boolean containsAny(String haystack, String... needles) {
        if (haystack == null || haystack.isBlank()) return false;
        for (String needle : needles) {
            if (needle != null && !needle.isBlank() && haystack.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    private List<String> safeList(List<String> values) {
        return values == null ? List.of() : values;
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private record ScoredSuggestion(ChatResponse.ProductSuggestion suggestion, double score, int index) {}
}
