package com.fashion.chatbotservice.conversation.impl;

import com.fashion.chatbotservice.conversation.RecommendationReadiness;
import com.fashion.chatbotservice.conversation.SlotFillingService;
import com.fashion.chatbotservice.conversation.StylingSlots;
import com.fashion.chatbotservice.model.ChatSession;
import com.fashion.chatbotservice.util.VietnameseNormalizer;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class SlotFillingServiceImpl implements SlotFillingService {

    private static final Pattern HEIGHT_WEIGHT_PATTERN = Pattern.compile(
            "(?:(?:cao|height)\\s*(\\d{2,3})(?:cm|\\s)?)?.*?(?:(?:nang|weight)\\s*(\\d{2,3})(?:kg)?)?",
            Pattern.CASE_INSENSITIVE);

    @Override
    public void mergeSlots(ChatSession.PreferenceProfile profile, String message) {
        if (profile == null || message == null || message.isBlank()) {
            return;
        }

        StylingSlots current = profile.getStylingSlots() != null
                ? profile.getStylingSlots().toBuilder().build()
                : StylingSlots.empty();
        String normalized = VietnameseNormalizer.normalize(message);

        fillGender(profile, current);
        fillOccasion(normalized, current);
        fillStyleVibe(normalized, current);
        fillProductType(normalized, current);
        fillBudget(profile, current);
        fillSize(profile, normalized, current);
        fillColor(normalized, current);
        fillMeasurements(profile, normalized, current);
        fillFitPreference(profile, normalized, current);

        profile.setStylingSlots(current);
        profile.setSlotConfidence(estimateConfidence(profile));
    }

    @Override
    public List<String> findMissingPrioritySlots(ChatSession.PreferenceProfile profile) {
        List<String> missing = new ArrayList<>();
        StylingSlots slots = safeSlots(profile);
        if (isBlank(slots.getOccasion())) {
            missing.add("occasion");
        }
        if (isBlank(slots.getProductType())) {
            missing.add("productType");
        }
        if (isBlank(slots.getStyleVibe())) {
            missing.add("styleVibe");
        }
        return missing;
    }

    @Override
    public double estimateConfidence(ChatSession.PreferenceProfile profile) {
        StylingSlots slots = safeSlots(profile);
        int score = 0;
        if (!isBlank(slots.getOccasion())) score++;
        if (!isBlank(slots.getProductType())) score++;
        if (!isBlank(slots.getStyleVibe())) score++;
        if (!isBlank(slots.getBudget())) score++;
        if (!isBlank(slots.getTargetGender())) score++;
        return Math.min(1.0d, score / 5.0d);
    }

    @Override
    public RecommendationReadiness evaluateReadiness(ChatSession.PreferenceProfile profile) {
        List<String> missingSlots = findMissingPrioritySlots(profile);
        int filledCoreSlots = Math.max(0, 3 - missingSlots.size());
        boolean hasEnoughCoreContext = filledCoreSlots >= 2;
        boolean hasSpecificProductSignal = !isBlank(safeSlots(profile).getProductType());
        return RecommendationReadiness.builder()
                .ready(hasEnoughCoreContext && hasSpecificProductSignal)
                .filledCoreSlots(filledCoreSlots)
                .missingPrioritySlots(missingSlots)
                .build();
    }

    private void fillGender(ChatSession.PreferenceProfile profile, StylingSlots slots) {
        if (isBlank(slots.getTargetGender()) && !isBlank(profile.getTargetGender())) {
            slots.setTargetGender(profile.getTargetGender());
        }
    }

    private void fillOccasion(String normalized, StylingSlots slots) {
        if (!isBlank(slots.getOccasion())) {
            return;
        }
        if (containsAny(normalized, "di lam", "office", "cong so")) {
            slots.setOccasion("office");
        } else if (containsAny(normalized, "di date", "hen ho", "toi nay")) {
            slots.setOccasion("date");
        } else if (containsAny(normalized, "du lich", "travel")) {
            slots.setOccasion("travel");
        } else if (containsAny(normalized, "di choi", "hang ngay", "daily", "casual")) {
            slots.setOccasion("daily");
        } else if (containsAny(normalized, "du tiec", "wedding", "su kien")) {
            slots.setOccasion("event");
        }
    }

    private void fillStyleVibe(String normalized, StylingSlots slots) {
        if (!isBlank(slots.getStyleVibe())) {
            return;
        }
        if (containsAny(normalized, "toi gian", "minimal", "clean")) {
            slots.setStyleVibe("minimal");
        } else if (containsAny(normalized, "tre trung", "youthful")) {
            slots.setStyleVibe("youthful");
        } else if (containsAny(normalized, "lich su", "truong thanh", "formal")) {
            slots.setStyleVibe("smart");
        } else if (containsAny(normalized, "noi bat", "trendy", "ca tinh")) {
            slots.setStyleVibe("statement");
        }
    }

    private void fillProductType(String normalized, StylingSlots slots) {
        if (!isBlank(slots.getProductType())) {
            return;
        }
        if (containsAny(normalized, "ao so mi", "so mi")) {
            slots.setProductType("ao so mi");
        } else if (containsAny(normalized, "ao thun", "t-shirt", "tee")) {
            slots.setProductType("ao thun");
        } else if (containsAny(normalized, "ao khoac", "blazer", "jacket")) {
            slots.setProductType("ao khoac");
        } else if (containsAny(normalized, "quan jean", "jean")) {
            slots.setProductType("quan jean");
        } else if (containsAny(normalized, "quan tay", "quan au")) {
            slots.setProductType("quan tay");
        } else if (containsAny(normalized, "chan vay")) {
            slots.setProductType("chan vay");
        } else if (containsAny(normalized, "dam", "vay")) {
            slots.setProductType("dam");
        } else if (containsAny(normalized, "set do", "outfit", "combo")) {
            slots.setProductType("set do");
        }
    }

    private void fillBudget(ChatSession.PreferenceProfile profile, StylingSlots slots) {
        if (isBlank(slots.getBudget()) && !isBlank(profile.getBudget())) {
            slots.setBudget(profile.getBudget());
        }
    }

    private void fillSize(ChatSession.PreferenceProfile profile, String normalized, StylingSlots slots) {
        if (!isBlank(slots.getSize())) {
            return;
        }
        Matcher matcher = Pattern.compile("\\b(xs|s|m|l|xl|xxl)\\b", Pattern.CASE_INSENSITIVE).matcher(normalized);
        if (matcher.find()) {
            slots.setSize(matcher.group(1).toUpperCase());
            return;
        }
        if (profile.getPreferredSizes() != null && !profile.getPreferredSizes().isEmpty()) {
            slots.setSize(profile.getPreferredSizes().iterator().next());
        }
    }

    private void fillColor(String normalized, StylingSlots slots) {
        if (!isBlank(slots.getColorPreference())) {
            return;
        }
        if (containsAny(normalized, "den", "black")) {
            slots.setColorPreference("den");
        } else if (containsAny(normalized, "trang", "white")) {
            slots.setColorPreference("trang");
        } else if (containsAny(normalized, "be", "kem")) {
            slots.setColorPreference("be");
        } else if (containsAny(normalized, "xanh")) {
            slots.setColorPreference("xanh");
        }
    }

    private void fillMeasurements(ChatSession.PreferenceProfile profile, String normalized, StylingSlots slots) {
        Matcher matcher = HEIGHT_WEIGHT_PATTERN.matcher(normalized);
        if (matcher.find()) {
            if (slots.getHeightCm() == null && matcher.group(1) != null) {
                slots.setHeightCm(parseIntSafely(matcher.group(1)));
            }
            if (slots.getWeightKg() == null && matcher.group(2) != null) {
                slots.setWeightKg(parseIntSafely(matcher.group(2)));
            }
        }
        if (slots.getHeightCm() == null) {
            slots.setHeightCm(profile.getLastHeightCm());
        }
        if (slots.getWeightKg() == null) {
            slots.setWeightKg(profile.getLastWeightKg());
        }
    }

    private void fillFitPreference(ChatSession.PreferenceProfile profile, String normalized, StylingSlots slots) {
        if (!isBlank(slots.getFitPreference())) {
            return;
        }
        if (containsAny(normalized, "oversize", "rong")) {
            slots.setFitPreference("oversized");
        } else if (containsAny(normalized, "vua nguoi", "regular")) {
            slots.setFitPreference("regular");
        } else if (containsAny(normalized, "om", "slim")) {
            slots.setFitPreference("slim");
        } else if (!isBlank(profile.getFitPreference())) {
            slots.setFitPreference(profile.getFitPreference());
        }
    }

    private StylingSlots safeSlots(ChatSession.PreferenceProfile profile) {
        return profile != null && profile.getStylingSlots() != null
                ? profile.getStylingSlots()
                : StylingSlots.empty();
    }

    private boolean containsAny(String text, String... patterns) {
        for (String pattern : patterns) {
            if (text.contains(pattern)) {
                return true;
            }
        }
        return false;
    }

    private Integer parseIntSafely(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
