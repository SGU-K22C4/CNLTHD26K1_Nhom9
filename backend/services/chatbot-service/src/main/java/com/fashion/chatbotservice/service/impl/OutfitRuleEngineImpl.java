package com.fashion.chatbotservice.service.impl;

import com.fashion.chatbotservice.service.OutfitRuleEngine;
import com.fashion.chatbotservice.util.VietnameseNormalizer;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Domain service thuần: quy tắc gợi ý outfit theo mùa/dịp/phong cách.
 * Không phụ thuộc LLM — chỉ chứa business rules.
 */
@Service
public class OutfitRuleEngineImpl implements OutfitRuleEngine {

    @Override
    public List<String> buildQueries(String occasion, String style) {
        LinkedHashSet<String> queries = new LinkedHashSet<>();
        String normalized = VietnameseNormalizer.normalize(occasion);

        buildSeasonQueries(normalized, queries);
        buildOccasionQueries(normalized, queries);

        if (style != null && !style.isBlank()) {
            buildStyleQueries(VietnameseNormalizer.normalize(style), queries);
        }

        if (queries.isEmpty()) {
            queries.add("áo");
            queries.add("quần");
        }

        return new ArrayList<>(queries);
    }

    @Override
    public Set<String> extractProductKeywords(String message) {
        LinkedHashSet<String> keywords = new LinkedHashSet<>();
        String normalized = VietnameseNormalizer.normalize(message);

        if (normalized.contains("ao so mi")) keywords.add("áo sơ mi");
        if (normalized.contains("ao phong") || normalized.contains("tshirt") || normalized.contains("thun")) keywords.add("áo phông");
        if (normalized.contains("polo")) keywords.add("polo");
        if (normalized.contains("ao khoac") || normalized.contains("jacket") || normalized.contains("blazer")) keywords.add("áo khoác");
        if (normalized.contains("hoodie")) keywords.add("hoodie");
        if (normalized.contains("quan jean") || normalized.contains("jeans")) keywords.add("jeans");
        if (normalized.contains("quan tay") || normalized.contains("trouser")) keywords.add("quần tây");
        if (normalized.contains("chan vay") || normalized.contains("vay")) keywords.add("chân váy");
        if (normalized.contains("dam")) keywords.add("đầm");

        return keywords;
    }

    @Override
    public boolean matchesKeywords(String productName, String productCategory, Set<String> keywords) {
        if (keywords.isEmpty()) return false;
        String combined = VietnameseNormalizer.normalize(productName + " " + productCategory);
        for (String keyword : keywords) {
            if (combined.contains(VietnameseNormalizer.normalize(keyword))) {
                return true;
            }
        }
        return false;
    }

    private void buildSeasonQueries(String normalized, Set<String> queries) {
        if (normalized.contains("he")) {
            queries.add("áo phông");
            queries.add("váy");
            queries.add("linen");
        }
        if (normalized.contains("dong")) {
            queries.add("áo khoác");
            queries.add("jeans");
            queries.add("len");
        }
        if (normalized.contains("thu")) {
            queries.add("áo sơ mi");
            queries.add("quần jeans");
        }
        if (normalized.contains("xuan")) {
            queries.add("áo sơ mi");
            queries.add("chân váy");
        }
    }

    private void buildOccasionQueries(String normalized, Set<String> queries) {
        if (normalized.contains("di lam") || normalized.contains("cong so")) {
            queries.add("áo sơ mi");
            queries.add("quần tây");
            queries.add("blazer");
            queries.add("chân váy midi");
        }
        if (normalized.contains("di tiec") || normalized.contains("su kien")) {
            queries.add("đầm");
            queries.add("áo khoác");
            queries.add("áo kiểu");
            queries.add("chân váy midi");
        }
        if (normalized.contains("du lich") || normalized.contains("di choi")) {
            queries.add("áo phông");
            queries.add("quần jeans");
            queries.add("linen");
            queries.add("áo khoác nhẹ");
        }
    }

    private void buildStyleQueries(String normalized, Set<String> queries) {
        if (normalized.contains("thanh lich") || normalized.contains("elegant")) {
            queries.add("áo sơ mi");
            queries.add("đầm");
            queries.add("blazer");
        }
        if (normalized.contains("casual") || normalized.contains("thoai mai")) {
            queries.add("áo phông");
            queries.add("jeans");
            queries.add("linen");
        }
        if (normalized.contains("sporty") || normalized.contains("the thao")) {
            queries.add("hoodie");
            queries.add("áo thun");
        }
        if (normalized.contains("minimal") || normalized.contains("basic")) {
            queries.add("áo sơ mi");
            queries.add("áo thun");
            queries.add("quần tây");
        }
    }
}
