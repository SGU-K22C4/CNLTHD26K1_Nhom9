package com.fashion.chatbotservice.service.impl;

import com.fashion.chatbotservice.agent.FashionTools;
import com.fashion.chatbotservice.agent.ToolResultCollector;
import com.fashion.chatbotservice.dto.ChatResponse;
import com.fashion.chatbotservice.model.ChatSession;
import com.fashion.chatbotservice.service.MultiIntentResolver;
import com.fashion.chatbotservice.util.VietnameseNormalizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class MultiIntentResolverImpl implements MultiIntentResolver {

    private final FashionTools fashionTools;

    @Override
    public ChatResponse resolve(String sessionId,
                                String message,
                                ChatSession session,
                                ToolResultCollector collector) {
        if (session == null || session.getPreferenceProfile() == null) {
            return null;
        }

        String normalized = VietnameseNormalizer.normalize(message == null ? "" : message);
        boolean asksReview = asksReview(normalized);
        boolean asksPromotion = asksPromotion(normalized);
        boolean asksDetail = asksDetail(normalized);

        if (!asksReview && !asksPromotion && !asksDetail) {
            return null;
        }

        ChatSession.SelectedProductContextSnapshot selected = resolveSelectedProductContext(message, session, collector);
        if (selected == null || selected.getProductId() == null || selected.getProductId().isBlank()) {
            return null;
        }
        session.getPreferenceProfile().setSelectedProductContext(selected);

        try {
            fashionTools.setCollector(collector);
            fashionTools.setPreferenceProfile(session.getPreferenceProfile());
            fashionTools.setCurrentUserId(session.getUserId());

            StringBuilder reply = new StringBuilder();
            String displayName = selected.getProductName() == null || selected.getProductName().isBlank()
                    ? "mẫu bạn vừa chọn"
                    : selected.getProductName();

            reply.append("Mình tổng hợp nhanh cho ").append(displayName).append(" nhé:");

            if (asksDetail) {
                reply.append("\n\n")
                        .append(fashionTools.getProductDetail(selected.getProductId()));
            }

            if (asksReview) {
                reply.append("\n\n")
                        .append(fashionTools.getProductReviews(selected.getProductId()));
            }

            if (asksPromotion) {
                reply.append("\n\n")
                        .append(fashionTools.getActivePromotions());
            }

            if (collector.getProducts().isEmpty()) {
                collector.addProducts(List.of(snapshotToSuggestion(selected)));
            }

            return ChatResponse.builder()
                    .sessionId(sessionId)
                    .intent("PRODUCT_FOLLOW_UP")
                    .confidence(0.94d)
                    .reply(reply.toString().trim())
                    .suggestions(collector.getProducts())
                    .promotions(collector.getPromotions())
                    .profile(session.getPreferenceProfile())
                    .createdAt(Instant.now())
                    .build();
        } catch (Exception ex) {
            log.warn("Multi-intent product follow-up failed: {}", ex.getMessage());
            return ChatResponse.builder()
                    .sessionId(sessionId)
                    .intent("PRODUCT_FOLLOW_UP")
                    .confidence(0.76d)
                    .reply("Mình chưa tổng hợp trọn vẹn thông tin cho mẫu bạn vừa chọn lúc này. Bạn thử hỏi lại từng ý như review, chi tiết hoặc khuyến mãi nhé.")
                    .suggestions(collector.getProducts())
                    .promotions(collector.getPromotions())
                    .profile(session.getPreferenceProfile())
                    .createdAt(Instant.now())
                    .build();
        } finally {
            fashionTools.clearCollector();
        }
    }

    private ChatSession.SelectedProductContextSnapshot resolveSelectedProductContext(String message,
                                                                                     ChatSession session,
                                                                                     ToolResultCollector collector) {
        ChatSession.SelectedProductContextSnapshot selected = session.getPreferenceProfile().getSelectedProductContext();
        if (selected != null && selected.getProductId() != null && !selected.getProductId().isBlank()) {
            return selected;
        }

        String titleHint = extractTitleHint(message);
        if (titleHint.isBlank()) {
            return null;
        }

        try {
            fashionTools.setCollector(collector);
            fashionTools.setPreferenceProfile(session.getPreferenceProfile());
            fashionTools.setCurrentUserId(session.getUserId());

            fashionTools.searchProductsStrict(titleHint, null, null, null, null);
            if (collector.getProducts().isEmpty()) {
                fashionTools.searchProducts(titleHint, null, null, null, null);
            }
        } catch (Exception ex) {
            log.warn("Fuzzy title grounding failed: {}", ex.getMessage());
        } finally {
            fashionTools.clearCollector();
        }

        if (collector.getProducts().isEmpty()) {
            return null;
        }

        ChatResponse.ProductSuggestion first = selectBestMatchingSuggestion(collector.getProducts(), titleHint);
        if (first.getProductId() == null || first.getProductId().isBlank()) {
            return null;
        }

        return ChatSession.SelectedProductContextSnapshot.builder()
                .productId(first.getProductId())
                .productName(first.getName())
                .category(first.getCategory())
                .categoryGender(first.getCategoryGender())
                .price(first.getPrice())
                .link(first.getLink())
                .selectedAt(Instant.now())
                .build();
    }

    private boolean asksReview(String normalized) {
        return containsAny(normalized,
                "review", "danh gia", "rating", "nhan xet", "bao nhieu sao", "co tot khong");
    }

    private boolean asksPromotion(String normalized) {
        return containsAny(normalized,
                "khuyen mai", "uu dai", "giam gia", "voucher", "coupon", "ma giam");
    }

    private boolean asksDetail(String normalized) {
        return containsAny(normalized,
                "chi tiet", "thong tin", "chat lieu", "mo ta", "con size", "mau gi", "size nao", "gia bao nhieu");
    }

    private boolean containsAny(String normalized, String... tokens) {
        if (normalized == null || normalized.isBlank()) {
            return false;
        }
        for (String token : tokens) {
            if (normalized.contains(token)) {
                return true;
            }
        }
        return false;
    }

    private String extractTitleHint(String message) {
        String normalized = VietnameseNormalizer.normalize(message == null ? "" : message)
                .replaceAll("[^\\p{L}\\p{Nd}\\s]", " ")
                .replaceAll("\\b(review|danh gia|thong tin|chi tiet|khuyen mai|chuong trinh|uu dai|voucher|coupon|ma giam|san pham|mau)\\b", " ")
                .replaceAll("\\b(the nao|nhu the nao|hien tai|luon|giup|minh|toi|em|vay thi|co)\\b", " ")
                .replaceAll("\\b\\d+(?:tr|trieu|k)\\b", " ")
                .replaceAll("\\s+", " ")
                .trim();
        if (normalized.split("\\s+").length < 3) {
            return "";
        }
        return normalized;
    }

    private ChatResponse.ProductSuggestion selectBestMatchingSuggestion(List<ChatResponse.ProductSuggestion> suggestions,
                                                                        String titleHint) {
        String normalizedHint = VietnameseNormalizer.normalize(titleHint == null ? "" : titleHint);
        return suggestions.stream()
                .max(Comparator.comparingInt(suggestion -> scoreTitleMatch(suggestion, normalizedHint)))
                .orElse(suggestions.get(0));
    }

    private int scoreTitleMatch(ChatResponse.ProductSuggestion suggestion, String normalizedHint) {
        String haystack = VietnameseNormalizer.normalize(
                (suggestion.getName() == null ? "" : suggestion.getName()) + " "
                        + (suggestion.getCategory() == null ? "" : suggestion.getCategory()));
        int score = 0;
        for (String token : normalizedHint.split("\\s+")) {
            if (token.length() < 2) {
                continue;
            }
            if (haystack.contains(token)) {
                score += token.length() >= 4 ? 3 : 1;
            }
        }
        if (haystack.contains(normalizedHint) && !normalizedHint.isBlank()) {
            score += 6;
        }
        return score;
    }

    private ChatResponse.ProductSuggestion snapshotToSuggestion(ChatSession.SelectedProductContextSnapshot selected) {
        return ChatResponse.ProductSuggestion.builder()
                .productId(selected.getProductId())
                .name(selected.getProductName())
                .category(selected.getCategory())
                .categoryGender(selected.getCategoryGender())
                .link(selected.getLink())
                .price(selected.getPrice())
                .reason("Sản phẩm người dùng vừa chọn")
                .build();
    }
}
