package com.fashion.chatbotservice.styling.impl;

import com.fashion.chatbotservice.model.ChatSession;
import com.fashion.chatbotservice.product.ProductMetadataProfile;
import com.fashion.chatbotservice.styling.OccasionAdvice;
import com.fashion.chatbotservice.styling.OccasionAdvisorService;
import com.fashion.chatbotservice.util.VietnameseNormalizer;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class OccasionAdvisorServiceImpl implements OccasionAdvisorService {

    @Override
    public OccasionAdvice advise(String search, ChatSession.PreferenceProfile profile, ProductMetadataProfile metadata) {
        String occasion = inferOccasion(search, profile);
        if (occasion == null || occasion.isBlank()) {
            return OccasionAdvice.empty();
        }

        List<String> avoid = new ArrayList<>();
        String direction;
        String colorGuidance;
        String closingAngle;

        // Occasion rules stay in Java so the bot can remain stylist-like even on
        // heuristic/direct flows without depending on prompt quality.
        switch (occasion) {
            case "work" -> {
                direction = "clean, gọn và đủ lịch sự để đi làm";
                colorGuidance = "Ưu tiên trắng, đen, be, navy hoặc tông trung tính dễ phối.";
                closingAngle = "Nếu bạn muốn an toàn để mặc đi làm nhiều lần, mình sẽ nghiêng về option sạch và ít rủi ro hơn.";
                avoid.add("Tránh chi tiết quá rườm rà hoặc quá casual.");
            }
            case "date" -> {
                direction = "đẹp, dễ tạo thiện cảm nhưng không quá gồng";
                colorGuidance = "Nên chọn tông mềm hoặc sáng vừa phải để nhìn gần gũi hơn.";
                closingAngle = "Nếu bạn muốn chốt nhanh cho buổi hẹn, mình sẽ ưu tiên option dễ gây thiện cảm hơn.";
                avoid.add("Tránh outfit quá cứng hoặc quá formal.");
            }
            case "daily" -> {
                direction = "dễ mặc, dễ giặt và dễ phối nhiều lần";
                colorGuidance = "Tông trung tính hoặc màu dễ phối sẽ thực dụng hơn.";
                closingAngle = "Nếu bạn cần mặc hằng ngày, mình sẽ nghiêng về mẫu versatile và ít lỗi mốt.";
                avoid.add("Tránh item quá khó phối cho nhu cầu mặc thường xuyên.");
            }
            case "travel" -> {
                direction = "thoải mái, ít nhăn và dễ di chuyển";
                colorGuidance = "Nên ưu tiên màu bền, ít lộ nhăn và dễ mix nhiều set.";
                closingAngle = "Nếu mang đi du lịch, mình sẽ ưu tiên mẫu dễ pack và linh hoạt hơn.";
                avoid.add("Tránh form quá bó hoặc chất dễ nhăn.");
            }
            default -> {
                direction = "chỉn chu vừa đủ và có điểm nhấn nhẹ";
                colorGuidance = "Ưu tiên tông sạch, lên ảnh ổn nhưng không quá phô.";
                closingAngle = "Nếu cần mặc cho dịp đặc biệt, mình sẽ chọn mẫu vừa an toàn vừa đủ nổi bật.";
                avoid.add("Tránh outfit quá casual.");
            }
        }

        if (metadata != null && metadata.getOccasionTags() != null && metadata.getOccasionTags().contains(occasion)) {
            direction = direction + ", và item này đang khớp đúng dịp mặc đó.";
        }

        return OccasionAdvice.builder()
                .occasion(occasion)
                .recommendedDirection(direction)
                .colorGuidance(colorGuidance)
                .closingAngle(closingAngle)
                .avoid(avoid)
                .build();
    }

    private String inferOccasion(String search, ChatSession.PreferenceProfile profile) {
        String normalizedSearch = VietnameseNormalizer.normalize(search == null ? "" : search);
        if (containsAny(normalizedSearch, "di lam", "office", "cong so")) {
            return "work";
        }
        if (containsAny(normalizedSearch, "di date", "hen ho", "toi nay")) {
            return "date";
        }
        if (containsAny(normalizedSearch, "hang ngay", "daily", "casual", "di choi")) {
            return "daily";
        }
        if (containsAny(normalizedSearch, "travel", "du lich")) {
            return "travel";
        }
        if (containsAny(normalizedSearch, "party", "du tiec", "wedding", "su kien")) {
            return "party_light";
        }

        if (profile != null && profile.getStylingSlots() != null && profile.getStylingSlots().getOccasion() != null) {
            return VietnameseNormalizer.normalize(profile.getStylingSlots().getOccasion());
        }
        return null;
    }

    private boolean containsAny(String haystack, String... needles) {
        for (String needle : needles) {
            if (haystack.contains(needle)) {
                return true;
            }
        }
        return false;
    }
}
