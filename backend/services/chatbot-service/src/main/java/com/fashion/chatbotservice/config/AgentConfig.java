package com.fashion.chatbotservice.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fashion.chatbotservice.agent.FashionAgent;
import com.fashion.chatbotservice.agent.FashionTools;
import com.fashion.chatbotservice.model.ChatSession;
import com.fashion.chatbotservice.repository.ChatSessionRepository;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolMemoryId;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
// FallbackChatLanguageModel is defined in this package (custom implementation for LangChain4j 0.36.x)
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.tool.ToolExecutor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static dev.langchain4j.agent.tool.ToolSpecifications.toolSpecificationFrom;

/**
 * Wiring LangChain4j components: model, memory, tools → FashionAgent.
 * Uses DeepSeek via OpenAI-compatible endpoint.
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class AgentConfig {

    private static final TypeReference<Map<String, Object>> TOOL_ARGUMENTS_TYPE = new TypeReference<>() {
    };

    private final ChatSessionRepository chatSessionRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${ai.base-url}")
    private String baseUrl;

    @Value("${ai.api-key}")
    private String apiKey;

    @Value("${ai.model:deepseek-3.2}")
    private String modelName;

    @Value("${ai.timeout-seconds:1200}")
    private int timeoutSeconds;

    @Value("${ai.max-tokens:10000}")
    private int maxTokens;

    @Value("${ai.log-requests:true}")
    private boolean logRequests;

    @Value("${ai.log-responses:true}")
    private boolean logResponses;

    @Value("${chatbot.memory.max-messages:1000}")
    private int maxMessages;

    @Bean
    public ChatLanguageModel chatLanguageModel() {
        log.info("Initializing FallbackChatLanguageModel: primary=gemini-2.5-flash, fallback=deepseek-3.2, baseUrl={}, timeout={}s, maxTokens={}",
                baseUrl, timeoutSeconds, maxTokens);

        ChatLanguageModel primary = OpenAiChatModel.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .modelName("gemini-2.5-flash")
                .temperature(0.1) // Lower = more deterministic tool calling
                .maxTokens(maxTokens)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .logRequests(logRequests)
                .logResponses(logResponses)
                .build();

        ChatLanguageModel fallback = OpenAiChatModel.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .modelName("deepseek-3.2")
                .temperature(0.1)
                .maxTokens(maxTokens)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .logRequests(logRequests)
                .logResponses(logResponses)
                .build();

        return new FallbackChatLanguageModel(primary, fallback);
    }

    private final Map<String, MessageWindowChatMemory> chatMemoryStore = new ConcurrentHashMap<>();

    @Bean
    public ChatMemoryProvider chatMemoryProvider() {
        return sessionId -> chatMemoryStore.computeIfAbsent(String.valueOf(sessionId), id -> {
            MessageWindowChatMemory memory = MessageWindowChatMemory.builder()
                    .id(id)
                    .maxMessages(maxMessages)
                    .build();
            hydrateMemory(memory, id);
            return memory;
        });
    }

    /**
     * Xóa chat memory của một session (dùng khi memory bị corrupted).
     */
    public void clearSessionMemory(String sessionId) {
        if (sessionId != null) {
            chatMemoryStore.remove(sessionId);
            log.info("Cleared corrupted chat memory for session: {}", sessionId);
        }
    }

    private void hydrateMemory(MessageWindowChatMemory memory, String sessionId) {
        if (sessionId == null || sessionId.isBlank())
            return;
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
            if (msg == null || msg.getContent() == null || msg.getContent().isBlank())
                continue;
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
                // LangChain4j beta can crash before entering our tool method when the model
                // sends `"field": null`. We register a tolerant executor so optional fields
                // remain nullable instead of corrupting the whole chat session.
                .tools(buildSafeToolExecutors(fashionTools))
                .build();
    }

    private Map<ToolSpecification, ToolExecutor> buildSafeToolExecutors(Object toolHost) {
        Map<ToolSpecification, ToolExecutor> executors = new LinkedHashMap<>();
        for (Method method : toolHost.getClass().getDeclaredMethods()) {
            if (!method.isAnnotationPresent(Tool.class)) {
                continue;
            }
            ToolSpecification specification = toolSpecificationFrom(method);
            executors.put(specification, (toolExecutionRequest, memoryId) ->
                    executeToolSafely(toolHost, method, toolExecutionRequest, memoryId));
        }
        return executors;
    }

    private String executeToolSafely(Object toolHost, Method method,
            ToolExecutionRequest toolExecutionRequest, Object memoryId) {
        Object[] arguments = prepareToolArguments(method, toolExecutionRequest, memoryId);
        try {
            method.setAccessible(true);
            Object result = method.invoke(toolHost, arguments);
            return result == null ? "" : String.valueOf(result);
        } catch (InvocationTargetException ex) {
            Throwable cause = ex.getCause() == null ? ex : ex.getCause();
            log.error("Error while executing tool {}", method.getName(), cause);
            return cause.getMessage();
        } catch (Exception ex) {
            log.error("Failed to execute tool {}", method.getName(), ex);
            return ex.getMessage();
        }
    }

    private Object[] prepareToolArguments(Method method, ToolExecutionRequest toolExecutionRequest, Object memoryId) {
        Map<String, Object> argumentsMap = parseToolArguments(toolExecutionRequest.arguments());
        Parameter[] parameters = method.getParameters();
        Object[] arguments = new Object[parameters.length];

        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];

            if (parameter.isAnnotationPresent(ToolMemoryId.class)) {
                arguments[i] = memoryId;
                continue;
            }

            String parameterName = parameter.getName();
            if (!argumentsMap.containsKey(parameterName)) {
                arguments[i] = defaultValueFor(parameter.getType());
                continue;
            }

            arguments[i] = coerceToolArgument(argumentsMap.get(parameterName), parameter);
        }

        return arguments;
    }

    private Map<String, Object> parseToolArguments(String rawArguments) {
        if (rawArguments == null || rawArguments.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(rawArguments, TOOL_ARGUMENTS_TYPE);
        } catch (Exception ex) {
            log.warn("Could not parse tool arguments, using empty argument map instead: {}", ex.getMessage());
            return Map.of();
        }
    }

    private Object coerceToolArgument(Object argument, Parameter parameter) {
        Class<?> parameterType = parameter.getType();

        if (argument == null) {
            return defaultValueFor(parameterType);
        }
        if (parameterType == String.class) {
            return String.valueOf(argument);
        }
        if (parameterType == Integer.class || parameterType == int.class) {
            return toInteger(argument, parameterType.isPrimitive() ? 0 : null);
        }
        if (parameterType == Long.class || parameterType == long.class) {
            return toLong(argument, parameterType.isPrimitive() ? 0L : null);
        }
        if (parameterType == Boolean.class || parameterType == boolean.class) {
            return toBoolean(argument, parameterType == boolean.class && Boolean.parseBoolean(String.valueOf(argument)));
        }
        if (parameterType.isEnum()) {
            return coerceEnum(argument, parameterType);
        }

        try {
            return objectMapper.convertValue(argument, objectMapper.constructType(parameter.getParameterizedType()));
        } catch (IllegalArgumentException ex) {
            log.warn("Could not coerce tool argument '{}' for type {}: {}",
                    parameter.getName(), parameterType.getSimpleName(), ex.getMessage());
            return defaultValueFor(parameterType);
        }
    }

    private Object defaultValueFor(Class<?> parameterType) {
        if (!parameterType.isPrimitive()) {
            return null;
        }
        if (parameterType == boolean.class) {
            return false;
        }
        if (parameterType == char.class) {
            return '\0';
        }
        if (parameterType == byte.class) {
            return (byte) 0;
        }
        if (parameterType == short.class) {
            return (short) 0;
        }
        if (parameterType == int.class) {
            return 0;
        }
        if (parameterType == long.class) {
            return 0L;
        }
        if (parameterType == float.class) {
            return 0f;
        }
        if (parameterType == double.class) {
            return 0d;
        }
        return null;
    }

    private Integer toInteger(Object argument, Integer defaultValue) {
        if (argument instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(argument).trim());
        } catch (Exception ex) {
            return defaultValue;
        }
    }

    private Long toLong(Object argument, Long defaultValue) {
        if (argument instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(argument).trim());
        } catch (Exception ex) {
            return defaultValue;
        }
    }

    private Boolean toBoolean(Object argument, Boolean defaultValue) {
        if (argument instanceof Boolean booleanValue) {
            return booleanValue;
        }
        if (argument == null) {
            return defaultValue;
        }
        String normalized = String.valueOf(argument).trim();
        if ("true".equalsIgnoreCase(normalized)) {
            return true;
        }
        if ("false".equalsIgnoreCase(normalized)) {
            return false;
        }
        return defaultValue;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private Object coerceEnum(Object argument, Class<?> enumType) {
        String rawValue = String.valueOf(argument).trim();
        if (rawValue.isBlank()) {
            return null;
        }
        try {
            return Enum.valueOf((Class<? extends Enum>) enumType, rawValue);
        } catch (IllegalArgumentException ex) {
            return Enum.valueOf((Class<? extends Enum>) enumType, rawValue.toUpperCase());
        }
    }
}

