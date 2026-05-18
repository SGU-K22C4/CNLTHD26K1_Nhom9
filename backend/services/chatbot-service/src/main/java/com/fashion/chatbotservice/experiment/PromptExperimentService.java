package com.fashion.chatbotservice.experiment;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * A/B Testing Framework cho System Prompts — Phase 3C.
 *
 * <p>Bucket users vào 3 nhóm dựa trên hash của userId:
 * <ul>
 *   <li>{@code control}   — v1 baseline (System prompt hiện tại)</li>
 *   <li>{@code variant-a} — v2 shorter (System prompt ngắn hơn ~40% token)</li>
 *   <li>{@code variant-b} — v3 structured (System prompt có cấu trúc rõ hơn)</li>
 * </ul>
 *
 * <p>Kết quả được log để so sánh quality score và latency.
 */
@Service
@Slf4j
public class PromptExperimentService {

    @Value("${chatbot.experiment.enabled:false}")
    private boolean experimentEnabled;

    @Value("${chatbot.experiment.control-ratio:70}")
    private int controlRatio; // 0-100, phần trăm user vào control group

    /** Map variant → mô tả (dùng cho logging). */
    private static final Map<String, String> VARIANT_DESCRIPTIONS = Map.of(
            "control",   "v1-baseline (full 143-line system prompt)",
            "variant-a", "v2-shorter (reduced to ~60 lines, intent-dynamic addons)",
            "variant-b", "v3-structured (headers + bullets format)"
    );

    /**
     * Xác định variant cho user dựa trên hash.
     */
    public String getBucket(String userId) {
        if (!experimentEnabled || userId == null) return "control";
        int hash = Math.abs(userId.hashCode()) % 100;
        if (hash < controlRatio) return "control";
        if (hash < controlRatio + (100 - controlRatio) / 2) return "variant-a";
        return "variant-b";
    }

    /**
     * Trả về system prompt phù hợp với bucket của user.
     * Hiện tại trả về key để AgentConfig/PromptTemplateLoader load đúng file.
     */
    public String getPromptKey(String userId) {
        String bucket = getBucket(userId);
        return switch (bucket) {
            case "variant-a" -> "v2-shorter";
            case "variant-b" -> "v3-structured";
            default          -> "v1-baseline";
        };
    }

    /**
     * Log kết quả experiment để phân tích sau.
     */
    public void logResult(String userId, String bucket, int qualityScore, long latencyMs) {
        if (!experimentEnabled) return;
        log.info("[EXPERIMENT] userId={}, bucket={}, qualityScore={}, latencyMs={}, variant={}",
                userId, bucket, qualityScore, latencyMs,
                VARIANT_DESCRIPTIONS.getOrDefault(bucket, bucket));
    }

    public boolean isExperimentEnabled() {
        return experimentEnabled;
    }
}
