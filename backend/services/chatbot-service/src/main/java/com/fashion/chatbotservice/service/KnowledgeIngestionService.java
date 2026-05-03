package com.fashion.chatbotservice.service;

/**
 * Đọc và chunk tài liệu từ resources/knowledge/ rồi lưu vào MongoDB.
 */
public interface KnowledgeIngestionService {

    /**
     * Đọc tất cả file .md từ thư mục knowledge và lưu chunks vào MongoDB.
     */
    int ingestAll();
}
