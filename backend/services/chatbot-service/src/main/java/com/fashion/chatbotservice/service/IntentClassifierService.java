package com.fashion.chatbotservice.service;

/**
 * Phân loại ý định (intent) của tin nhắn user.
 */
public interface IntentClassifierService {

    String CONSULT_SIZE = "CONSULT_SIZE";
    String CONSULT_SEASON = "CONSULT_SEASON";
    String ASK_PROMOTION = "ASK_PROMOTION";
    String ASK_POLICY = "ASK_POLICY";
    String SEARCH_PRODUCT = "SEARCH_PRODUCT";
    String CHECK_ORDER = "CHECK_ORDER";
    String GREETING = "GREETING";
    String OUT_OF_DOMAIN = "OUT_OF_DOMAIN";
    String GENERAL = "GENERAL";

    /**
     * Phân loại intent từ tin nhắn.
     */
    IntentScore classify(String message);

    /**
     * Khởi tạo dữ liệu training mặc định nếu chưa có.
     */
    void bootstrapDefaultIntentsIfNeeded();

    record IntentScore(String intent, double confidence) {}
}
