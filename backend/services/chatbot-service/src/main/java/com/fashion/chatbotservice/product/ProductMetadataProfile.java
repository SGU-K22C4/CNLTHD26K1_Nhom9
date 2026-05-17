package com.fashion.chatbotservice.product;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.LinkedHashSet;
import java.util.Set;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class ProductMetadataProfile {

    private String productId;
    private String productName;
    private String category;
    private String fitType;
    private String fashionRisk;
    private String stylingDifficulty;

    @Builder.Default
    private Set<String> styleTags = new LinkedHashSet<>();

    @Builder.Default
    private Set<String> occasionTags = new LinkedHashSet<>();

    @Builder.Default
    private Set<String> fitTags = new LinkedHashSet<>();

    @Builder.Default
    private Set<String> bodyShapeTags = new LinkedHashSet<>();

    @Builder.Default
    private Set<String> vibeTags = new LinkedHashSet<>();

    @Builder.Default
    private Set<String> seasonTags = new LinkedHashSet<>();

    @Builder.Default
    private Set<String> whyBuyTags = new LinkedHashSet<>();

    private int versatilityScore;

    public static ProductMetadataProfile empty() {
        return ProductMetadataProfile.builder().build();
    }
}
