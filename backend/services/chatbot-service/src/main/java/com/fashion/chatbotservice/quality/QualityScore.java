package com.fashion.chatbotservice.quality;

import lombok.Builder;
import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * DTO chứa kết quả chấm điểm chất lượng response — Phase 2C.
 */
@Data
@Builder
public class QualityScore {

    /** Điểm tổng (0–100). */
    private int total;

    /** Chi tiết điểm theo từng tiêu chí. */
    @Builder.Default
    private Map<String, Integer> breakdown = new LinkedHashMap<>();

    /** Danh sách cảnh báo khi phát hiện vấn đề. */
    @Builder.Default
    private java.util.List<String> warnings = new java.util.ArrayList<>();

    public void addPoint(String criterion, int points) {
        breakdown.merge(criterion, points, Integer::sum);
    }

    public void addWarning(String warning) {
        warnings.add(warning);
    }

    /** Tính tổng điểm từ breakdown. */
    public void calculate() {
        this.total = breakdown.values().stream().mapToInt(Integer::intValue).sum();
        this.total = Math.max(0, Math.min(100, this.total));
    }

    public boolean isLowQuality() {
        return total < 60;
    }

    public boolean isAcceptable() {
        return total >= 60 && total < 75;
    }

    public boolean isGood() {
        return total >= 75;
    }
}
