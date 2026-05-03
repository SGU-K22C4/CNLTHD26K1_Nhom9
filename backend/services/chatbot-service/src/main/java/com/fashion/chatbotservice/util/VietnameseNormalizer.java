package com.fashion.chatbotservice.util;

import java.util.Locale;

/**
 * Utility chuẩn hóa chuỗi tiếng Việt: bỏ dấu, lowercase, loại ký tự đặc biệt.
 * Dùng chung cho mọi service cần normalize message tiếng Việt.
 */
public final class VietnameseNormalizer {

    private VietnameseNormalizer() {}

    public static String normalize(String value) {
        if (value == null) return "";
        return value.toLowerCase(Locale.ROOT)
                .replace('đ', 'd')
                .replaceAll("[áàảãạâấầẩẫậăắằẳẵặ]", "a")
                .replaceAll("[éèẻẽẹêếềểễệ]", "e")
                .replaceAll("[íìỉĩị]", "i")
                .replaceAll("[óòỏõọôốồổỗộơớờởỡợ]", "o")
                .replaceAll("[úùủũụưứừửữự]", "u")
                .replaceAll("[ýỳỷỹỵ]", "y")
                .replaceAll("[^a-z0-9\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }
}
