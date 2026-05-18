package com.fashion.chatbotservice.query;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * DTO chứa kết quả phân tích (enriched) của một query từ user.
 * Được tạo ra bởi {@link com.fashion.chatbotservice.query.impl.QueryUnderstandingServiceImpl}.
 *
 * <p>Phase 2B — Query Understanding Layer.
 */
@Data
@Builder
public class EnrichedQuery {

    /** Loại sản phẩm được đề cập (áo thun, quần jean...). */
    @Builder.Default
    private List<String> products = List.of();

    /** Màu sắc được đề cập (đen, trắng, navy...). */
    @Builder.Default
    private List<String> colors = List.of();

    /** Size được đề cập (S, M, L, XL...). */
    @Builder.Default
    private List<String> sizes = List.of();

    /** Ngân sách tối đa (VND). */
    private Double maxBudget;

    /** Ngân sách tối thiểu (VND). */
    private Double minBudget;

    /** Dịp mặc (đi làm, đi tiệc, du lịch...). */
    private String occasion;

    /** Query đã normalize (tiếng Việt không dấu). */
    private String normalizedQuery;

    /**
     * True nếu đây là refinement query (dựa trên context trước đó),
     * ví dụ: "màu đen không?" sau khi đã hỏi về áo.
     */
    @Builder.Default
    private boolean isRefinement = false;

    /** True nếu user đang so sánh hoặc phân vân giữa các sản phẩm. */
    @Builder.Default
    private boolean isComparison = false;

    /**
     * Có đủ thông tin để search ngay không (không cần hỏi thêm).
     */
    public boolean hasEnoughForDirectSearch() {
        return (!products.isEmpty() || occasion != null)
                && !normalizedQuery.isBlank();
    }
}
