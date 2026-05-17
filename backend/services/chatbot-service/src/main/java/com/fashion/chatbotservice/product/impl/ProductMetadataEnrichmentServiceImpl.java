package com.fashion.chatbotservice.product.impl;

import com.fashion.chatbotservice.dto.ChatResponse;
import com.fashion.chatbotservice.product.ProductMetadataEnrichmentService;
import com.fashion.chatbotservice.product.ProductMetadataProfile;
import com.fashion.chatbotservice.util.VietnameseNormalizer;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class ProductMetadataEnrichmentServiceImpl implements ProductMetadataEnrichmentService {

    @Override
    public ProductMetadataProfile enrich(ChatResponse.ProductSuggestion suggestion) {
        if (suggestion == null) {
            return ProductMetadataProfile.empty();
        }

        ProductMetadataProfile metadata = ProductMetadataProfile.builder()
                .productId(suggestion.getProductId())
                .productName(suggestion.getName())
                .category(suggestion.getCategory())
                .fitType("regular")
                .fashionRisk("safe")
                .stylingDifficulty("easy")
                .versatilityScore(5)
                .build();

        String haystack = normalize((suggestion.getName() == null ? "" : suggestion.getName()) + " "
                + (suggestion.getCategory() == null ? "" : suggestion.getCategory()));

        applyCategoryMapping(metadata, haystack);
        applyTitlePatternMapping(metadata, haystack);
        deriveWhyBuyTags(metadata, haystack);

        return metadata;
    }

    private void applyCategoryMapping(ProductMetadataProfile metadata, String haystack) {
        if (containsAny(haystack, "ao so mi", "shirt")) {
            metadata.getStyleTags().addAll(Set.of("minimal", "office", "smart_casual"));
            metadata.getOccasionTags().addAll(Set.of("work", "daily"));
            metadata.getFitTags().add("regular");
            metadata.getVibeTags().addAll(Set.of("clean", "mature"));
            metadata.getSeasonTags().addAll(Set.of("summer", "fall"));
            metadata.setVersatilityScore(8);
        } else if (containsAny(haystack, "ao thun", "t shirt", "tee")) {
            metadata.getStyleTags().addAll(Set.of("basic", "casual", "minimal"));
            metadata.getOccasionTags().addAll(Set.of("daily", "travel"));
            metadata.getVibeTags().addAll(Set.of("easy", "young"));
            metadata.getSeasonTags().addAll(Set.of("summer", "spring"));
            metadata.setVersatilityScore(9);
        } else if (containsAny(haystack, "blazer", "ao khoac", "jacket")) {
            metadata.getStyleTags().addAll(Set.of("office", "elevated", "smart_casual"));
            metadata.getOccasionTags().addAll(Set.of("work", "meeting", "dinner"));
            metadata.getFitTags().add("structured");
            metadata.getVibeTags().addAll(Set.of("mature", "polished"));
            metadata.getSeasonTags().addAll(Set.of("fall", "winter", "spring"));
            metadata.setFashionRisk("medium");
            metadata.setStylingDifficulty("medium");
            metadata.setVersatilityScore(7);
        } else if (containsAny(haystack, "quan tay", "trouser")) {
            metadata.getStyleTags().addAll(Set.of("office", "minimal", "smart_casual"));
            metadata.getOccasionTags().addAll(Set.of("work", "daily"));
            metadata.getFitTags().add("straight");
            metadata.getVibeTags().addAll(Set.of("clean", "mature"));
            metadata.setVersatilityScore(8);
        } else if (containsAny(haystack, "jean", "denim")) {
            metadata.getStyleTags().addAll(Set.of("casual", "daily", "relaxed"));
            metadata.getOccasionTags().addAll(Set.of("daily", "travel", "weekend"));
            metadata.getFitTags().addAll(Set.of("straight", "wide_leg"));
            metadata.getVibeTags().addAll(Set.of("young", "easy"));
            metadata.setVersatilityScore(8);
        } else if (containsAny(haystack, "dam", "vay", "dress")) {
            metadata.getStyleTags().addAll(Set.of("feminine", "elegant", "easy_polish"));
            metadata.getOccasionTags().addAll(Set.of("date", "office", "party_light"));
            metadata.getFitTags().addAll(Set.of("a_line", "wrap"));
            metadata.getVibeTags().addAll(Set.of("soft", "feminine"));
            metadata.getSeasonTags().addAll(Set.of("spring", "summer", "fall"));
            metadata.setFashionRisk("medium");
            metadata.setStylingDifficulty("medium");
            metadata.setVersatilityScore(7);
        } else if (containsAny(haystack, "chan vay", "skirt")) {
            metadata.getStyleTags().addAll(Set.of("feminine", "office", "trend_soft"));
            metadata.getOccasionTags().addAll(Set.of("work", "cafe", "dinner"));
            metadata.getFitTags().addAll(Set.of("a_line", "straight"));
            metadata.getVibeTags().addAll(Set.of("soft", "young"));
            metadata.getSeasonTags().addAll(Set.of("spring", "summer", "fall"));
            metadata.setFashionRisk("medium");
            metadata.setStylingDifficulty("medium");
            metadata.setVersatilityScore(7);
        }
    }

    private void applyTitlePatternMapping(ProductMetadataProfile metadata, String haystack) {
        if (containsAny(haystack, "linen")) {
            metadata.getStyleTags().add("casual_smart");
            metadata.getOccasionTags().add("daily");
            metadata.getSeasonTags().add("summer");
            metadata.getWhyBuyTags().add("breathable");
            metadata.setVersatilityScore(Math.max(metadata.getVersatilityScore(), 8));
        }
        if (containsAny(haystack, "oxford")) {
            metadata.getStyleTags().add("office");
            metadata.getWhyBuyTags().add("safe_choice");
            metadata.getVibeTags().add("clean");
            metadata.setVersatilityScore(Math.max(metadata.getVersatilityScore(), 8));
        }
        if (containsAny(haystack, "oversize", "oversized")) {
            metadata.setFitType("oversized");
            metadata.getFitTags().add("oversized");
            metadata.getVibeTags().add("relaxed");
            metadata.setFashionRisk("medium");
            metadata.setStylingDifficulty("medium");
        }
        if (containsAny(haystack, "jacquard", "hoa tiet", "pattern", "soc")) {
            metadata.getStyleTags().add("statement");
            metadata.getOccasionTags().add("party_light");
            metadata.setFashionRisk("medium");
            metadata.setStylingDifficulty("medium");
        }
        if (containsAny(haystack, "basic", "regular", "tron")) {
            metadata.getWhyBuyTags().add("easy_to_match");
            metadata.getWhyBuyTags().add("safe_choice");
            metadata.setStylingDifficulty("easy");
            metadata.setFashionRisk("safe");
            metadata.setVersatilityScore(Math.max(metadata.getVersatilityScore(), 9));
        }
        if (containsAny(haystack, "dang ten", "phoi")) {
            metadata.getStyleTags().add("feminine_detail");
            metadata.getVibeTags().add("soft");
            metadata.getWhyBuyTags().add("looks_premium");
        }
    }

    private void deriveWhyBuyTags(ProductMetadataProfile metadata, String haystack) {
        if (metadata.getVersatilityScore() >= 8) {
            metadata.getWhyBuyTags().add("easy_to_match");
        }
        if ("safe".equals(metadata.getFashionRisk())) {
            metadata.getWhyBuyTags().add("safe_choice");
        }
        if (containsAny(haystack, "office", "so mi", "blazer", "trouser")) {
            metadata.getWhyBuyTags().add("looks_polished");
        }
        if (metadata.getOccasionTags().contains("date")) {
            metadata.getWhyBuyTags().add("easy_to_impress");
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
}
