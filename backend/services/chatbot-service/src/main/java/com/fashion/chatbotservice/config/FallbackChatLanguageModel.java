package com.fashion.chatbotservice.config;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.data.message.AiMessage;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * Custom Fallback wrapper cho nhieu ChatLanguageModel.
 * Khi model chinh fail (het credit, timeout, rate-limit),
 * tu dong chuyen sang model du phong tiep theo.
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
        Exception lastException = null;

        for (int i = 0; i < models.size(); i++) {
            try {
                Response<AiMessage> response = models.get(i).generate(messages);
                if (i > 0) {
                    log.info("Fallback model #{} responded successfully", i);
                }
                return response;
            } catch (Exception ex) {
                lastException = ex;
                log.warn("Model #{} failed ({}): {}. Trying next fallback...",
                        i, ex.getClass().getSimpleName(), ex.getMessage());
            }
        }

        throw new RuntimeException("All " + models.size() + " LLM models failed. Last error: "
                + (lastException != null ? lastException.getMessage() : "unknown"), lastException);
    }
}
