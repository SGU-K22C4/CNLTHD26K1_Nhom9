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
import dev.langchain4j.model.openai.OpenAiChatModel;
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
 * Uses DeepSeek via OpenAI-compatible endpoint.
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class AgentConfig {

    private final ChatSessionRepository chatSessionRepository;

    @Value("${ai.base-url}")
    private String baseUrl;

    @Value("${ai.api-key}")
    private String apiKey;

    @Value("${ai.model:deepseek-3.2}")
    private String modelName;

    @Value("${ai.timeout-seconds:120}")
    private int timeoutSeconds;

    @Value("${ai.max-tokens:1000}")
    private int maxTokens;

    @Value("${chatbot.memory.max-messages:20}")
    private int maxMessages;

    @Bean
    public ChatLanguageModel chatLanguageModel() {
        return OpenAiChatModel.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .modelName(modelName)
                .temperature(0.3)
                .maxTokens(maxTokens)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .logRequests(true)
                .logResponses(true)
                .build();
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
