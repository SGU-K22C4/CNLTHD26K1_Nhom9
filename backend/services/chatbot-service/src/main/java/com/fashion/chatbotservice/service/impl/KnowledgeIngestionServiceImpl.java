package com.fashion.chatbotservice.service.impl;

import com.fashion.chatbotservice.model.KnowledgeDocument;
import com.fashion.chatbotservice.repository.KnowledgeDocumentRepository;
import com.fashion.chatbotservice.service.GraphRagService;
import com.fashion.chatbotservice.service.KnowledgeIngestionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Đọc và chunk tài liệu từ resources/knowledge/ rồi lưu vào MongoDB.
 * Chạy tự động khi startup (pha 1).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class KnowledgeIngestionServiceImpl implements KnowledgeIngestionService {

    private final KnowledgeDocumentRepository repository;
    private final GraphRagService graphRagService;

    @Value("${chatbot.knowledge.enabled:true}")
    private boolean knowledgeEnabled;

    @Value("${chatbot.knowledge.data-path:classpath:knowledge}")
    private String dataPath;

    @PostConstruct
    public void ingestOnStartup() {
        if (!knowledgeEnabled) {
            log.info("Knowledge base disabled, skipping ingestion");
            return;
        }
        try {
            ingestAll();
        } catch (Exception ex) {
            log.warn("Knowledge ingestion skipped at startup: {}", ex.getMessage());
        }
    }

    @Override
    public int ingestAll() {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        int totalChunks = 0;
        List<KnowledgeDocument> ingestedDocuments = new ArrayList<>();

        try {
            Resource[] resources = resolver.getResources(dataPath + "/*.md");
            for (Resource resource : resources) {
                String filename = resource.getFilename();
                if (filename == null) continue;

                String content = readResource(resource);
                List<KnowledgeDocument> chunks = chunkMarkdown(filename, content);

                repository.deleteBySource(filename);
                repository.saveAll(chunks);
                ingestedDocuments.addAll(chunks);

                totalChunks += chunks.size();
                log.info("Ingested {} chunks from {}", chunks.size(), filename);
            }

            graphRagService.rebuildGraph(ingestedDocuments);
        } catch (Exception ex) {
            log.error("Failed to ingest knowledge base: {}", ex.getMessage());
        }

        return totalChunks;
    }

    /**
     * Chunk tài liệu markdown theo heading (## hoặc ###).
     */
    List<KnowledgeDocument> chunkMarkdown(String source, String content) {
        List<KnowledgeDocument> chunks = new ArrayList<>();
        String[] lines = content.split("\n");

        String documentTitle = "";
        String currentHeading = "";
        String currentTopic = "";
        StringBuilder currentContent = new StringBuilder();

        for (String rawLine : lines) {
            String line = rawLine.trim();

            if (line.startsWith("# ") && !line.startsWith("## ")) {
                documentTitle = line.substring(2).trim();
                continue;
            }

            if (line.startsWith("## ") || line.startsWith("### ")) {
                // Flush previous chunk
                if (!currentContent.isEmpty() && !currentHeading.isBlank()) {
                    chunks.add(buildChunk(source, currentHeading, currentTopic, currentContent.toString().trim()));
                }

                currentHeading = line.replaceFirst("^#+\\s*", "").trim();
                currentTopic = deriveTopic(currentHeading);
                currentContent = new StringBuilder();
            } else if (!line.isBlank()) {
                if (currentContent.length() > 0) {
                    currentContent.append("\n");
                }
                currentContent.append(line);
            }
        }

        // Flush last chunk
        if (!currentContent.isEmpty() && !currentHeading.isBlank()) {
            chunks.add(buildChunk(source, currentHeading, currentTopic, currentContent.toString().trim()));
        }

        // Fallback: nếu không có heading, lưu toàn bộ nội dung làm 1 chunk
        if (chunks.isEmpty() && !content.isBlank()) {
            String title = documentTitle.isBlank() ? source : documentTitle;
            chunks.add(buildChunk(source, title, "general", content.trim()));
        }

        return chunks;
    }

    private KnowledgeDocument buildChunk(String source, String title, String topic, String content) {
        return KnowledgeDocument.builder()
                .id(UUID.randomUUID().toString())
                .source(source)
                .title(title)
                .topic(topic)
                .content(content)
                .updatedAt(Instant.now())
                .build();
    }

    private String deriveTopic(String heading) {
        String lower = heading.toLowerCase();
        if (lower.contains("giao hàng") || lower.contains("vận chuyển")) return "giao_hang";
        if (lower.contains("đổi trả")) return "doi_tra";
        if (lower.contains("thanh toán")) return "thanh_toan";
        if (lower.contains("bảo hành")) return "bao_hanh";
        if (lower.contains("size") || lower.contains("chọn size")) return "size";
        if (lower.contains("bảo quản")) return "bao_quan";
        if (lower.contains("khuyến mãi") || lower.contains("giảm giá")) return "khuyen_mai";
        if (lower.contains("bảo mật")) return "bao_mat";
        if (lower.contains("tài khoản")) return "tai_khoan";
        return "general";
    }

    private String readResource(Resource resource) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            return sb.toString();
        } catch (Exception ex) {
            log.error("Failed to read resource {}: {}", resource.getFilename(), ex.getMessage());
            return "";
        }
    }
}
