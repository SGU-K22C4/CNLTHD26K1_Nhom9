package com.fashion.chatbotservice.conversation;

/**
 * SalesStage keeps the conversational goal explicit so the orchestrator can
 * decide whether to ask, recommend, compare, or softly close instead of
 * treating every turn like a plain search request.
 */
public enum SalesStage {
    DISCOVERY,
    STYLE_DISCOVERY,
    FILTERING,
    RECOMMENDING,
    COMPARING,
    CLOSING,
    AFTER_SALES
}
