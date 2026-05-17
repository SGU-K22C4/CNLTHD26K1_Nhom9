package com.fashion.chatbotservice.sales.impl;

import com.fashion.chatbotservice.dto.ChatResponse;
import com.fashion.chatbotservice.sales.CompareEngine;
import com.fashion.chatbotservice.util.VietnameseNormalizer;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
public class CompareEngineImpl implements CompareEngine {

    @Override
    public CompareResult compare(List<ChatResponse.ProductSuggestion> suggestions) {
        if (suggestions == null || suggestions.isEmpty()) {
            return new CompareResult(null, null, null, null, "", "", "", "", "");
        }

        ChatResponse.ProductSuggestion safer = suggestions.stream()
                .max(Comparator.comparingInt(this::safetyScore))
                .orElse(null);
        ChatResponse.ProductSuggestion stylish = suggestions.stream()
                .max(Comparator.comparingInt(this::styleScore))
                .orElse(safer);
        ChatResponse.ProductSuggestion easy = suggestions.stream()
                .max(Comparator.comparingInt(this::easyStyleScore))
                .orElse(safer);
        ChatResponse.ProductSuggestion value = suggestions.stream()
                .max(Comparator.comparingInt(this::valueScore))
                .orElse(safer);

        String recommendation = safer == null
                ? ""
                : "Nếu bạn muốn chọn nhanh và an toàn, mình nghiêng về **" + safer.getName()
                + "** vì mẫu này dễ mặc và dễ phối hơn trong nhóm hiện tại.";

        return new CompareResult(
                safer,
                stylish,
                easy,
                value,
                buildSaferReason(safer),
                buildStylishReason(stylish),
                buildValueReason(value),
                buildEasyStyleReason(easy),
                recommendation);
    }

    private String buildSaferReason(ChatResponse.ProductSuggestion suggestion) {
        if (suggestion == null) {
            return "";
        }
        return "Mẫu này an toàn hơn vì form dễ mặc và ít rủi ro phối đồ hơn.";
    }

    private String buildStylishReason(ChatResponse.ProductSuggestion suggestion) {
        if (suggestion == null) {
            return "";
        }
        return "Mẫu này nổi bật hơn vì có điểm nhấn thời trang rõ hơn trong nhóm hiện tại.";
    }

    private String buildValueReason(ChatResponse.ProductSuggestion suggestion) {
        if (suggestion == null) {
            return "";
        }
        return "Mẫu này đáng tiền hơn nếu bạn muốn cân bằng giữa giá và độ dễ ứng dụng.";
    }

    private String buildEasyStyleReason(ChatResponse.ProductSuggestion suggestion) {
        if (suggestion == null) {
            return "";
        }
        return "Mẫu này dễ phối hơn nếu bạn muốn mặc được nhiều dịp mà không cần nghĩ nhiều.";
    }

    private int safetyScore(ChatResponse.ProductSuggestion suggestion) {
        int score = baseScore(suggestion);
        String normalized = normalizedName(suggestion);
        if (normalized.contains("basic") || normalized.contains("regular") || normalized.contains("linen")) score += 2;
        if (normalized.contains("soc") || normalized.contains("cut out") || normalized.contains("tay phong")) score -= 1;
        return score;
    }

    private int styleScore(ChatResponse.ProductSuggestion suggestion) {
        int score = baseScore(suggestion);
        String normalized = normalizedName(suggestion);
        if (normalized.contains("dang ten") || normalized.contains("cropped") || normalized.contains("phoi")) score += 2;
        if (normalized.contains("basic")) score -= 1;
        return score;
    }

    private int easyStyleScore(ChatResponse.ProductSuggestion suggestion) {
        int score = baseScore(suggestion);
        String normalized = normalizedName(suggestion);
        if (normalized.contains("trang") || normalized.contains("den") || normalized.contains("be")) score += 2;
        if (normalized.contains("soc")) score -= 1;
        return score;
    }

    private int valueScore(ChatResponse.ProductSuggestion suggestion) {
        int score = baseScore(suggestion);
        long price = parsePrice(suggestion.getPrice());
        if (price > 0 && price <= 1_200_000L) score += 2;
        if (price > 1_800_000L) score -= 1;
        return score;
    }

    private int baseScore(ChatResponse.ProductSuggestion suggestion) {
        int score = 0;
        if (suggestion.getAvailableSizes() != null && !suggestion.getAvailableSizes().isEmpty()) {
            score += 2;
        }
        if (suggestion.getAvailableColors() != null && !suggestion.getAvailableColors().isEmpty()) {
            score += 1;
        }
        return score;
    }

    private String normalizedName(ChatResponse.ProductSuggestion suggestion) {
        return VietnameseNormalizer.normalize(
                (suggestion.getName() == null ? "" : suggestion.getName()) + " "
                        + (suggestion.getCategory() == null ? "" : suggestion.getCategory()))
                .toLowerCase();
    }

    private long parsePrice(String price) {
        if (price == null) {
            return -1;
        }
        String digits = price.replaceAll("[^0-9]", "");
        if (digits.isBlank()) {
            return -1;
        }
        try {
            return Long.parseLong(digits);
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }
}
