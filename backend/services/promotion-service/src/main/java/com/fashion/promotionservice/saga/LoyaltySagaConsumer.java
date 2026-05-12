package com.fashion.promotionservice.saga;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fashion.common.event.OrderCancelledEvent;
import com.fashion.common.event.SagaTopics;
import com.fashion.promotionservice.dto.request.RefundRequest;
import com.fashion.promotionservice.service.LoyaltyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.TopicSuffixingStrategy;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Saga Consumer for Promotion Service.
 * <p>
 * Listens to {@link SagaTopics#ORDER_CANCELLED} events and refunds loyalty points
 * that were used (redeemed) when the order was created.
 * <p>
 * Idempotency is guaranteed by {@link LoyaltyService#refund}, which checks for
 * duplicate REFUND transactions by {@code refId} before processing.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class LoyaltySagaConsumer {

    private final ObjectMapper objectMapper;
    private final LoyaltyService loyaltyService;

    // ── Order Cancelled → Refund Loyalty Points ─────────────────────────────

    @RetryableTopic(
            attempts = "${app.kafka.saga.retry-attempts:4}",
            backoff = @Backoff(
                    delayExpression = "${app.kafka.saga.retry-delay-ms:1000}",
                    multiplierExpression = "${app.kafka.saga.retry-multiplier:2.0}"
            ),
            retryTopicSuffix = ".retry",
            dltTopicSuffix = ".dlt",
            topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE,
            autoCreateTopics = "true"
    )
    @KafkaListener(topics = SagaTopics.ORDER_CANCELLED, groupId = "${spring.application.name}-loyalty-refund")
    @Transactional
    public void handleOrderCancelled(String payload) {
        OrderCancelledEvent event = parseEvent(payload);

        // Skip: no points were used in this order
        if (event.getUsedPoints() == null || event.getUsedPoints() <= 0) {
            log.info("No points to refund for orderId={}", event.getOrderId());
            return;
        }

        // Skip: guest users are not eligible for loyalty
        if (event.getUserId() == null || event.getUserId().startsWith("guest-")) {
            log.info("Guest user, skip refund for orderId={}", event.getOrderId());
            return;
        }

        // Delegate to LoyaltyService.refund() — already idempotent via refId check
        loyaltyService.refund(
                RefundRequest.builder()
                        .userId(event.getUserId())
                        .refId(String.valueOf(event.getOrderId()))
                        .description("Refund points for cancelled order")
                        .build(),
                event.getUserId()
        );
        log.info("Loyalty refund processed for orderId={} userId={}", event.getOrderId(), event.getUserId());
    }

    // ── DLT Handler ─────────────────────────────────────────────────────────

    @DltHandler
    public void handleDltMessage(
            String payload,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String receivedTopic,
            @Header(name = KafkaHeaders.DLT_ORIGINAL_TOPIC, required = false) String originalTopic,
            @Header(name = KafkaHeaders.DLT_ORIGINAL_PARTITION, required = false) Integer originalPartition,
            @Header(name = KafkaHeaders.DLT_ORIGINAL_OFFSET, required = false) Long originalOffset,
            @Header(name = KafkaHeaders.EXCEPTION_MESSAGE, required = false) String exceptionMessage
    ) {
        log.error(
                "Loyalty saga event routed to DLT receivedTopic={} originalTopic={} partition={} offset={} error={} payload={}",
                receivedTopic, originalTopic, originalPartition, originalOffset, exceptionMessage, payload
        );
    }

    // ── Private Helpers ─────────────────────────────────────────────────────

    private OrderCancelledEvent parseEvent(String payload) {
        try {
            return objectMapper.readValue(payload, OrderCancelledEvent.class);
        } catch (Exception e) {
            throw new IllegalStateException("Invalid OrderCancelledEvent payload", e);
        }
    }
}
