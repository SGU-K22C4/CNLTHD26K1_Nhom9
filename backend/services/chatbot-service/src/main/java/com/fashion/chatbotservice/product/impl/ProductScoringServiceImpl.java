package com.fashion.chatbotservice.product.impl;

import com.fashion.chatbotservice.dto.ChatResponse;
import com.fashion.chatbotservice.model.ChatSession;
import com.fashion.chatbotservice.product.ProductMetadataProfile;
import com.fashion.chatbotservice.product.ProductScoringService;
import com.fashion.chatbotservice.styling.BodyShapeAdvice;
import com.fashion.chatbotservice.styling.BodyShapeAdvisorService;
import com.fashion.chatbotservice.styling.OccasionAdvice;
import com.fashion.chatbotservice.styling.OccasionAdvisorService;
import com.fashion.chatbotservice.util.VietnameseNormalizer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class ProductScoringServiceImpl implements ProductScoringService {

    private final BodyShapeAdvisorService bodyShapeAdvisorService;
    private final OccasionAdvisorService occasionAdvisorService;

    @Override
    public ScoreResult score(ChatResponse.ProductSuggestion suggestion,
                             ProductMetadataProfile metadata,
                             ChatSession.PreferenceProfile profile,
                             String search,
                             Long minPrice,
                             Long maxPrice,
                             String color,
                             String size) {
        double score = 0.0d;
        List<String> reasons = new ArrayList<>();

        String normalizedSearch = normalize(search);
        String normalizedName = normalize((suggestion.getName() == null ? "" : suggestion.getName()) + " "
                + (suggestion.getCategory() == null ? "" : suggestion.getCategory()));

        OccasionAdvice occasionAdvice = occasionAdvisorService.advise(search, profile, metadata);
        BodyShapeAdvice bodyShapeAdvice = bodyShapeAdvisorService.advise(profile, metadata);

        double occasionMatch = scoreOccasionMatch(occasionAdvice, metadata);
        if (occasionMatch > 0.0d) {
            score += occasionMatch;
            reasons.add(buildOccasionReason(occasionAdvice));
        }

        double styleMatch = scoreStyleMatch(profile, normalizedSearch, metadata);
        if (styleMatch > 0.0d) {
            score += styleMatch;
            reasons.add("Hop vibe ban dang uu tien hon.");
        }

        double bodyFitMatch = scoreBodyFitMatch(bodyShapeAdvice, metadata);
        if (bodyFitMatch > 0.0d) {
            score += bodyFitMatch;
            reasons.add(buildBodyShapeReason(bodyShapeAdvice));
        }

        double budgetMatch = scoreBudgetMatch(suggestion, profile, minPrice, maxPrice);
        if (budgetMatch > 0.0d) {
            score += budgetMatch;
            reasons.add("Nam trong tam gia de chot.");
        }

        if (size != null && !size.isBlank() && safeList(suggestion.getAvailableSizes()).stream()
                .anyMatch(item -> normalize(item).equals(normalize(size)))) {
            score += 2.4d;
            reasons.add("Co dung size ban dang can.");
        } else if (!safeList(suggestion.getAvailableSizes()).isEmpty()) {
            score += 0.8d;
        } else {
            score -= 2.0d;
        }

        if (color != null && !color.isBlank() && safeList(suggestion.getAvailableColors()).stream()
                .anyMatch(item -> normalize(item).contains(normalize(color)))) {
            score += 1.8d;
            reasons.add("Co dung tong mau ban uu tien.");
        }

        score += metadata.getVersatilityScore() * 0.18d;
        if (metadata.getVersatilityScore() >= 8) {
            reasons.add("De phoi va mac duoc nhieu dip.");
        }

        if ("safe".equals(metadata.getFashionRisk()) && containsAny(normalizedSearch, "an toan", "de mac", "de phoi")) {
            score += 1.6d;
        }
        if ("medium".equals(metadata.getFashionRisk()) && containsAny(normalizedSearch, "noi bat", "co diem nhan")) {
            score += 1.2d;
        }

        if (normalizedName.contains("ao so mi") && normalizedSearch.contains("ao so mi")) {
            score += 1.0d;
        }

        reasons.addAll(summarizeWhyBuy(metadata, occasionAdvice, bodyShapeAdvice));
        return new ScoreResult(score, dedupeReasons(reasons));
    }

    private double scoreOccasionMatch(OccasionAdvice occasionAdvice, ProductMetadataProfile metadata) {
        if (occasionAdvice == null || occasionAdvice.getOccasion() == null || occasionAdvice.getOccasion().isBlank()) {
            return 0.0d;
        }
        return metadata.getOccasionTags().contains(occasionAdvice.getOccasion()) ? 2.4d : 0.0d;
    }

    private double scoreStyleMatch(ChatSession.PreferenceProfile profile,
                                   String normalizedSearch,
                                   ProductMetadataProfile metadata) {
        String explicitVibe = profile != null && profile.getStylingSlots() != null
                ? normalize(profile.getStylingSlots().getStyleVibe())
                : "";
        if (explicitVibe.isBlank()) {
            if (containsAny(normalizedSearch, "toi gian", "minimal") && metadata.getStyleTags().contains("minimal")) {
                return 1.8d;
            }
            if (containsAny(normalizedSearch, "lich su", "office") && metadata.getStyleTags().contains("office")) {
                return 1.8d;
            }
            return 0.0d;
        }

        if ("minimal".equals(explicitVibe) && metadata.getStyleTags().contains("minimal")) return 1.8d;
        if ("smart".equals(explicitVibe) && (metadata.getStyleTags().contains("office")
                || metadata.getStyleTags().contains("smart_casual"))) return 1.8d;
        if ("statement".equals(explicitVibe) && metadata.getStyleTags().contains("statement")) return 1.8d;
        if ("youthful".equals(explicitVibe) && metadata.getVibeTags().contains("young")) return 1.4d;
        return 0.0d;
    }

    private double scoreBodyFitMatch(BodyShapeAdvice bodyShapeAdvice, ProductMetadataProfile metadata) {
        if (bodyShapeAdvice == null || bodyShapeAdvice.getRecommendedFit() == null || bodyShapeAdvice.getRecommendedFit().isBlank()) {
            return 0.0d;
        }

        double baseScore = 0.4d * bodyShapeAdvice.getConfidence();
        String recommendedFit = normalize(bodyShapeAdvice.getRecommendedFit());
        boolean fitMatched = metadata.getFitTags().stream()
                .map(this::normalize)
                .anyMatch(tag -> tag.contains(recommendedFit)
                        || ("gon".equals(recommendedFit) && (tag.contains("regular") || tag.contains("straight"))));
        if (fitMatched) {
            return baseScore + 0.8d;
        }
        if ("structured".equals(recommendedFit) && metadata.getFitTags().contains("structured")) {
            return baseScore + 0.8d;
        }
        return baseScore;
    }

    private double scoreBudgetMatch(ChatResponse.ProductSuggestion suggestion,
                                    ChatSession.PreferenceProfile profile,
                                    Long minPrice,
                                    Long maxPrice) {
        Long preferredMax = maxPrice != null ? maxPrice : parseBudget(profile == null ? null : profile.getBudget());
        Long price = parsePriceToLong(suggestion.getPrice());
        if (price == null || preferredMax == null) {
            return 0.0d;
        }
        if (minPrice != null && price < minPrice) {
            return 0.0d;
        }
        if (price <= preferredMax) {
            double ratio = (double) price / preferredMax;
            if (ratio >= 0.6d && ratio <= 1.0d) return 1.7d;
            return 1.1d;
        }
        return -0.8d;
    }

    private List<String> summarizeWhyBuy(ProductMetadataProfile metadata,
                                         OccasionAdvice occasionAdvice,
                                         BodyShapeAdvice bodyShapeAdvice) {
        List<String> reasons = new ArrayList<>();
        if (metadata.getWhyBuyTags().contains("easy_to_match")) {
            reasons.add("De phoi voi item co ban.");
        }
        if (metadata.getWhyBuyTags().contains("safe_choice")) {
            reasons.add("La lua chon kha an toan neu ban muon mac de.");
        }
        if (metadata.getWhyBuyTags().contains("looks_premium")) {
            reasons.add("Nhin co diem nhan nen tong the trong cao cap hon.");
        }
        if (occasionAdvice != null && occasionAdvice.getColorGuidance() != null && !occasionAdvice.getColorGuidance().isBlank()) {
            reasons.add(occasionAdvice.getColorGuidance());
        }
        if (bodyShapeAdvice != null && !bodyShapeAdvice.getAvoid().isEmpty()) {
            reasons.add(bodyShapeAdvice.getAvoid().getFirst());
        }
        return reasons;
    }

    private String buildOccasionReason(OccasionAdvice occasionAdvice) {
        if (occasionAdvice == null || occasionAdvice.getRecommendedDirection() == null || occasionAdvice.getRecommendedDirection().isBlank()) {
            return "Hop dip mac ban dang nham toi.";
        }
        return "Hop dip nay vi giup outfit " + occasionAdvice.getRecommendedDirection() + ".";
    }

    private String buildBodyShapeReason(BodyShapeAdvice bodyShapeAdvice) {
        if (bodyShapeAdvice == null || bodyShapeAdvice.getStylingTips().isEmpty()) {
            return "Form nay kha an toan voi dang nguoi dang mo ta.";
        }
        return bodyShapeAdvice.getStylingTips().getFirst();
    }

    private List<String> dedupeReasons(List<String> reasons) {
        List<String> deduped = new ArrayList<>();
        for (String reason : reasons) {
            if (reason == null || reason.isBlank()) {
                continue;
            }
            if (!deduped.contains(reason)) {
                deduped.add(reason);
            }
        }
        return deduped;
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

    private boolean containsAny(String haystack, String... tokens) {
        for (String token : tokens) {
            if (haystack.contains(token)) {
                return true;
            }
        }
        return false;
    }

    private String normalize(String text) {
        return VietnameseNormalizer.normalize(text == null ? "" : text).toLowerCase();
    }

    private List<String> safeList(List<String> values) {
        return values == null ? List.of() : values;
    }
}
