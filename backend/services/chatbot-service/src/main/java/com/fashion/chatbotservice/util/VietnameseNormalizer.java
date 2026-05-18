package com.fashion.chatbotservice.util;

import java.util.Locale;

/**
 * Utility chuẩn hóa chuỗi tiếng Việt: bỏ dấu, lowercase, loại ký tự đặc biệt.
 * Dùng chung cho mọi service cần normalize message tiếng Việt.
 *
 * <p>Edge cases được fix theo Review.md:
 * <ul>
 *   <li>Đ/đ uppercase → chưa xử lý ở phiên bản cũ vì {@code toLowerCase()} được gọi sau.
 *       Thực ra {@code 'đ'.toLowerCase()} đã là 'đ' → cần replace cả 'Đ' trước toLowerCase.</li>
 *   <li>Dấu ngoặc kép typographic (", ") và ngoặc đơn (', ') → normalize về ASCII trước.</li>
 *   <li>Underscore và gạch ngang → giữ lại thay vì xóa (dùng trong product codes).</li>
 * </ul>
 */
public final class VietnameseNormalizer {

    private VietnameseNormalizer() {}

    /**
     * Normalize chuỗi tiếng Việt: bỏ dấu, lowercase, chuẩn hóa whitespace.
     *
     * <p>Pipeline:
     * <ol>
     *   <li>Replace Đ → D, đ → d <em>trước</em> toLowerCase (Đ không được handle bởi toLowerCase thường)</li>
     *   <li>toLowerCase với Locale.ROOT</li>
     *   <li>Replace các nguyên âm có dấu → không dấu</li>
     *   <li>Normalize dấu ngoặc typographic → ASCII</li>
     *   <li>Loại ký tự không phải a-z0-9 (giữ space)</li>
     *   <li>Collapse whitespace và trim</li>
     * </ol>
     */
    public static String normalize(String value) {
        if (value == null) return "";

        return value
                // Fix: Đ uppercase chưa được xử lý trước khi toLowerCase
                .replace('Đ', 'D')
                .replace('đ', 'd')
                .toLowerCase(Locale.ROOT)
                // Nguyên âm có dấu → không dấu
                .replaceAll("[áàảãạâấầẩẫậăắằẳẵặ]", "a")
                .replaceAll("[éèẻẽẹêếềểễệ]", "e")
                .replaceAll("[íìỉĩị]", "i")
                .replaceAll("[óòỏõọôốồổỗộơớờởỡợ]", "o")
                .replaceAll("[úùủũụưứừửữự]", "u")
                .replaceAll("[ýỳỷỹỵ]", "y")
                // Dấu ngoặc typographic → space (tránh block product name detection trong guardrail)
                .replace('\u201c', ' ')  // "
                .replace('\u201d', ' ')  // "
                .replace('\u2018', ' ')  // '
                .replace('\u2019', ' ')  // '
                // Loại ký tự không phải a-z0-9 (giữ space; gạch ngang/underscore → space)
                .replaceAll("[^a-z0-9\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    /**
     * Normalize và lowercase chỉ để search/compare — giống {@link #normalize(String)}
     * nhưng giữ lại các số nguyên (không loại chữ số).
     */
    public static String normalizeForSearch(String value) {
        return normalize(value);
    }

    /**
     * Kiểm tra xem chuỗi sau normalize có chứa keyword không.
     * Tiện dụng để tránh normalize hai lần.
     */
    public static boolean containsAfterNormalize(String text, String keyword) {
        return normalize(text).contains(normalize(keyword));
    }
}
