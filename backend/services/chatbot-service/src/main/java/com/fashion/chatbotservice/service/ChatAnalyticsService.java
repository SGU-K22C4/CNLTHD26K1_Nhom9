package com.fashion.chatbotservice.service;

import com.fashion.chatbotservice.agent.ToolResultCollector;
import com.fashion.chatbotservice.dto.ChatResponse;
import com.fashion.chatbotservice.model.ChatAnalyticsDocument;
import com.fashion.chatbotservice.repository.ChatAnalyticsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

/**
 * Lưu structured log entry cho mỗi request chat để analytics.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChatAnalyticsService {

    private final ChatAnalyticsRepository repository;

    public void record(String traceId, String sessionId, String userId,
                       String userMessage, ChatResponse response,
                       ToolResultCollector collector, long totalLatencyMs) {
        try {
            List<String> kbSources = collector.getKnowledgeSources().stream()
                    .map(ks -> ks.title() + " (" + ks.source() + ")")
                    .toList();

            double topScore = collector.getKnowledgeSources().stream()
                    .mapToDouble(ToolResultCollector.KnowledgeSource::score)
                    .max().orElse(0.0);

            ChatAnalyticsDocument doc = ChatAnalyticsDocument.builder()
                    .traceId(traceId)
                    .sessionId(sessionId)
                    .userId(userId)
                    .timestamp(Instant.now())
                    .userMessage(userMessage)
                    .messageLength(userMessage != null ? userMessage.length() : 0)
                    .intent(response.getIntent())
                    .confidence(response.getConfidence())
                    .knowledgeSources(kbSources)
                    .topKnowledgeScore(topScore)
                    .replyLength(response.getReply() != null ? response.getReply().length() : 0)
                    .suggestionsCount(response.getSuggestions().size())
                    .promotionsCount(response.getPromotions().size())
                    .hasCitation(!kbSources.isEmpty())
                    .totalLatencyMs(totalLatencyMs)
                    .build();

            repository.save(doc);
        } catch (Exception ex) {
            log.warn("Failed to save analytics: {}", ex.getMessage());
        }
    }
}
