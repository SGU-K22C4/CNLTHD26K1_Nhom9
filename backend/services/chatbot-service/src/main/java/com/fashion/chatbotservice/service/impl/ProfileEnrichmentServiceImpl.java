package com.fashion.chatbotservice.service.impl;

import com.fashion.chatbotservice.model.ChatSession;
import com.fashion.chatbotservice.model.UserPreferenceDocument;
import com.fashion.chatbotservice.repository.UserPreferenceRepository;
import com.fashion.chatbotservice.service.ProfileEnrichmentService;
import com.fashion.chatbotservice.util.VietnameseNormalizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Enriches user preference profile from chat messages and purchase history.
 * Runs before agent processing to provide context.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProfileEnrichmentServiceImpl implements ProfileEnrichmentService {

    private static final Pattern SIZE_PATTERN = Pattern.compile(
            "size\\s*[:=]?\\s*(xs|s|m|l|xl|xxl|\\d{2})", Pattern.CASE_INSENSITIVE);

    private final WebClient webClient;
    private final UserPreferenceRepository userPreferenceRepository;

    @Value("${chatbot.order-service-url:http://localhost:8080}")
    private String orderServiceUrl;

    @Value("${chatbot.product-service-url:http://localhost:8080}")
    private String productServiceUrl;

    @Value("${chatbot.user-service-url:http://localhost:8080}")
    private String userServiceUrl;

    @Override
    public void enrichFromMessage(ChatSession.PreferenceProfile profile, String message) {
        if (profile == null || message == null) return;

        String normalized = VietnameseNormalizer.normalize(message);

        extractSizePreference(profile, normalized);
        extractColorPreference(profile, normalized);
        extractCategoryPreference(profile, normalized);
        extractTonePreference(profile, normalized);
        extractFocusTags(profile, normalized);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void enrichFromPurchaseHistory(ChatSession.PreferenceProfile profile, String userId) {
        if (profile == null || userId == null || userId.startsWith("guest-")) return;

        // Skip nếu profile đã có dữ liệu purchase history → tránh gọi HTTP mỗi message
        if (!profile.getPreferredSizes().isEmpty() && !profile.getPreferredColors().isEmpty()) return;

        try {
            Map<String, Object> pageData = webClient.get()
                    .uri(orderServiceUrl + "/api/v1/orders?page=0&size=20")
                    .headers(headers -> headers.add("X-User-Id", userId))
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (pageData == null) return;

            Object content = pageData.get("content");
            if (!(content instanceof List<?> orders)) return;

            for (Object orderObj : orders) {
                if (!(orderObj instanceof Map<?, ?> order)) continue;
                Object items = order.get("items");
                if (!(items instanceof List<?> itemList)) continue;

                for (Object itemObj : itemList) {
                    if (!(itemObj instanceof Map<?, ?> item)) continue;

                    String size = stringValue(item.get("size"));
                    if (!size.isBlank()) {
                        profile.getPreferredSizes().add(size.toUpperCase(Locale.ROOT));
                    }

                    String color = stringValue(item.get("color"));
                    if (!color.isBlank()) {
                        profile.getPreferredColors().add(color);
                    }
                }
            }
        } catch (Exception ex) {
            log.warn("Unable to hydrate profile from order history: {}", ex.getMessage());
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void enrichFromWishlist(ChatSession.PreferenceProfile profile, String userId) {
        if (profile == null || userId == null || userId.startsWith("guest-")) return;

        if (!profile.getPreferredCategories().isEmpty()
                && !profile.getPreferredColors().isEmpty()
                && !profile.getPreferredSizes().isEmpty()) {
            return;
        }

        try {
            Map<String, Object> pageData = webClient.get()
                    .uri(productServiceUrl + "/api/v1/wishlists?page=0&size=20")
                    .headers(headers -> headers.add("X-User-Id", userId))
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (pageData == null) return;

            Object content = pageData.get("content");
            if (!(content instanceof List<?> products)) return;

            for (Object productObj : products) {
                if (!(productObj instanceof Map<?, ?> product)) continue;
                hydrateFromProductPayload(profile, product);
            }
        } catch (Exception ex) {
            log.warn("Unable to hydrate profile from wishlist: {}", ex.getMessage());
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void enrichFromUserProfile(ChatSession.PreferenceProfile profile, String userId) {
        if (profile == null || userId == null || userId.startsWith("guest-")) return;

        try {
            Map<String, Object> payload = webClient.get()
                    .uri(userServiceUrl + "/api/v1/users/me")
                    .headers(headers -> headers.add("X-User-Id", userId))
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (payload == null) return;

            String firstName = stringValue(payload.get("firstName"));
            String lastName = stringValue(payload.get("lastName"));
            if (!firstName.isBlank() || !lastName.isBlank()) {
                profile.setPreferredTone("Professional");
            }
        } catch (Exception ex) {
            log.debug("User profile enrichment skipped: {}", ex.getMessage());
        }
    }

    private void extractSizePreference(ChatSession.PreferenceProfile profile, String normalized) {
        Matcher sizeMatcher = SIZE_PATTERN.matcher(normalized);
        if (sizeMatcher.find()) {
            profile.getPreferredSizes().add(sizeMatcher.group(1).toUpperCase(Locale.ROOT));
        }
    }

    private void extractColorPreference(ChatSession.PreferenceProfile profile, String normalized) {
        if (normalized.contains("xanh") || normalized.contains("blue")) profile.getPreferredColors().add("xanh");
        if (normalized.contains("den") || normalized.contains("black")) profile.getPreferredColors().add("đen");
        if (normalized.contains("trang") || normalized.contains("white")) profile.getPreferredColors().add("trắng");
        if (normalized.contains("do") || normalized.contains("red")) profile.getPreferredColors().add("đỏ");
        if (normalized.contains("hong") || normalized.contains("pink")) profile.getPreferredColors().add("hồng");
    }

    private void extractCategoryPreference(ChatSession.PreferenceProfile profile, String normalized) {
        if (normalized.contains("ao")) profile.getPreferredCategories().add("áo");
        if (normalized.contains("quan")) profile.getPreferredCategories().add("quần");
        if (normalized.contains("vay")) profile.getPreferredCategories().add("váy");
        if (normalized.contains("dam")) profile.getPreferredCategories().add("đầm");
    }

    private void extractTonePreference(ChatSession.PreferenceProfile profile, String normalized) {
        if (normalized.contains("than thien") || normalized.contains("friendly")) {
            profile.setPreferredTone("Friendly");
        }
        if (normalized.contains("chuyen nghiep") || normalized.contains("professional")) {
            profile.setPreferredTone("Professional");
        }
    }

    private void extractFocusTags(ChatSession.PreferenceProfile profile, String normalized) {
        if (normalized.contains("sustain") || normalized.contains("ben vung")) {
            profile.getFocusTags().add("Sustainability");
        }
        if (normalized.contains("fit") || normalized.contains("silhouette")) {
            profile.getFocusTags().add("Silhouette & Fit");
        }
    }

    @SuppressWarnings("unchecked")
    private void hydrateFromProductPayload(ChatSession.PreferenceProfile profile, Map<?, ?> product) {
        String categoryName = stringValue(product.get("categoryName"));
        if (!categoryName.isBlank()) {
            profile.getPreferredCategories().add(categoryName);
        }

        Object variants = product.get("variants");
        if (!(variants instanceof List<?> variantList)) return;

        for (Object variantObj : variantList) {
            if (!(variantObj instanceof Map<?, ?> variant)) continue;
            String color = stringValue(variant.get("colorName"));
            if (!color.isBlank()) {
                profile.getPreferredColors().add(color);
            }
            Object sizes = variant.get("sizes");
            if (!(sizes instanceof List<?> sizeList)) continue;
            for (Object sizeObj : sizeList) {
                if (!(sizeObj instanceof Map<?, ?> sizeItem)) continue;
                String sizeName = stringValue(sizeItem.get("sizeName"));
                if (!sizeName.isBlank()) {
                    profile.getPreferredSizes().add(sizeName.toUpperCase(Locale.ROOT));
                }
            }
        }
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    // ========== LONG-TERM PROFILE PERSISTENCE (Task 4) ==========

    @Override
    public void persistProfileAsync(String userId, ChatSession.PreferenceProfile profile) {
        if (userId == null || userId.startsWith("guest-") || profile == null) return;

        CompletableFuture.runAsync(() -> {
            try {
                UserPreferenceDocument doc = userPreferenceRepository.findByUserId(userId)
                        .orElse(UserPreferenceDocument.builder()
                                .userId(userId)
                                .build());

                doc.setProfile(profile);
                doc.setLastUpdatedAt(Instant.now());
                userPreferenceRepository.save(doc);

                log.debug("Persisted preference profile for user {}", userId);
            } catch (Exception ex) {
                log.warn("Failed to persist profile for user {}: {}", userId, ex.getMessage());
            }
        });
    }

    @Override
    public ChatSession.PreferenceProfile loadPersistedProfile(String userId) {
        if (userId == null || userId.startsWith("guest-")) {
            return ChatSession.PreferenceProfile.empty();
        }

        try {
            return userPreferenceRepository.findByUserId(userId)
                    .map(UserPreferenceDocument::getProfile)
                    .orElse(ChatSession.PreferenceProfile.empty());
        } catch (Exception ex) {
            log.warn("Failed to load persisted profile for user {}: {}", userId, ex.getMessage());
            return ChatSession.PreferenceProfile.empty();
        }
    }
}
