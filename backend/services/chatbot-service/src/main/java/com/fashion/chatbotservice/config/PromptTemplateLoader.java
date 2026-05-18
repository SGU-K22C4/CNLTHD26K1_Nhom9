package com.fashion.chatbotservice.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Load system prompt templates từ classpath resources.
 *
 * <p>Kết hợp với {@link com.fashion.chatbotservice.experiment.PromptExperimentService}
 * để A/B test các variant prompts.
 *
 * <p>File layout (classpath:prompts/):
 * <pre>
 * prompts/
 *   v1-baseline.md    — System prompt gốc (đầy đủ, ~143 dòng)
 *   v2-shorter.md     — Variant rút gọn (~60 dòng), intent-dynamic addons
 *   v3-structured.md  — Variant có headers + bullets rõ ràng hơn
 * </pre>
 *
 * <p>Nếu file không tìm thấy → fallback về prompt inline (không crash service).
 */
@Component
@Slf4j
public class PromptTemplateLoader {

    private static final String PROMPT_DIR = "prompts/";

    /** In-memory cache để tránh đọc classpath nhiều lần. */
    private final Map<String, String> promptCache = new ConcurrentHashMap<>();

    /**
     * Load prompt template theo key.
     *
     * @param promptKey key tương ứng với filename: "v1-baseline" → "prompts/v1-baseline.md"
     * @return nội dung prompt, hoặc {@link #FALLBACK_PROMPT} nếu file không tồn tại
     */
    public String load(String promptKey) {
        return promptCache.computeIfAbsent(promptKey, this::loadFromClasspath);
    }

    private String loadFromClasspath(String key) {
        String path = PROMPT_DIR + key + ".md";
        ClassPathResource resource = new ClassPathResource(path);

        if (!resource.exists()) {
            log.warn("Prompt file not found: {}. Using fallback prompt.", path);
            return FALLBACK_PROMPT;
        }

        try (InputStream is = resource.getInputStream()) {
            String content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            log.info("Loaded prompt template: {} ({} chars)", path, content.length());
            return content;
        } catch (IOException ex) {
            log.error("Failed to load prompt file {}: {}", path, ex.getMessage());
            return FALLBACK_PROMPT;
        }
    }

    /**
     * Reload tất cả cached prompts (dùng khi update file mà không restart service).
     */
    public void reload() {
        promptCache.clear();
        log.info("PromptTemplateLoader cache cleared — prompts will reload on next request.");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Fallback inline prompt (khi chưa có file)
    // ─────────────────────────────────────────────────────────────────────────

    private static final String FALLBACK_PROMPT = """
            Bạn là trợ lý tư vấn thời trang của một cửa hàng thời trang Việt Nam.
            Nhiệm vụ: tư vấn sản phẩm, gợi ý outfit, tư vấn size, và hỗ trợ đơn hàng.
            
            NGUYÊN TẮC:
            - Chỉ đề xuất sản phẩm có trong hệ thống (từ tool calls).
            - Không bịa giá, tên sản phẩm, hay mã khuyến mãi.
            - Trả lời bằng tiếng Việt, thân thiện và chuyên nghiệp.
            - Nếu không có thông tin → thừa nhận và hướng dẫn liên hệ CSKH.
            """;
}
