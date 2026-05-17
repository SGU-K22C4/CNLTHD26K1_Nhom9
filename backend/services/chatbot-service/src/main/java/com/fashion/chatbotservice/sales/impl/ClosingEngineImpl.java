package com.fashion.chatbotservice.sales.impl;

import com.fashion.chatbotservice.conversation.SalesStage;
import com.fashion.chatbotservice.model.ChatSession;
import com.fashion.chatbotservice.sales.CompareEngine;
import com.fashion.chatbotservice.sales.ClosingEngine;
import org.springframework.stereotype.Service;

@Service
public class ClosingEngineImpl implements ClosingEngine {

    @Override
    public String buildSoftClose(ChatSession.PreferenceProfile profile, int suggestionCount) {
        SalesStage stage = profile != null ? profile.getSalesStage() : null;
        if (stage == SalesStage.COMPARING) {
            return "Nếu bạn muốn chốt nhanh, mình có thể chọn luôn 1 phương án an toàn nhất trong nhóm này cho bạn.";
        }
        if (stage == SalesStage.CLOSING) {
            return suggestionCount > 1
                    ? "Trong các option hiện tại, bạn muốn mình chốt giúp 1 mẫu an toàn nhất hay 1 mẫu nổi bật hơn?"
                    : "Nếu bạn muốn, mình có thể gợi ý luôn cách phối để bạn chốt nhanh hơn.";
        }
        if (stage == SalesStage.RECOMMENDING) {
            return suggestionCount > 0
                    ? "Bạn thích vibe an toàn, dễ mặc hay muốn nổi bật hơn để mình chốt 1-2 mẫu phù hợp nhất?"
                    : "";
        }
        return "";
    }

    @Override
    public String buildDecisionClose(ChatSession.PreferenceProfile profile, CompareEngine.CompareResult compareResult) {
        if (compareResult == null || compareResult.saferChoice() == null) {
            return "";
        }
        SalesStage stage = profile != null ? profile.getSalesStage() : null;
        if (stage == SalesStage.COMPARING || stage == SalesStage.CLOSING) {
            return "Nếu bạn muốn chốt nhanh, mình nghiêng về **" + compareResult.saferChoice().getName()
                    + "** trước. Nếu muốn nổi bật hơn thì chuyển sang **"
                    + safeName(compareResult.moreStylishChoice(), compareResult.saferChoice().getName()) + "**.";
        }
        return "Nếu cần, mình có thể chốt giúp bạn 1 mẫu an toàn nhất hoặc 1 mẫu nổi bật hơn trong nhóm này.";
    }

    private String safeName(com.fashion.chatbotservice.dto.ChatResponse.ProductSuggestion suggestion, String fallback) {
        return suggestion != null && suggestion.getName() != null && !suggestion.getName().isBlank()
                ? suggestion.getName()
                : fallback;
    }
}
