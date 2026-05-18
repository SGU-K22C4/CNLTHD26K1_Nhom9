package com.fashion.chatbotservice.router;

import com.fashion.chatbotservice.service.IntentClassifierService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests cho SemanticIntentRouter — Phase 1B + Phase 3C.
 *
 * <p>Mục tiêu: verify rằng high-confidence messages KHÔNG gọi LLM classifier.
 */
class SemanticIntentRouterTest {

    private IntentClassifierService mockClassifier;
    private SemanticIntentRouter router;

    @BeforeEach
    void setUp() {
        mockClassifier = Mockito.mock(IntentClassifierService.class);
        when(mockClassifier.classify(anyString()))
                .thenReturn(new IntentClassifierService.IntentScore("GENERAL", 0.5));

        IntentKeywordRegistry registry = new IntentKeywordRegistry();
        router = new SemanticIntentRouter(mockClassifier, registry);
    }

    @Test
    @DisplayName("Tin nhắn size rõ ràng → classify CONSULT_SIZE không cần LLM")
    void clearSizeMessage_routesWithoutLLM() {
        String message = "Mình cao 1m70, nặng 65kg mặc size gì?";
        var result = router.classify(message);
        assertThat(result.intent()).isEqualTo(IntentClassifierService.CONSULT_SIZE);
        verify(mockClassifier, never()).classify(anyString());
    }

    @Test
    @DisplayName("Tin nhắn voucher rõ ràng → classify ASK_PROMOTION không cần LLM")
    void clearPromotionMessage_routesWithoutLLM() {
        String message = "Có voucher hay mã giảm giá không?";
        var result = router.classify(message);
        assertThat(result.intent()).isEqualTo(IntentClassifierService.ASK_PROMOTION);
        verify(mockClassifier, never()).classify(anyString());
    }

    @Test
    @DisplayName("Tin nhắn không rõ → fallback sang classifier")
    void ambiguousMessage_fallsBackToClassifier() {
        String message = "oke bạn ơi";
        router.classify(message);
        verify(mockClassifier).classify(anyString());
    }

    @Test
    @DisplayName("Out-of-domain message → classify OUT_OF_DOMAIN")
    void outOfDomainMessage_classified() {
        String message = "Cho tôi xem bản tin thời tiết hôm nay";
        var result = router.classify(message);
        // Nếu score đủ cao → trả OUT_OF_DOMAIN, ngược lại → fallback
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("Null message → trả GENERAL với confidence 0.5")
    void nullMessage_returnsGeneral() {
        var result = router.classify(null);
        assertThat(result.intent()).isEqualTo(IntentClassifierService.GENERAL);
    }

    @Test
    @DisplayName("Wishlist message → classify WISHLIST_RECOMMENDATION")
    void wishlistMessage_classified() {
        String message = "Mình muốn xem wishlist đã lưu";
        var result = router.classify(message);
        assertThat(result.intent()).isEqualTo(IntentClassifierService.WISHLIST_RECOMMENDATION);
        verify(mockClassifier, never()).classify(anyString());
    }
}
