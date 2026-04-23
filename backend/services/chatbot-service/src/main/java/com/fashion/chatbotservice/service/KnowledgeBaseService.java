package com.fashion.chatbotservice.service;

import java.util.List;

/**
 * Tìm kiếm thông tin trong Knowledge Base (chính sách, FAQ, hướng dẫn).
 */
public interface KnowledgeBaseService {

    record SearchResult(String content, String title, String source, String topic, double score) {}

    /**
     * Tìm kiếm chunks phù hợp với query.
     */
    List<SearchResult> search(String query);
}
