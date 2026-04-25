package com.fashion.chatbotservice.service;

import java.util.List;

/**
 * Domain service: tính toán size thời trang dựa trên số đo cơ thể.
 */
public interface SizeAdvisorService {

    record Measurements(Integer heightCm, Integer weightKg, Integer chestCm, Integer waistCm, Integer hipCm) {
        public List<String> missingFields() {
            List<String> missing = new java.util.ArrayList<>();
            if (heightCm == null) missing.add("chiều cao (cm)");
            if (weightKg == null) missing.add("cân nặng (kg)");
            if (chestCm == null) missing.add("vòng ngực (cm)");
            if (waistCm == null) missing.add("vòng eo (cm)");
            if (hipCm == null) missing.add("vòng hông (cm)");
            return missing;
        }

        public boolean hasMinimumData() {
            return heightCm != null && weightKg != null;
        }
    }

    record SizeResult(String recommendedSize, String note) {}

    enum GarmentType { TOP, BOTTOM }

    /**
     * Trích xuất số đo từ tin nhắn tiếng Việt.
     */
    Measurements extractMeasurements(String message);

    /**
     * Gợi ý size dựa trên số đo cơ thể.
     */
    SizeResult suggest(Measurements measurements, GarmentType garmentType);

    /**
     * Nhận diện loại trang phục (áo / quần) từ tin nhắn.
     */
    GarmentType detectGarmentType(String message);
}
