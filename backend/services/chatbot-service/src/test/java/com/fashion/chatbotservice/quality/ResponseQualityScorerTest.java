package com.fashion.chatbotservice.quality;

import com.fashion.chatbotservice.agent.ToolResultCollector;
import com.fashion.chatbotservice.dto.ChatResponse;
import com.fashion.chatbotservice.quality.impl.ResponseQualityScorerImpl;
import com.fashion.chatbotservice.service.IntentClassifierService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests cho ResponseQualityScorer — Phase 2C + Phase 3C.
 */
class ResponseQualityScorerTest {

    private ResponseQualityScorer scorer;

    @BeforeEach
    void setUp() {
        scorer = new ResponseQualityScorerImpl();
    }

    @Test
    @DisplayName("Response có products và grounded price → điểm cao")
    void highQualityProductResponse() {
        ToolResultCollector collector = new ToolResultCollector();
        collector.addProducts(List.of(
                ChatResponse.ProductSuggestion.builder()
                        .productId("P001").name("Áo thun basic").price("299.000đ").build()
        ));

        ChatResponse response = ChatResponse.builder()
                .sessionId("test")
                .intent(IntentClassifierService.SEARCH_PRODUCT)
                .confidence(0.9)
                .reply("Mình tìm thấy áo thun basic giá 299.000đ, phù hợp với style casual bạn muốn. " +
                        "Bạn muốn thêm vào giỏ hàng không ạ?")
                .suggestions(collector.getProducts())
                .createdAt(Instant.now())
                .build();

        QualityScore score = scorer.score(response, collector, "tìm áo thun");

        assertThat(score.getTotal()).isGreaterThanOrEqualTo(60);
        assertThat(score.isLowQuality()).isFalse();
        assertThat(score.getBreakdown()).containsKey("has_products");
    }

    @Test
    @DisplayName("Response không có products cho intent SEARCH_PRODUCT → điểm thấp hơn")
    void lowQualityNoProducts() {
        ToolResultCollector collector = new ToolResultCollector(); // empty

        ChatResponse response = ChatResponse.builder()
                .sessionId("test")
                .intent(IntentClassifierService.SEARCH_PRODUCT)
                .confidence(0.7)
                .reply("Mình nghĩ có thể có áo thun khoảng vài trăm nghìn.")
                .createdAt(Instant.now())
                .build();

        QualityScore score = scorer.score(response, collector, "tìm áo thun");

        assertThat(score.getBreakdown()).doesNotContainKey("has_products");
        assertThat(score.getWarnings()).isNotEmpty();
    }

    @Test
    @DisplayName("Null response → score 0 với warning")
    void nullResponseHandled() {
        QualityScore score = scorer.score(null, new ToolResultCollector(), "test");
        assertThat(score.getTotal()).isEqualTo(0);
        assertThat(score.getWarnings()).containsExactly("Null response");
    }

    @Test
    @DisplayName("Greeting intent → score tốt vì không yêu cầu products")
    void greetingIntentScoresOk() {
        ToolResultCollector collector = new ToolResultCollector();

        ChatResponse response = ChatResponse.builder()
                .sessionId("test")
                .intent(IntentClassifierService.GREETING)
                .confidence(0.98)
                .reply("Xin chào! Mình có thể giúp bạn tìm sản phẩm, tư vấn size và kiểm tra khuyến mãi nhé!")
                .createdAt(Instant.now())
                .build();

        QualityScore score = scorer.score(response, collector, "xin chào");
        assertThat(score.getTotal()).isGreaterThan(40);
    }

    @Test
    @DisplayName("QualityScore.calculate() tính đúng tổng")
    void calculateSumsBreakdown() {
        QualityScore score = QualityScore.builder().build();
        score.addPoint("a", 20);
        score.addPoint("b", 30);
        score.addPoint("c", 25);
        score.calculate();
        assertThat(score.getTotal()).isEqualTo(75);
        assertThat(score.isGood()).isTrue();
    }
}
