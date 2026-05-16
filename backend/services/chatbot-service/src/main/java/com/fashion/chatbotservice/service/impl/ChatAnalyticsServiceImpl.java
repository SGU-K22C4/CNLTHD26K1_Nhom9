package com.fashion.chatbotservice.service.impl;

import com.fashion.chatbotservice.agent.ToolResultCollector;
import com.fashion.chatbotservice.dto.ChatResponse;
import com.fashion.chatbotservice.model.ChatAnalyticsDocument;
import com.fashion.chatbotservice.repository.ChatAnalyticsRepository;
import com.fashion.chatbotservice.service.ChatAnalyticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Lưu structured log entry cho mỗi request chat để analytics.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChatAnalyticsServiceImpl implements ChatAnalyticsService {

    private final ChatAnalyticsRepository repository;

    @Override
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

            LinkedHashMap<String, Object> metadata = new LinkedHashMap<>();
            metadata.put("suggestedProductIds", response.getSuggestions().stream()
                    .map(item -> item != null ? item.getProductId() : null)
                    .filter(id -> id != null && !id.isBlank())
                    .collect(Collectors.toList()));
            metadata.put("suggestedProductNames", response.getSuggestions().stream()
                    .map(item -> item != null ? item.getName() : null)
                    .filter(name -> name != null && !name.isBlank())
                    .limit(5)
                    .collect(Collectors.toList()));
            metadata.put("promotionCodes", response.getPromotions().stream()
                    .map(item -> item != null ? item.getCode() : null)
                    .filter(code -> code != null && !code.isBlank())
                    .collect(Collectors.toList()));
            metadata.put("guardrailViolations", collector.getGuardrailViolations());
            metadata.put("toolFailure", collector.hasToolFailure());

            ChatAnalyticsDocument doc = ChatAnalyticsDocument.builder()
                    .traceId(traceId)
                    .sessionId(sessionId)
                    .userId(userId)
                    .timestamp(Instant.now())
                    .userMessage(userMessage)
                    .messageLength(userMessage != null ? userMessage.length() : 0)
                    .intent(response.getIntent())
                    .confidence(response.getConfidence())
                    .eventType("chat_turn")
                    .knowledgeSources(kbSources)
                    .topKnowledgeScore(topScore)
                    .replyLength(response.getReply() != null ? response.getReply().length() : 0)
                    .suggestionsCount(response.getSuggestions().size())
                    .promotionsCount(response.getPromotions().size())
                    .hasCitation(!kbSources.isEmpty())
                    .metadata(metadata)
                    .totalLatencyMs(totalLatencyMs)
                    .build();

            repository.save(doc);
        } catch (Exception ex) {
            log.warn("Failed to save analytics: {}", ex.getMessage());
        }
    }
}
