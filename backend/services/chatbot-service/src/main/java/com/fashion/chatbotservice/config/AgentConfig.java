package com.fashion.chatbotservice.config;

import com.fashion.chatbotservice.agent.FashionAgent;
import com.fashion.chatbotservice.agent.FashionTools;
import com.fashion.chatbotservice.model.ChatSession;
import com.fashion.chatbotservice.repository.ChatSessionRepository;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
// FallbackChatLanguageModel is defined in this package (custom implementation for LangChain4j 0.36.x)
import dev.langchain4j.service.AiServices;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Wiring LangChain4j components: model, memory, tools → FashionAgent.
 * Uses Ollama as LLM provider.
 * Supports both local Ollama (localhost:11434) and cloud Ollama endpoints.
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class AgentConfig {

    private final ChatSessionRepository chatSessionRepository;

    @Value("${ollama.base-url:https://syncopated-pedagogic-nadia.ngrok-free.dev/api/chat}")
    private String baseUrl;

    @Value("${ollama.model:llama3.1:8b}")
    private String modelName;

    @Value("${ollama.fallback-model:llama3.1:8b}")
    private String fallbackModelName;

    @Value("${ollama.timeout-seconds:120}")
    private int primaryTimeoutSeconds;

    @Value("${ollama.fallback-timeout-seconds:90}")
    private int fallbackTimeoutSeconds;

    @Value("${chatbot.memory.max-messages:20}")
    private int maxMessages;

    @Bean
    public ChatLanguageModel chatLanguageModel() {
        String normalizedBaseUrl = normalizeOllamaBaseUrl(baseUrl);
        Duration primaryTimeout = Duration.ofSeconds(Math.max(10, primaryTimeoutSeconds));
        Duration fallbackTimeout = Duration.ofSeconds(Math.max(10, fallbackTimeoutSeconds));

        // Ollama native API supports base host URL and also tolerates user-provided /api/chat URL via normalization.
        ChatLanguageModel primaryModel = buildModel(normalizedBaseUrl, modelName, primaryTimeout);

        if (fallbackModelName == null
                || fallbackModelName.isBlank()
                || fallbackModelName.equalsIgnoreCase(modelName)) {
            return primaryModel;
        }

        ChatLanguageModel fallbackModel = buildModel(normalizedBaseUrl, fallbackModelName, fallbackTimeout);
        return new FallbackChatLanguageModel(primaryModel, fallbackModel);
    }

    private ChatLanguageModel buildModel(String normalizedBaseUrl, String targetModel, Duration timeout) {
        return OllamaChatModel.builder()
                .baseUrl(normalizedBaseUrl)
                .modelName(targetModel)
                .temperature(0.3)
                .timeout(timeout)
                .build();
    }

    private String normalizeOllamaBaseUrl(String rawBaseUrl) {
        String normalized = rawBaseUrl == null || rawBaseUrl.isBlank()
                ? "https://syncopated-pedagogic-nadia.ngrok-free.dev"
                : rawBaseUrl.trim();

        if (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }

        if (normalized.endsWith("/api/chat")) {
            return normalized.substring(0, normalized.length() - "/api/chat".length());
        }

        if (normalized.endsWith("/v1")) {
            return normalized.substring(0, normalized.length() - "/v1".length());
        }

        return normalized;
    }

    @Bean
    public ChatMemoryProvider chatMemoryProvider() {
        Map<String, MessageWindowChatMemory> memoryBySession = new ConcurrentHashMap<>();
        return sessionId -> memoryBySession.computeIfAbsent(String.valueOf(sessionId), id -> {
            MessageWindowChatMemory memory = MessageWindowChatMemory.builder()
                    .id(id)
                    .maxMessages(maxMessages)
                    .build();
            hydrateMemory(memory, id);
            return memory;
        });
    }

    private void hydrateMemory(MessageWindowChatMemory memory, String sessionId) {
        if (sessionId == null || sessionId.isBlank()) return;
        try {
            chatSessionRepository.findBySessionId(sessionId)
                    .map(ChatSession::getMessages)
                    .filter(messages -> messages != null && !messages.isEmpty())
                    .ifPresent(messages -> addMessagesToMemory(memory, messages));
        } catch (Exception ex) {
            log.warn("Skip chat memory hydration for session {}: {}", sessionId, ex.getMessage());
        }
    }

    private void addMessagesToMemory(MessageWindowChatMemory memory, List<ChatSession.ChatMessage> messages) {
        int start = Math.max(0, messages.size() - maxMessages);
        for (int i = start; i < messages.size(); i++) {
            ChatSession.ChatMessage msg = messages.get(i);
            if (msg == null || msg.getContent() == null || msg.getContent().isBlank()) continue;
            if (msg.getSender() == ChatSession.Sender.USER) {
                memory.add(UserMessage.from(msg.getContent()));
            } else {
                memory.add(AiMessage.from(msg.getContent()));
            }
        }
    }

    @Bean
    public FashionAgent fashionAgent(ChatLanguageModel model,
                                     ChatMemoryProvider memoryProvider,
                                     FashionTools fashionTools) {
        return AiServices.builder(FashionAgent.class)
                .chatLanguageModel(model)
                .chatMemoryProvider(memoryProvider)
                .tools(fashionTools)
                .build();
    }
}
