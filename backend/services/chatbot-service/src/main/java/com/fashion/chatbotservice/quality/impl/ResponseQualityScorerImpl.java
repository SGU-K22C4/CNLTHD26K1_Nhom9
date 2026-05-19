package com.fashion.chatbotservice.quality.impl;

import com.fashion.chatbotservice.agent.ToolResultCollector;
import com.fashion.chatbotservice.dto.ChatResponse;
import com.fashion.chatbotservice.quality.QualityScore;
import com.fashion.chatbotservice.quality.ResponseQualityScorer;
import com.fashion.chatbotservice.service.IntentClassifierService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Triển khai Response Quality Scorer — Phase 2C.
 *
 * <p>Điểm tối đa: 100. Cấu trúc:
 * <pre>
 * ┌─────────────────────────────────────────┬───────┐
 * │ Tiêu chí                                │ Điểm  │
 * ├─────────────────────────────────────────┼───────┤
 * │ Grounding: có tool results (products)   │ +15   │
 * │ Grounding: giá khớp với tool data       │ +10   │
 * │ No hallucination (không bịa sản phẩm)   │ +20   │
 * │ Relevance: trả lời đúng intent          │ +15   │
 * │ Completeness: có lý giải/gợi ý          │ +10   │
 * │ CTA: có call-to-action phù hợp          │ +5    │
 * │ Brevity: không quá dài (< 500 chars)    │ +5    │
 * │ Policy grounding (khi hỏi chính sách)   │ +10   │
 * │ Safety: không có nội dung nhạy cảm      │ +10   │
 * └─────────────────────────────────────────┴───────┘
 * </pre>
 *
 * <p>Log cảnh báo khi score < 60 để review thủ công.
 */
@Service
@Slf4j
public class ResponseQualityScorerImpl implements ResponseQualityScorer {

    // Hallucination detection: các pattern "bịa" giá/sản phẩm
    private static final List<String> HALLUCINATION_SIGNALS = List.of(
            "ví dụ", "khoảng", "tầm", "có thể là", "thường thì",
            "theo tôi biết", "mình nghĩ"
    );

    private static final List<String> CTA_SIGNALS = List.of(
            "thêm vào giỏ", "mua ngay", "đặt hàng", "liên hệ", "xem thêm",
            "bạn muốn", "mình có thể", "thử ngay", "chốt không"
    );

    private static final List<String> REASONING_SIGNALS = List.of(
            "vì", "bởi vì", "lý do", "phù hợp với", "dễ phối", "an toàn",
            "nổi bật hơn", "gợi ý bạn"
    );

    @Override
    public QualityScore score(ChatResponse response, ToolResultCollector collector, String userMessage) {
        QualityScore qs = QualityScore.builder().build();

        if (response == null) {
            qs.addWarning("Null response");
            qs.calculate();
            return qs;
        }

        String reply = response.getReply() == null ? "" : response.getReply().toLowerCase();
        String intent = response.getIntent();

        // 1. Grounding: có products từ tool
        if (collector != null && !collector.getProducts().isEmpty()) {
            qs.addPoint("has_products", 15);
        } else if (isProductIntent(intent) && reply.contains("mình chưa tìm thấy")) {
            // Honest no-result reply vẫn OK
            qs.addPoint("honest_no_result", 5);
        } else if (isProductIntent(intent)) {
            qs.addWarning("No products suggested for a product intent");
        }

        // 2. Grounding: giá trong reply có vẻ từ data thật (có chữ số + đ/k/vnđ)
        if (containsGroundedPrice(reply)) {
            qs.addPoint("grounded_prices", 10);
        }

        // 3. No hallucination
        boolean hasHallucination = HALLUCINATION_SIGNALS.stream()
                .anyMatch(signal -> reply.contains(signal)
                        && (reply.contains("giá") || reply.contains("sản phẩm")));
        if (!hasHallucination) {
            qs.addPoint("no_hallucination", 20);
        } else {
            qs.addWarning("Possible hallucination detected in reply");
        }

        // 4. Relevance: response khớp với intent
        if (isRelevantToIntent(reply, intent)) {
            qs.addPoint("relevant", 15);
        } else {
            qs.addWarning("Response may not match intent: " + intent);
        }

        // 5. Completeness: có reasoning/explanation
        boolean hasReasoning = REASONING_SIGNALS.stream().anyMatch(reply::contains);
        if (hasReasoning) {
            qs.addPoint("has_reasoning", 10);
        }

        // 6. CTA
        boolean hasCTA = CTA_SIGNALS.stream().anyMatch(reply::contains);
        if (hasCTA) {
            qs.addPoint("has_cta", 5);
        }

        // 7. Brevity (không quá dài)
        if (reply.length() < 500) {
            qs.addPoint("brevity", 5);
        } else if (reply.length() > 1000) {
            qs.addWarning("Response too long: " + reply.length() + " chars");
        }

        // 8. Policy grounding
        if (IntentClassifierService.ASK_POLICY.equals(intent)) {
            boolean hasKnowledgeCitation = reply.contains("chính sách")
                    || reply.contains("theo quy định") || reply.contains("hướng dẫn");
            if (hasKnowledgeCitation) {
                qs.addPoint("policy_grounded", 10);
            }
        } else {
            qs.addPoint("policy_grounded", 10); // Non-policy intents không cần
        }

        // 9. Safety
        boolean isSafe = !containsUnsafeContent(reply);
        if (isSafe) {
            qs.addPoint("safety", 10);
        } else {
            qs.addWarning("Potentially unsafe content detected");
        }

        qs.calculate();

        if (qs.isLowQuality()) {
            log.warn("Low quality response (score={}): intent={}, warnings={}, reply={}",
                    qs.getTotal(), intent, qs.getWarnings(),
                    reply.substring(0, Math.min(100, reply.length())));
        } else {
            log.debug("Quality score: {}/100 for intent={}", qs.getTotal(), intent);
        }

        return qs;
    }

    private boolean isProductIntent(String intent) {
        return IntentClassifierService.SEARCH_PRODUCT.equals(intent)
                || IntentClassifierService.CONSULT_SIZE.equals(intent)
                || IntentClassifierService.CONSULT_SEASON.equals(intent);
    }

    private boolean containsGroundedPrice(String reply) {
        return reply.matches(".*\\d{3,}[\\s.,]*(đ|k|vnđ|đồng|000).*")
                || reply.matches(".*\\d+[.,]\\d{3}.*");
    }

    private boolean isRelevantToIntent(String reply, String intent) {
        if (intent == null) return true;
        return switch (intent) {
            case IntentClassifierService.SEARCH_PRODUCT ->
                    reply.contains("sản phẩm") || reply.contains("mẫu") || reply.contains("áo")
                            || reply.contains("quần") || reply.contains("váy") || reply.contains("tìm");
            case IntentClassifierService.CONSULT_SIZE ->
                    reply.contains("size") || reply.contains("số đo") || reply.contains("chiều cao");
            case IntentClassifierService.ASK_PROMOTION ->
                    reply.contains("khuyến mãi") || reply.contains("voucher") || reply.contains("giảm");
            case IntentClassifierService.CHECK_ORDER ->
                    reply.contains("đơn hàng") || reply.contains("ORD") || reply.contains("giao hàng");
            case IntentClassifierService.ASK_POLICY ->
                    reply.contains("chính sách") || reply.contains("quy định") || reply.contains("đổi trả");
            default -> true;
        };
    }

    private boolean containsUnsafeContent(String reply) {
        return reply.contains("chính trị") || reply.contains("bạo lực")
                || reply.contains("lừa đảo") || reply.contains("hack");
    }
}
