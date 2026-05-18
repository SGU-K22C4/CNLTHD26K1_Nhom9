package com.fashion.chatbotservice.config;

import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.data.message.AiMessage;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Custom Fallback wrapper cho nhieu ChatLanguageModel.
 * Khi model chinh fail (het credit, timeout, rate-limit),
 * tu dong chuyen sang model du phong tiep theo.
 *
 * <p>Override cả 3 phương thức generate() để hỗ trợ tool calling
 * (LangChain4j AiServices gọi generate with ToolSpecification).
 */
@Slf4j
public class FallbackChatLanguageModel implements ChatLanguageModel {

    private final List<ChatLanguageModel> models;

    public FallbackChatLanguageModel(ChatLanguageModel primary, ChatLanguageModel... fallbacks) {
        this.models = new ArrayList<>();
        this.models.add(primary);
        this.models.addAll(List.of(fallbacks));
    }

    @Override
    public Response<AiMessage> generate(List<ChatMessage> messages) {
        return executeWithFallback(model -> model.generate(messages));
    }

    @Override
    public Response<AiMessage> generate(List<ChatMessage> messages, List<ToolSpecification> toolSpecifications) {
        return executeWithFallback(model -> model.generate(messages, toolSpecifications));
    }

    @Override
    public Response<AiMessage> generate(List<ChatMessage> messages, ToolSpecification toolSpecification) {
        return executeWithFallback(model -> model.generate(messages, toolSpecification));
    }

    private Response<AiMessage> sanitizeResponse(Response<AiMessage> response) {
        if (response == null || response.content() == null || !response.content().hasToolExecutionRequests()) {
            return response;
        }

        try {
            List<ToolExecutionRequest> requests = response.content().toolExecutionRequests();
            if (requests == null || requests.isEmpty()) {
                return response;
            }

            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            List<ToolExecutionRequest> sanitizedRequests = new ArrayList<>();
            boolean modifiedAny = false;

            for (ToolExecutionRequest req : requests) {
                String argsJson = req.arguments();
                if (argsJson == null || argsJson.isBlank()) {
                    sanitizedRequests.add(req);
                    continue;
                }

                Map<String, Object> map = mapper.readValue(argsJson, new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
                boolean modified = false;
                for (Map.Entry<String, Object> entry : map.entrySet()) {
                    if (entry.getValue() == null) {
                        entry.setValue("");
                        modified = true;
                    }
                }

                if (modified) {
                    String newArgsJson = mapper.writeValueAsString(map);
                    ToolExecutionRequest sanitizedReq = ToolExecutionRequest.builder()
                            .id(req.id())
                            .name(req.name())
                            .arguments(newArgsJson)
                            .build();
                    sanitizedRequests.add(sanitizedReq);
                    modifiedAny = true;
                    log.info("Sanitized tool execution request '{}': replaced null arguments with empty strings", req.name());
                } else {
                    sanitizedRequests.add(req);
                }
            }

            if (modifiedAny) {
                AiMessage sanitizedAiMessage = response.content().text() != null
                        ? AiMessage.from(response.content().text(), sanitizedRequests)
                        : AiMessage.from(sanitizedRequests);
                return Response.from(sanitizedAiMessage, response.tokenUsage(), response.finishReason());
            }
        } catch (Exception ex) {
            log.warn("Failed to sanitize tool execution requests: {}", ex.getMessage());
        }

        return response;
    }

    private Response<AiMessage> executeWithFallback(ModelInvoker invoker) {
        Exception lastException = null;

        for (int i = 0; i < models.size(); i++) {
            try {
                Response<AiMessage> response = invoker.invoke(models.get(i));
                if (i > 0) {
                    log.info("Fallback model #{} responded successfully", i);
                }
                return sanitizeResponse(response);
            } catch (Exception ex) {
                lastException = ex;
                log.warn("Model #{} failed ({}): {}. Trying next fallback...",
                        i, ex.getClass().getSimpleName(), ex.getMessage());
            }
        }

        throw new RuntimeException("All " + models.size() + " LLM models failed. Last error: "
                + (lastException != null ? lastException.getMessage() : "unknown"), lastException);
    }

    @FunctionalInterface
    private interface ModelInvoker {
        Response<AiMessage> invoke(ChatLanguageModel model);
    }
}
