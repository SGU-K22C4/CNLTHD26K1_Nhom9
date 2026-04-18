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
        // Model chính (thông minh, tool-calling tốt nhưng tốn credit)
        ChatLanguageModel primaryModel = OpenAiChatModel.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .modelName(modelName) // Ví dụ: google/gemini-1.5-pro
                .maxTokens(maxTokens)
                .temperature(0.3)
                .timeout(Duration.ofSeconds(30))
                .logRequests(true)
                .logResponses(true)
                .build();

        // Model dự phòng (giá rẻ, miễn phí hoặc public, dùng để chữa cháy khi Model chính hết credit)
        ChatLanguageModel fallbackModel = OpenAiChatModel.builder()
                .apiKey(apiKey) // Dùng chung key hoặc key provider khác
                .baseUrl(baseUrl)
                .modelName("google/gemini-pro") // Model giá rẻ/free của OpenRouter
                .maxTokens(maxTokens)
                .temperature(0.3)
                .timeout(Duration.ofSeconds(15))
                .build();

        // Langchain4j sẽ tự động chuyển sang fallbackModel nếu primaryModel ném ra Exception (Timeout, 429 Rate Limit, 402 Payment Required...)
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
