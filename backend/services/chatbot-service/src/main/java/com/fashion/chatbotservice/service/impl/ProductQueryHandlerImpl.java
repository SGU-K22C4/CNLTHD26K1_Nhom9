package com.fashion.chatbotservice.service.impl;

import com.fashion.chatbotservice.agent.FashionTools;
import com.fashion.chatbotservice.agent.ResponseAssembler;
import com.fashion.chatbotservice.agent.ToolResultCollector;
import com.fashion.chatbotservice.dto.ChatResponse;
import com.fashion.chatbotservice.model.ChatSession;
import com.fashion.chatbotservice.service.IntentClassifierService;
import com.fashion.chatbotservice.service.ProductQueryHandler;
import com.fashion.chatbotservice.util.VietnameseNormalizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Keeps product-search branching in one place so refinement, strict lookup and
 * relaxed fallback behave consistently between agent fallback and direct flows.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProductQueryHandlerImpl implements ProductQueryHandler {

    private static final Set<String> GENERIC_DESCRIPTORS = Set.of(
            "trang", "den", "do", "xanh", "hong", "vang", "nau", "be", "kem", "xam", "ghi", "bac",
            "navy", "gray", "black", "white", "red", "blue", "pink",
            "nam", "nu", "unisex", "cotton", "kaki", "jean", "denim",
            "tay", "dai", "short", "oversize", "form", "slim", "regular"
    );

    private final FashionTools fashionTools;

    @Override
    public boolean shouldHandleDirectSearch(String message, ChatSession.PreferenceProfile profile) {
        String normalized = VietnameseNormalizer.normalize(message == null ? "" : message);
        if (normalized.isBlank()) {
            return false;
        }

        if (isCreativeOrContentRequest(normalized)) {
            return false;
        }

        if (shouldBrowseProducts(message)
                || isGenericBudgetOnlyFollowUp(message)
                || isBroadDiscoveryRequest(normalized)
                || isBestsellerIntent(normalized)
                || hasStrongCategoryOverride(normalized)) {
            return true;
        }

        String extracted = extractProductSearchKeyword(message);
        return extracted != null
                && !extracted.isBlank()
                && !isRefinementFollowUp(message, normalized)
                && !isExplicitProductCheck(message)
                && !isGenericGarmentKeyword(extracted);
    }

    @Override
    public ChatResponse handleExplicitLookup(String sessionId,
                                             String message,
                                             ChatSession session,
                                             ToolResultCollector collector) {
        String normalized = VietnameseNormalizer.normalize(message == null ? "" : message);
        if (isRefinementFollowUp(message, normalized)
                && session != null
                && session.getPreferenceProfile() != null
                && session.getPreferenceProfile().getLastProductCategoryQueried() != null
                && !session.getPreferenceProfile().getLastProductCategoryQueried().isBlank()) {
            return null;
        }

        if (!isExplicitProductCheck(message)) {
            return null;
        }

        String searchKeyword = extractProductSearchKeyword(message);
        if (searchKeyword.isBlank()) {
            return ChatResponse.builder()
                    .sessionId(sessionId)
                    .intent(IntentClassifierService.SEARCH_PRODUCT)
                    .confidence(0.84d)
                    .reply("Bạn cho mình biết rõ tên/mẫu sản phẩm cần kiểm tra nhé. Ví dụ: 'Áo sơ mi Oxford'.")
                    .suggestions(List.of())
                    .promotions(List.of())
                    .profile(session.getPreferenceProfile())
                    .createdAt(Instant.now())
                    .build();
        }

        if (isGenericGarmentKeyword(searchKeyword)) {
            return null;
        }

        Double[] priceRange = parsePriceRangeFromMessage(message);
        Double minPrice = priceRange[0];
        Double maxPrice = priceRange[1];
        boolean wantsSimilar = wantsSimilarSuggestion(message);

        try {
            fashionTools.setCollector(collector);
            fashionTools.setPreferenceProfile(session.getPreferenceProfile());
            fashionTools.setCurrentUserId(session.getUserId());

            String strictResult = fashionTools.searchProductsStrict(
                    searchKeyword,
                    minPrice != null ? minPrice.longValue() : null,
                    maxPrice != null ? maxPrice.longValue() : null,
                    null,
                    extractSizeFilter(message));

            if (!collector.getProducts().isEmpty()) {
                return ChatResponse.builder()
                        .sessionId(sessionId)
                        .intent(IntentClassifierService.SEARCH_PRODUCT)
                        .confidence(0.92d)
                        .reply(strictResult)
                        .suggestions(collector.getProducts())
                        .promotions(List.of())
                        .profile(session.getPreferenceProfile())
                        .createdAt(Instant.now())
                        .build();
            }
        } catch (Exception ex) {
            log.warn("Strict product lookup failed: {}", ex.getMessage());
        } finally {
            fashionTools.clearCollector();
        }

        String priceNote = formatPriceRange(minPrice, maxPrice);
        String notFoundReply = priceNote.isBlank()
                ? "Hiện chưa có sản phẩm \"" + searchKeyword + "\" trong hệ thống."
                : "Hiện chưa có sản phẩm \"" + searchKeyword + "\" trong khoảng giá " + priceNote + ".";

        if (!wantsSimilar) {
            return ChatResponse.builder()
                    .sessionId(sessionId)
                    .intent(IntentClassifierService.SEARCH_PRODUCT)
                    .confidence(0.86d)
                    .reply(notFoundReply + " Bạn muốn mình gợi ý mẫu tương tự không ạ?")
                    .suggestions(List.of())
                    .promotions(List.of())
                    .profile(session.getPreferenceProfile())
                    .createdAt(Instant.now())
                    .build();
        }

        String relaxedKeyword = extractGarmentKeyword(message);
        if (relaxedKeyword == null || relaxedKeyword.isBlank()) {
            relaxedKeyword = searchKeyword;
        }

        ToolResultCollector relaxedCollector = new ToolResultCollector();
        try {
            fashionTools.setCollector(relaxedCollector);
            fashionTools.setPreferenceProfile(session.getPreferenceProfile());
            fashionTools.setCurrentUserId(session.getUserId());
            fashionTools.searchProducts(relaxedKeyword, null, null, null, extractSizeFilter(message));
        } catch (Exception ex) {
            log.warn("Relaxed product lookup failed: {}", ex.getMessage());
        } finally {
            fashionTools.clearCollector();
        }

        if (!relaxedCollector.getProducts().isEmpty()) {
            collector.addProducts(relaxedCollector.getProducts());
            return ChatResponse.builder()
                    .sessionId(sessionId)
                    .intent(IntentClassifierService.SEARCH_PRODUCT)
                    .confidence(0.88d)
                    .reply(notFoundReply + " Mình gợi ý một số mẫu tương tự để bạn tham khảo nhé:")
                    .suggestions(relaxedCollector.getProducts())
                    .promotions(List.of())
                    .profile(session.getPreferenceProfile())
                    .createdAt(Instant.now())
                    .build();
        }

        return ChatResponse.builder()
                .sessionId(sessionId)
                .intent(IntentClassifierService.SEARCH_PRODUCT)
                .confidence(0.84d)
                .reply(notFoundReply + " Bạn muốn mình tìm theo loại đồ khác không ạ?")
                .suggestions(List.of())
                .promotions(List.of())
                .profile(session.getPreferenceProfile())
                .createdAt(Instant.now())
                .build();
    }

    @Override
    public ChatResponse searchWithContext(String sessionId,
                                          String message,
                                          ChatSession session,
                                          ToolResultCollector collector) {
        ChatSession.PreferenceProfile profile = session.getPreferenceProfile();
        String normalized = VietnameseNormalizer.normalize(message == null ? "" : message);
        String searchKeyword = resolveSearchKeywordForTurn(message, profile);

        try {
            if (shouldListProductTypes(message, searchKeyword)) {
                fashionTools.setCollector(collector);
                fashionTools.setPreferenceProfile(profile);
                fashionTools.setCurrentUserId(session.getUserId());
                String typeResult = fashionTools.listProductTypes(deriveProductTypeGroupHint(searchKeyword, message));
                return ResponseAssembler.build(sessionId, typeResult, collector, profile);
            }

            Double[] priceRange = parsePriceRangeFromMessage(message);
            Double minPrice = priceRange[0];
            Double maxPrice = priceRange[1];
            if (maxPrice == null) {
                maxPrice = parseBudget(profile != null ? profile.getBudget() : null);
            }
            boolean hasPriceFilter = minPrice != null || maxPrice != null;

            fashionTools.setCollector(collector);
            fashionTools.setPreferenceProfile(profile);
            fashionTools.setCurrentUserId(session.getUserId());

            String sizeFilter = extractSizeFilter(message);
            String colorFilter = extractColorFilterForTurn(message, profile);
            boolean browseRequest = shouldBrowseProducts(message)
                    || isGenericBudgetOnlyFollowUp(message)
                    || isBroadDiscoveryRequest(normalized)
                    || isBestsellerIntent(normalized);

            String reply;
            if (browseRequest) {
                reply = fashionTools.browseProducts(
                        minPrice != null ? minPrice.longValue() : null,
                        maxPrice != null ? maxPrice.longValue() : null,
                        colorFilter,
                        sizeFilter);
                if (isBestsellerIntent(normalized) && !collector.getProducts().isEmpty()) {
                    reply = "Mình gom trước vài mẫu đang dễ chốt và nổi bật nhất để bạn xem nhanh:\n\n"
                            + reply;
                } else if (isGenericBudgetOnlyFollowUp(message) && !collector.getProducts().isEmpty()) {
                    reply = "Mình lọc nhanh các mẫu đang nằm trong khoảng ngân sách bạn đưa ra:\n\n"
                            + reply;
                }
                return ResponseAssembler.build(sessionId, reply, collector, profile);
            }

            reply = fashionTools.searchProducts(
                    searchKeyword,
                    minPrice != null ? minPrice.longValue() : null,
                    maxPrice != null ? maxPrice.longValue() : null,
                    colorFilter,
                    sizeFilter);

            if (collector.getProducts().isEmpty() && hasPriceFilter) {
                log.info("No products found with price filter [{}-{}], retrying without price for keyword: {}",
                        minPrice, maxPrice, searchKeyword);
                fashionTools.clearCollector();
                fashionTools.setCollector(collector);
                fashionTools.setPreferenceProfile(profile);
                fashionTools.setCurrentUserId(session.getUserId());
                fashionTools.searchProducts(searchKeyword, null, null, colorFilter, sizeFilter);

                if (!collector.getProducts().isEmpty()) {
                    String priceNote = formatPriceRange(minPrice, maxPrice);
                    reply = "Hiện chưa có sản phẩm \"" + searchKeyword + "\" trong khoảng giá " + priceNote
                            + ". Tuy nhiên, mình tìm thấy một số mẫu gần nhất cho bạn tham khảo:";
                    return ResponseAssembler.build(sessionId, reply, collector, profile);
                }
            }

            if (collector.getProducts().isEmpty()) {
                reply = "Mình chưa tìm thấy sản phẩm nào khớp với \"" + searchKeyword
                        + "\". Bạn thử tìm với từ khóa ngắn hơn (VD: 'áo thun', 'quần jean', 'váy') hoặc cho mình biết thêm chi tiết nhé!";
            }
            return ResponseAssembler.build(sessionId, reply, collector, profile);
        } catch (Exception ex) {
            log.warn("Heuristic product search failed: {}", ex.getMessage());
            return ChatResponse.builder()
                    .sessionId(sessionId)
                    .intent(IntentClassifierService.SEARCH_PRODUCT)
                    .confidence(0.72d)
                    .reply("Mình chưa thể tìm sản phẩm lúc này. Bạn thử lại sau nhé!")
                    .suggestions(collector.getProducts())
                    .promotions(collector.getPromotions())
                    .profile(profile)
                    .createdAt(Instant.now())
                    .build();
        } finally {
            fashionTools.clearCollector();
        }
    }

    @Override
    public ChatResponse refreshForGenderContext(String sessionId,
                                                String searchKeyword,
                                                ChatSession session) {
        ToolResultCollector collector = new ToolResultCollector();
        try {
            fashionTools.setCollector(collector);
            fashionTools.setPreferenceProfile(session.getPreferenceProfile());
            fashionTools.setCurrentUserId(session.getUserId());
            String colorFilter = extractColorFilterForTurn(searchKeyword, session.getPreferenceProfile());
            String reply = fashionTools.searchProducts(searchKeyword, null, null, colorFilter, null);
            return ResponseAssembler.build(sessionId, reply, collector, session.getPreferenceProfile());
        } catch (Exception ex) {
            log.warn("Gender-context refresh failed: {}", ex.getMessage());
            return ChatResponse.builder()
                    .sessionId(sessionId)
                    .intent(IntentClassifierService.SEARCH_PRODUCT)
                    .confidence(0.82d)
                    .reply("Ok, mình sẽ lọc lại theo đồ "
                            + humanizeGender(session.getPreferenceProfile().getTargetGender())
                            + " cho bạn ở lượt tiếp theo nhé.")
                    .suggestions(List.of())
                    .promotions(List.of())
                    .profile(session.getPreferenceProfile())
                    .createdAt(Instant.now())
                    .build();
        } finally {
            fashionTools.clearCollector();
        }
    }

    private boolean shouldListProductTypes(String message, String searchKeyword) {
        String normalized = VietnameseNormalizer.normalize(message == null ? "" : message);
        if (searchKeyword == null || searchKeyword.isBlank()) {
            return false;
        }
        return normalized.contains("co loai nao")
                || normalized.contains("nhung loai nao")
                || normalized.contains("gom nhung loai")
                || normalized.contains("co mau nao")
                || (isGenericGarmentKeyword(searchKeyword) && !isRefinementFollowUp(message, normalized));
    }

    private boolean shouldBrowseProducts(String message) {
        String normalized = VietnameseNormalizer.normalize(message == null ? "" : message);
        return normalized.contains("co gi dep")
                || normalized.contains("xem them")
                || normalized.contains("goi y cho minh")
                || normalized.contains("goi y cho toi")
                || normalized.contains("goi y cho em")
                || normalized.contains("co gi phu hop")
                || normalized.contains("list san pham")
                || normalized.contains("goi y san pham")
                || normalized.contains("xem san pham")
                || normalized.contains("san pham nao")
                || normalized.contains("best seller")
                || normalized.contains("ban chay")
                || normalized.contains("nhieu luot ban nhat")
                || normalized.contains("bestseller");
    }

    private String deriveProductTypeGroupHint(String searchKeyword, String message) {
        String garment = extractGarmentKeyword(message);
        if (!garment.isBlank()) {
            return garment;
        }
        return searchKeyword == null ? "" : searchKeyword;
    }

    private String resolveSearchKeywordForTurn(String message, ChatSession.PreferenceProfile profile) {
        String normalized = VietnameseNormalizer.normalize(message == null ? "" : message);
        if (shouldResetPreviousCategoryContext(normalized, message)) {
            String extractedOverride = extractProductSearchKeyword(message);
            return extractedOverride == null ? "" : extractedOverride;
        }
        if (isRefinementFollowUp(message, normalized)
                && profile != null
                && profile.getLastProductCategoryQueried() != null
                && !profile.getLastProductCategoryQueried().isBlank()) {
            return profile.getLastProductCategoryQueried();
        }
        String extracted = extractProductSearchKeyword(message);
        if (extracted != null && !extracted.isBlank() && !isGenericBudgetOnlyFollowUp(message)) {
            return extracted;
        }
        if (profile != null && profile.getLastProductCategoryQueried() != null && !profile.getLastProductCategoryQueried().isBlank()) {
            return profile.getLastProductCategoryQueried();
        }
        return extracted;
    }

    private boolean shouldResetPreviousCategoryContext(String normalized, String message) {
        return normalized.contains("list san pham")
                || normalized.contains("goi y san pham")
                || normalized.contains("xem san pham")
                || normalized.contains("best seller")
                || normalized.contains("ban chay")
                || normalized.contains("nhieu luot ban nhat")
                || isGenericBudgetOnlyFollowUp(message)
                || isBroadDiscoveryRequest(normalized);
    }

    private boolean isBroadDiscoveryRequest(String normalized) {
        return containsAnyKeyword(normalized,
                "goi y cho toi", "goi y cho minh", "goi y cho em",
                "xem giup minh", "tu van giup minh",
                "co mau nao dep", "co gi phu hop",
                "co gi de mac", "chon giup minh");
    }

    private boolean isBestsellerIntent(String normalized) {
        return containsAnyKeyword(normalized,
                "best seller", "bestseller", "ban chay",
                "nhieu luot ban nhat", "ban chay nhat", "hot nhat");
    }

    private boolean hasStrongCategoryOverride(String normalized) {
        return containsAnyKeyword(normalized,
                "ao so mi", "ao khoac", "ao thun", "chan vay", "dam",
                "vay", "quan jean", "quan tay", "blazer");
    }

    private String extractColorFilterForTurn(String message, ChatSession.PreferenceProfile profile) {
        if (message == null || message.isBlank()) {
            return null;
        }
        String normalized = VietnameseNormalizer.normalize(message);
        if (normalized.contains("mau den") || normalized.contains("den khong") || normalized.contains("tone den")) return "đen";
        if (normalized.contains("mau trang") || normalized.contains("trang khong") || normalized.contains("tone trang")) return "trắng";
        if (normalized.contains("mau xanh") || normalized.contains("xanh khong") || normalized.contains("tone xanh")) return "xanh";
        if (normalized.contains("mau do") || normalized.contains("do khong") || normalized.contains("tone do")) return "đỏ";
        if (normalized.contains("mau hong") || normalized.contains("hong khong")) return "hồng";
        if (normalized.contains("mau vang") || normalized.contains("vang khong")) return "vàng";
        if (normalized.contains("mau nau") || normalized.contains("nau khong")) return "nâu";
        if (normalized.contains("mau be") || normalized.contains("be khong")) return "be";
        if (normalized.contains("mau xam") || normalized.contains("xam khong") || normalized.contains("ghi khong")) return "xám";
        if (normalized.contains("mau navy") || normalized.contains("navy khong")) return "navy";
        if (normalized.contains("mau kem") || normalized.contains("kem khong")) return "kem";

        if (isRefinementFollowUp(message, normalized)
                && profile != null
                && profile.getPreferredColors() != null
                && !profile.getPreferredColors().isEmpty()) {
            return profile.getPreferredColors().iterator().next();
        }
        return null;
    }

    private boolean isRefinementFollowUp(String message, String normalizedMessage) {
        if (message == null || message.isBlank()) {
            return false;
        }
        boolean budgetFollowUp = isGenericBudgetOnlyFollowUp(message);
        boolean sizeFollowUp = extractSizeFilter(message) != null;
        boolean fitFollowUp = isSizeFitFollowUp(normalizedMessage);
        boolean colorFollowUp = containsAnyKeyword(normalizedMessage,
                "den", "trang", "xanh", "do", "hong", "vang", "nau", "be", "xam", "ghi", "navy", "kem");
        boolean quickChoiceFollowUp = containsAnyKeyword(normalizedMessage,
                "mau nay", "mau kia", "loai nay", "loai kia", "cai nay", "cai kia",
                "phuong an nay", "phuong an kia", "vay thi", "the con", "neu vay", "ok vay");
        return budgetFollowUp || sizeFollowUp || fitFollowUp || colorFollowUp || quickChoiceFollowUp;
    }

    private boolean isSizeFitFollowUp(String normalizedMessage) {
        if (normalizedMessage == null || normalizedMessage.isBlank()) {
            return false;
        }
        return normalizedMessage.matches(".*\\b(xs|s|m|l|xl|xxl)\\s*(hay|hoac|vs|voi)\\s*(xs|s|m|l|xl|xxl)\\b.*")
                || containsAnyKeyword(normalizedMessage,
                "phan van size", "size nao hop", "size nao on", "size nao dep",
                "om hay rong", "fit nao hop", "vai rong", "nguc day", "mong to", "dui to",
                "mac rong", "mac om", "vua van", "thoai mai hon");
    }

    private boolean isExplicitProductCheck(String message) {
        if (message == null || message.isBlank()) return false;
        String normalized = VietnameseNormalizer.normalize(message);
        String searchKeyword = extractProductSearchKeyword(message);

        boolean hasCheckPhrase = normalized.contains("co khong")
                || normalized.contains("con khong")
                || normalized.contains("co hang")
                || normalized.contains("con hang")
                || normalized.contains("ton kho")
                || normalized.contains("het hang")
                || normalized.contains("co ban");

        boolean hasSpecificCue = normalized.contains("san pham ")
                || normalized.contains("sp ")
                || normalized.contains("ten ")
                || normalized.contains("ma ");

        boolean hasQuote = message.contains("\"") || message.contains("'");
        boolean hasConcreteKeyword = searchKeyword != null
                && !searchKeyword.isBlank()
                && !isGenericGarmentKeyword(searchKeyword);
        boolean attributeOnlyRefinement = isAttributeOnlyRefinement(message, normalized);

        if (hasQuote) {
            return true;
        }
        if (attributeOnlyRefinement) {
            return false;
        }

        return hasCheckPhrase && hasConcreteKeyword
                && (hasSpecificCue || !isRefinementFollowUp(message, normalized));
    }

    private boolean isAttributeOnlyRefinement(String message, String normalized) {
        if (message == null || message.isBlank()) {
            return false;
        }
        boolean hasColor = extractColorFilterForTurn(message, null) != null;
        boolean hasSize = extractSizeFilter(message) != null;
        boolean hasBudget = hasPriceSignal(normalized);
        boolean hasConcreteGarment = !extractGarmentKeyword(message).isBlank();
        boolean refinementFollowUp = isRefinementFollowUp(message, normalized);

        if (!refinementFollowUp) {
            return false;
        }
        if (hasColor || hasSize || hasBudget) {
            return !hasConcreteGarment || isGenericGarmentKeyword(extractProductSearchKeyword(message));
        }
        return false;
    }

    private boolean wantsSimilarSuggestion(String message) {
        if (message == null || message.isBlank()) return false;
        String normalized = VietnameseNormalizer.normalize(message);
        return normalized.contains("tuong tu")
                || normalized.contains("gan giong")
                || normalized.contains("thay the")
                || normalized.contains("goi y")
                || normalized.contains("mau khac")
                || normalized.contains("san pham khac");
    }

    private boolean isGenericGarmentKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) return false;
        String normalized = VietnameseNormalizer.normalize(keyword);
        String[] baseGarments = {
                "ao", "ao thun", "ao so mi", "ao khoac", "ao polo", "ao hoodie", "ao len",
                "quan", "quan jean", "quan tay", "quan short", "quan dai",
                "vay", "dam", "chan vay", "ao dai", "giay", "tui", "non"
        };

        for (String garment : baseGarments) {
            if (normalized.equals(garment)) return true;
            if (normalized.startsWith(garment + " ")) {
                String rest = normalized.substring(garment.length()).trim();
                if (rest.isBlank()) return true;
                String[] tokens = rest.split("\\s+");
                boolean descriptorOnly = true;
                for (String token : tokens) {
                    if (!GENERIC_DESCRIPTORS.contains(token)) {
                        descriptorOnly = false;
                        break;
                    }
                }
                if (descriptorOnly) return true;
            }
        }
        return false;
    }

    private String formatPriceRange(Double minPrice, Double maxPrice) {
        if (minPrice == null && maxPrice == null) {
            return "";
        }
        if (minPrice == null) {
            return "dưới " + formatMoney(maxPrice.longValue());
        }
        if (maxPrice == null) {
            return "từ " + formatMoney(minPrice.longValue());
        }
        return "từ " + formatMoney(minPrice.longValue()) + " đến " + formatMoney(maxPrice.longValue());
    }

    private String formatMoney(long amount) {
        if (amount % 1_000_000 == 0) {
            return (amount / 1_000_000) + " triệu";
        }
        if (amount >= 1_000_000) {
            double million = amount / 1_000_000d;
            return String.format(java.util.Locale.US, "%.1f triệu", million);
        }
        if (amount % 1_000 == 0) {
            return (amount / 1_000) + "k";
        }
        return String.valueOf(amount);
    }

    private Double parseBudget(String budgetText) {
        if (budgetText == null || budgetText.isBlank()) return null;
        String normalized = VietnameseNormalizer.normalize(budgetText).replace(",", ".");
        Matcher compactMillion = Pattern.compile("(\\d+)\\s*tr\\s*(\\d{1,3})\\b").matcher(normalized);
        if (compactMillion.find()) {
            return parseCompactMillion(compactMillion.group(1), compactMillion.group(2));
        }
        Matcher million = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*(tr|trieu)").matcher(normalized);
        if (million.find()) return Double.parseDouble(million.group(1)) * 1_000_000d;
        Matcher thousand = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*k").matcher(normalized);
        if (thousand.find()) return Double.parseDouble(thousand.group(1)) * 1_000d;
        Matcher plain = Pattern.compile("(\\d{5,})").matcher(normalized);
        if (plain.find()) return Double.parseDouble(plain.group(1));
        return null;
    }

    private Double[] parsePriceRangeFromMessage(String message) {
        if (message == null || message.isBlank()) return new Double[]{null, null};
        String normalized = VietnameseNormalizer.normalize(message).replace(",", ".");

        Matcher compactRange = Pattern.compile("(\\d+\\s*tr\\s*\\d{1,3}|\\d+(?:\\.\\d+)?\\s*(?:k|tr|trieu)?)\\s*(den|toi)\\s*(\\d+\\s*tr\\s*\\d{1,3}|\\d+(?:\\.\\d+)?\\s*(?:k|tr|trieu)?)").matcher(normalized);
        if (compactRange.find()) {
            double first = parseAmountToken(compactRange.group(1));
            double second = parseAmountToken(compactRange.group(3));
            return new Double[]{Math.min(first, second), Math.max(first, second)};
        }

        Matcher range = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*(k|tr|trieu)?\\s*(den|toi)\\s*(\\d+(?:\\.\\d+)?)\\s*(k|tr|trieu)?").matcher(normalized);
        if (range.find()) {
            double first = toVnd(range.group(1), range.group(2));
            double second = toVnd(range.group(4), range.group(5));
            return new Double[]{Math.min(first, second), Math.max(first, second)};
        }

        Matcher under = Pattern.compile("(duoi|toi da|khong qua)\\s*(\\d+(?:\\.\\d+)?)\\s*(k|tr|trieu)?").matcher(normalized);
        if (under.find()) {
            return new Double[]{null, toVnd(under.group(2), under.group(3))};
        }

        Matcher over = Pattern.compile("(tren|tu)\\s*(\\d+(?:\\.\\d+)?)\\s*(k|tr|trieu)?").matcher(normalized);
        if (over.find()) {
            return new Double[]{toVnd(over.group(2), over.group(3)), null};
        }
        return new Double[]{null, null};
    }

    private double parseAmountToken(String token) {
        String normalized = VietnameseNormalizer.normalize(token == null ? "" : token).replace(",", ".").trim();
        Matcher compactMillion = Pattern.compile("(\\d+)\\s*tr\\s*(\\d{1,3})\\b").matcher(normalized);
        if (compactMillion.matches()) {
            return parseCompactMillion(compactMillion.group(1), compactMillion.group(2));
        }

        Matcher million = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*(tr|trieu)\\b").matcher(normalized);
        if (million.matches()) {
            return Double.parseDouble(million.group(1)) * 1_000_000d;
        }

        Matcher thousand = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*k\\b").matcher(normalized);
        if (thousand.matches()) {
            return Double.parseDouble(thousand.group(1)) * 1_000d;
        }

        Matcher plain = Pattern.compile("(\\d+(?:\\.\\d+)?)").matcher(normalized);
        if (plain.matches()) {
            double value = Double.parseDouble(plain.group(1));
            return value >= 10_000 ? value : value * 1_000d;
        }
        return 0d;
    }

    private double parseCompactMillion(String millionPart, String suffixPart) {
        String suffix = suffixPart == null ? "" : suffixPart.trim();
        if (suffix.isBlank()) {
            return Double.parseDouble(millionPart) * 1_000_000d;
        }
        String decimalSuffix = suffix.length() == 1 ? "0." + suffix : "0." + suffix;
        return (Double.parseDouble(millionPart) + Double.parseDouble(decimalSuffix)) * 1_000_000d;
    }

    private double toVnd(String number, String unit) {
        double value = Double.parseDouble(number);
        if (unit == null || unit.isBlank()) {
            return value >= 10_000 ? value : value * 1_000d;
        }
        String normalizedUnit = VietnameseNormalizer.normalize(unit);
        if (normalizedUnit.startsWith("tr")) {
            return value * 1_000_000d;
        }
        return value * 1_000d;
    }

    private boolean isGenericBudgetOnlyFollowUp(String message) {
        if (message == null || message.isBlank()) {
            return false;
        }
        String normalized = VietnameseNormalizer.normalize(message);
        boolean hasPrice = hasPriceSignal(normalized);
        boolean hasGarment = !extractGarmentKeyword(message).isBlank();
        boolean hasColor = containsAnyKeyword(normalized,
                "den", "trang", "xanh", "do", "hong", "vang", "nau", "be", "xam", "ghi");
        boolean hasSize = extractSizeFilter(message) != null;
        return hasPrice && !hasGarment && !hasColor && !hasSize;
    }

    private boolean hasPriceSignal(String normalizedMessage) {
        return normalizedMessage != null
                && (normalizedMessage.contains("duoi")
                || normalizedMessage.contains("tren")
                || normalizedMessage.contains("toi da")
                || normalizedMessage.contains("khong qua")
                || normalizedMessage.contains("k")
                || normalizedMessage.contains("tr")
                || normalizedMessage.contains("trieu"));
    }

    private String extractSizeFilter(String message) {
        if (message == null || message.isBlank()) return null;
        Matcher matcher = Pattern.compile("\\b(xs|s|m|l|xl|xxl)\\b", Pattern.CASE_INSENSITIVE).matcher(message);
        if (matcher.find()) {
            return matcher.group(1).toUpperCase(java.util.Locale.ROOT);
        }
        return null;
    }

    private String extractGarmentKeyword(String message) {
        String normalized = VietnameseNormalizer.normalize(message == null ? "" : message);
        for (String garment : List.of(
                "ao so mi", "ao thun", "ao polo", "ao hoodie", "ao len", "ao khoac",
                "quan jean", "quan tay", "quan short", "quan chino", "chan vay", "vay", "dam", "blazer")) {
            if (normalized.contains(garment)) {
                return garment;
            }
        }
        return "";
    }

    private String extractProductSearchKeyword(String message) {
        if (message == null || message.isBlank()) {
            return "";
        }
        String normalized = VietnameseNormalizer.normalize(message)
                .replaceAll("[^\\p{L}\\p{Nd}\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim();

        String garment = extractGarmentKeyword(message);
        if (!garment.isBlank()) {
            return garment;
        }

        String titleCandidate = normalized
                .replaceAll("\\b(review|danh gia|thong tin|chi tiet|san pham|mau|khuyen mai|chuong trinh|gia bao nhieu|list)\\b", " ")
                .replaceAll("\\b(co|nao|the nao|cho toi|cho minh|cho em|giup|minh|toi|em|vay thi)\\b", " ")
                .replaceAll("\\b\\d+(?:tr|trieu|k)\\b", " ")
                .replaceAll("\\s+", " ")
                .trim();
        if (titleCandidate.split("\\s+").length >= 3) {
            return titleCandidate;
        }

        return normalized;
    }

    private String humanizeGender(String gender) {
        String normalized = VietnameseNormalizer.normalize(gender == null ? "" : gender);
        if ("male".equals(normalized)) return "nam";
        if ("female".equals(normalized)) return "nữ";
        return "phù hợp";
    }

    private boolean isCreativeOrContentRequest(String normalized) {
        return containsAnyKeyword(normalized,
                "viet bai", "viet post", "viet content", "viet stt", "viet status", "viet caption", "viet cap",
                "viet quang cao", "viet gioi thieu", "viet tho", "viet van", "viet pr", "viet facebook", "viet fb",
                "bai viet facebook", "bai dang facebook", "post facebook", "status facebook", "stt facebook",
                "dang facebook", "dang fb", "dang stt", "dang status", "dang caption",
                "gioi thieu hai huoc", "quang cao hai huoc", "stt hai huoc", "status hai huoc", "caption hai huoc",
                "gioi thieu hom hinh", "quang cao hom hinh", "stt hom hinh", "status hom hinh", "caption hom hinh",
                "viet doan gioi thieu", "viet doan quang cao", "viet status", "viet caption", "viet stt",
                "viet ho", "viet giup", "viet gium", "dang len facebook", "dang len fb"
        );
    }

    private boolean containsAnyKeyword(String normalizedMessage, String... keywords) {
        if (normalizedMessage == null || normalizedMessage.isBlank()) {
            return false;
        }
        for (String keyword : keywords) {
            if (normalizedMessage.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
}
