package com.fashion.productservice.saga;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fashion.common.event.InventoryReservationResultEvent;
import com.fashion.common.event.OrderCancelledEvent;
import com.fashion.common.event.OrderCreatedEvent;
import com.fashion.common.event.OrderItemEvent;
import com.fashion.common.event.SagaTopics;
import com.fashion.productservice.entity.VariantSize;
import com.fashion.productservice.repository.saga.VariantSizeSagaRepository;
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

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class InventorySagaConsumer {

    private final ObjectMapper objectMapper;
    private final VariantSizeSagaRepository variantSizeSagaRepository;
    private final SagaEventPublisher sagaEventPublisher;

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
    @KafkaListener(topics = SagaTopics.ORDER_CREATED, groupId = "${spring.application.name}-inventory")
    @Transactional
    public void handleOrderCreated(String payload) {
        OrderCreatedEvent event = parseOrderCreatedEvent(payload);
        if (event.getOrderId() == null) {
            throw new IllegalStateException("orderId is required in OrderCreatedEvent");
        }
        if (event.getItems() == null || event.getItems().isEmpty()) {
            publishFailure(event.getOrderId(), "Order items are empty");
            return;
        }

        List<VariantSize> reservedRows = new ArrayList<>();
        for (OrderItemEvent item : event.getItems()) {
            VariantSize size = findVariantSizeForUpdate(item);
            if (size == null) {
                publishFailure(event.getOrderId(), "Variant size not found for productId=" + item.getProductId());
                return;
            }

            if (size.getQuantity() < item.getQuantity()) {
                publishFailure(event.getOrderId(), "Insufficient inventory for productId=" + item.getProductId());
                return;
            }

            reservedRows.add(size);
        }

        for (int i = 0; i < event.getItems().size(); i++) {
            VariantSize size = reservedRows.get(i);
            OrderItemEvent item = event.getItems().get(i);
            size.setQuantity(size.getQuantity() - item.getQuantity());
            if (size.getQuantity() <= 0) {
                size.setStatus("Het hang");
            }
        }

        variantSizeSagaRepository.saveAll(reservedRows);
        sagaEventPublisher.publishInventoryReservationResult(InventoryReservationResultEvent.builder()
                .orderId(event.getOrderId())
                .success(true)
                .reason(null)
                .build());
    }

    // ── ORDER_CANCELLED → Restore inventory ─────────────────────────────────
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
    @KafkaListener(topics = SagaTopics.ORDER_CANCELLED, groupId = "${spring.application.name}-inventory-restore")
    @Transactional
    public void handleOrderCancelled(String payload) {
        OrderCancelledEvent event = parseOrderCancelledEvent(payload);
        if (event.getOrderId() == null) {
            log.warn("Ignoring ORDER_CANCELLED event with null orderId");
            return;
        }
        if (event.getItems() == null || event.getItems().isEmpty()) {
            log.info("ORDER_CANCELLED orderId={} has no items, skipping inventory restore", event.getOrderId());
            return;
        }

        log.info("Restoring inventory for cancelled orderId={}", event.getOrderId());

        for (OrderItemEvent item : event.getItems()) {
            VariantSize size = findVariantSizeForUpdate(item);
            if (size == null) {
                log.warn("Cannot restore inventory: variant size not found for productId={}, color={}, size={}",
                        item.getProductId(), item.getColor(), item.getSize());
                continue;
            }
            size.setQuantity(size.getQuantity() + item.getQuantity());
            if (size.getQuantity() > 0) {
                size.setStatus("Con hang");
            }
            variantSizeSagaRepository.save(size);
        }

        log.info("Inventory restored successfully for cancelled orderId={}", event.getOrderId());
    }

    private VariantSize findVariantSizeForUpdate(OrderItemEvent item) {
        String color = item.getColor() == null ? "" : item.getColor().trim();
        String size = item.getSize() == null ? "" : item.getSize().trim();

        if (!color.isBlank()) {
            return variantSizeSagaRepository.findForUpdate(item.getProductId(), color, size)
                    .stream()
                    .findFirst()
                    .orElse(null);
        }

        return variantSizeSagaRepository.findForUpdateWithoutColor(item.getProductId(), size)
                .stream()
                .findFirst()
                .orElse(null);
    }

    private void publishFailure(Long orderId, String reason) {
        sagaEventPublisher.publishInventoryReservationResult(InventoryReservationResultEvent.builder()
                .orderId(orderId)
                .success(false)
                .reason(reason)
                .build());
    }

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
                "Inventory saga event routed to DLT receivedTopic={} originalTopic={} partition={} offset={} error={} payload={}",
                receivedTopic, originalTopic, originalPartition, originalOffset, exceptionMessage, payload
        );
    }

    private OrderCreatedEvent parseOrderCreatedEvent(String payload) {
        try {
            return objectMapper.readValue(payload, OrderCreatedEvent.class);
        } catch (Exception e) {
            throw new IllegalStateException("Invalid OrderCreatedEvent payload", e);
        }
    }

    private OrderCancelledEvent parseOrderCancelledEvent(String payload) {
        try {
            return objectMapper.readValue(payload, OrderCancelledEvent.class);
        } catch (Exception e) {
            throw new IllegalStateException("Invalid OrderCancelledEvent payload", e);
        }
    }
}

