package com.fashion.chatbotservice.service.impl;

import com.fashion.chatbotservice.service.ProductTaxonomyService;
import com.fashion.chatbotservice.util.VietnameseNormalizer;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class ProductTaxonomyServiceImpl implements ProductTaxonomyService {

    @Override
    public List<String> extractTypeLabels(String name, String category) {
        String normalized = normalizeText(stringValue(name) + " " + stringValue(category));
        LinkedHashSet<String> types = new LinkedHashSet<>();
        boolean matched = false;

        if (containsAny(normalized, "ao thun", "ao phong", "t-shirt", "tee")) {
            types.add("ao thun");
            matched = true;
        }
        if (containsAny(normalized, "ao so mi", "shirt")) {
            types.add("ao so mi");
            matched = true;
        }
        if (containsAny(normalized, "ao khoac", "jacket", "blazer", "coat")) {
            types.add("ao khoac");
            matched = true;
        }
        if (containsAny(normalized, "parka", "bomber")) {
            types.add("ao khoac");
            matched = true;
        }
        if (containsAny(normalized, "ao polo", "polo")) {
            types.add("ao polo");
            matched = true;
        }
        if (containsAny(normalized, "ao hoodie", "hoodie")) {
            types.add("ao hoodie");
            matched = true;
        }
        if (containsAny(normalized, "ao len", "sweater", "knit")) {
            types.add("ao len");
            matched = true;
        }
        if (containsAny(normalized, "ao kieu")) {
            types.add("ao kieu");
            matched = true;
        }
        if (containsAny(normalized, "ao ghi le", "ao gi le", "gilet", "vest")) {
            types.add("ao ghi le");
            matched = true;
        }
        if (containsAny(normalized, "quan jean", "jeans", "denim")) {
            types.add("quan jean");
            matched = true;
        }
        if (containsAny(normalized, "quan tay", "trouser", "slacks")) {
            types.add("quan tay");
            matched = true;
        }
        if (containsAny(normalized, "quan short", "short")) {
            types.add("quan short");
            matched = true;
        }
        if (containsAny(normalized, "quan dai")) {
            types.add("quan dai");
            matched = true;
        }
        if (containsAny(normalized, "quan vay")) {
            types.add("quan vay");
            matched = true;
        }
        if (containsAny(normalized, "chan vay", "skirt")) {
            types.add("chan vay");
            matched = true;
        }
        if (containsAny(normalized, "dam", "dress")) {
            types.add("dam");
            matched = true;
        }
        if (containsAny(normalized, "vay")) {
            types.add("vay");
            matched = true;
        }
        if (containsAny(normalized, "jumpsuit")) {
            types.add("jumpsuit");
            matched = true;
        }
        if (containsAny(normalized, "ao dai")) {
            types.add("ao dai");
            matched = true;
        }
        if (containsAny(normalized, "giay", "shoes", "sneaker", "boot")) {
            types.add("giay");
            matched = true;
        }
        if (containsAny(normalized, "tui", "bag", "handbag", "backpack")) {
            types.add("tui");
            matched = true;
        }
        if (containsAny(normalized, "non", "hat", "cap")) {
            types.add("non");
            matched = true;
        }

        if (!matched) {
            String categoryLabel = stringValue(category).trim();
            if (!categoryLabel.isBlank()) {
                types.add(categoryLabel);
            }
        }

        return new ArrayList<>(types);
    }

    @Override
    public String resolveGroupLabel(String value) {
        String normalized = normalizeText(value);
        if (normalized.contains("ao")) return "ao";
        if (normalized.contains("quan")) return "quan";
        if (normalized.contains("vay") || normalized.contains("dam") || normalized.contains("chan vay")
                || normalized.contains("jumpsuit")) {
            return "vay/dam";
        }
        if (normalized.contains("giay") || normalized.contains("tui") || normalized.contains("non")
                || normalized.contains("phu kien")) {
            return "phu kien";
        }
        return "khac";
    }

    @Override
    public String inferOccasionContext(String normalizedSearch) {
        if (normalizedSearch == null || normalizedSearch.isBlank()) {
            return "";
        }
        if (containsAny(normalizedSearch, "di lam", "cong so", "office")) return "office";
        if (containsAny(normalizedSearch, "di tiec", "su kien", "party")) return "party";
        if (containsAny(normalizedSearch, "du lich", "di choi", "hang ngay", "casual")) return "casual";
        if (containsAny(normalizedSearch, "mua he", "he")) return "summer";
        if (containsAny(normalizedSearch, "mua dong", "dong")) return "winter";
        return "";
    }

    @Override
    public Set<String> inferTaxonomyLabels(String haystack) {
        LinkedHashSet<String> labels = new LinkedHashSet<>();
        if (haystack == null || haystack.isBlank()) {
            return labels;
        }

        if (containsAny(haystack, "ao so mi", "shirt", "blazer", "trouser", "quan tay")) {
            labels.add("office");
            labels.add("safe");
        }
        if (containsAny(haystack, "ao thun", "ao phong", "jean", "short", "hoodie", "bomber")) {
            labels.add("casual");
        }
        if (containsAny(haystack, "dam", "dress", "ao kieu", "chan vay", "jacquard")) {
            labels.add("statement");
        }
        if (containsAny(haystack, "midi", "regular", "basic", "cotton", "linen", "tron")) {
            labels.add("safe");
        }
        if (containsAny(haystack, "linen", "short", "midi", "ao phong")) {
            labels.add("summer");
        }
        if (containsAny(haystack, "len", "hoodie", "ao khoac", "jacket", "coat")) {
            labels.add("winter");
        }
        if (containsAny(haystack, "ao khoac", "blazer", "dress", "dam", "chan vay")) {
            labels.add("party");
        }
        return labels;
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

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }
}
