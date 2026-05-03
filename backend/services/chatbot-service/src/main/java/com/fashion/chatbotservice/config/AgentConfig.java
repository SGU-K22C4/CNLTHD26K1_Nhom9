package com.fashion.chatbotservice.config;

import com.fashion.chatbotservice.agent.FashionAgent;
import com.fashion.chatbotservice.agent.FashionTools;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
// FallbackChatLanguageModel is defined in this package (custom implementation for LangChain4j 0.36.x)
import dev.langchain4j.service.AiServices;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Wiring LangChain4j components: model, memory, tools → FashionAgent.
 * Uses Ollama (OpenAI-compatible API) as LLM provider.
 * Supports both local Ollama (localhost:11434) and cloud Ollama endpoints.
 */
@Configuration
public class AgentConfig {

    @Value("${openrouter.api-key:}")
    private String apiKey;

    @Value("${openrouter.base-url:https://openrouter.ai/api/v1}")
    private String baseUrl;

    @Value("${openrouter.model:openai/gpt-4o-mini}")
    private String modelName;

    @Value("${openrouter.max-tokens:600}")
    private int maxTokens;

    @Value("${chatbot.memory.max-messages:20}")
    private int maxMessages;

    @Bean
    public ChatLanguageModel chatLanguageModel() {
        // Model chính (Ollama - chạy local hoặc cloud, miễn phí không giới hạn)
        ChatLanguageModel primaryModel = OpenAiChatModel.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .modelName(modelName) // Ví dụ: qwen2.5:7b (Ollama)
                .maxTokens(maxTokens)
                .temperature(0.3)
                .timeout(Duration.ofSeconds(60)) // Ollama local có thể chậm hơn cloud
                .logRequests(true)
                .logResponses(true)
                .build();

        // Model dự phòng (Ollama fallback - model nhẹ hơn nếu model chính quá tải)
        ChatLanguageModel fallbackModel = OpenAiChatModel.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .modelName("llama3.1:8b") // Model dự phòng nhẹ của Ollama
                .maxTokens(maxTokens)
                .temperature(0.3)
                .timeout(Duration.ofSeconds(30))
                .build();

        // Tự động chuyển sang fallbackModel nếu primaryModel fail (Timeout, model not found...)
        return new FallbackChatLanguageModel(primaryModel, fallbackModel);
    }

    @Bean
    public ChatMemoryProvider chatMemoryProvider() {
        return sessionId -> MessageWindowChatMemory.builder()
                .id(sessionId)
                .maxMessages(maxMessages)
                .build();
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
