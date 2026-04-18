package com.fashion.chatbotservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * Lưu trữ một chunk của tài liệu knowledge base đã được embedding.
 */
@Document(collection = "knowledge_documents")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeDocument {

    @Id
    private String id;

    /** Tên file nguồn (e.g. "faq.md") */
    private String source;

    /** Tiêu đề heading gần nhất */
    private String title;

    /** Chủ đề (e.g. "giao_hang", "doi_tra") */
    private String topic;

    /** Nội dung chunk */
    private String content;

    /** Thời điểm tạo/cập nhật */
    private Instant updatedAt;
}
