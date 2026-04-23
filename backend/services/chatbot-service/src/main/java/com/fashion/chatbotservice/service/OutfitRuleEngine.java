package com.fashion.chatbotservice.service;

import java.util.List;
import java.util.Set;

/**
 * Domain service: quy tắc gợi ý outfit theo mùa/dịp/phong cách.
 */
public interface OutfitRuleEngine {

    /**
     * Xây dựng danh sách query terms dựa trên dịp/mùa/phong cách.
     */
    List<String> buildQueries(String occasion, String style);

    /**
     * Trích xuất từ khóa sản phẩm cụ thể mà user đề cập.
     */
    Set<String> extractProductKeywords(String message);

    /**
     * Kiểm tra sản phẩm có khớp từ khóa mà user yêu cầu không.
     */
    boolean matchesKeywords(String productName, String productCategory, Set<String> keywords);
}
