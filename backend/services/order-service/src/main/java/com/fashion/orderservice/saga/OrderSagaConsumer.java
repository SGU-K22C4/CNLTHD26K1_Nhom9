package com.fashion.orderservice.saga;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fashion.common.event.InventoryReservationResultEvent;
import com.fashion.common.event.OrderCancelledEvent;
import com.fashion.common.event.OrderItemEvent;
import com.fashion.common.event.PaymentResultEvent;
import com.fashion.common.event.SagaTopics;
import com.fashion.orderservice.entity.Order;
import com.fashion.orderservice.repository.OrderRepository;
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

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderSagaConsumer {

    private final ObjectMapper objectMapper;
    private final OrderRepository orderRepository;
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
    @KafkaListener(topics = SagaTopics.INVENTORY_RESERVATION_RESULT, groupId = "${spring.application.name}-inventory")
    @Transactional
    public void handleInventoryReservationResult(String payload) {
        InventoryReservationResultEvent event = parseInventoryEvent(payload);
        Order order = orderRepository.findById(event.getOrderId())
                .orElseThrow(() -> new IllegalStateException("Order not found for inventory result: " + event.getOrderId()));

        if (isTerminal(order.getStatus())) {
            log.info("Inventory result ignored for terminal orderId={} status={}", order.getId(), order.getStatus());
            return;
        }

        if (event.isSuccess()) {
            order.setInventoryReserved(true);
            orderRepository.save(order);
            return;
        }

        order.setInventoryReserved(false);
        if (order.getPaymentStatus() == Order.PaymentStatus.PAID) {
            log.error("REFUND NEEDED: payment success but inventory failed orderId={}", order.getId());
            saveIfChanged(order, Order.OrderStatus.CANCELLED, Order.PaymentStatus.PAID);
        } else {
            saveIfChanged(order, Order.OrderStatus.CANCELLED, resolveFailurePaymentStatus(order));
        }
    }

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
    @KafkaListener(topics = SagaTopics.PAYMENT_RESULT, groupId = "${spring.application.name}-payment")
    @Transactional
    public void handlePaymentResult(String payload) {
        PaymentResultEvent event = parsePaymentEvent(payload);
        Order order = orderRepository.findById(event.getOrderId())
                .orElseThrow(() -> new IllegalStateException("Order not found for payment result: " + event.getOrderId()));

        if (order.getPaymentMethod() == Order.PaymentMethod.COD) {
            return;
        }

        if (isTerminal(order.getStatus())) {
            log.info("Payment result ignored for terminal orderId={} status={}", order.getId(), order.getStatus());
            return;
        }

        if (event.isSuccess()) {
            if (Boolean.TRUE.equals(order.getInventoryReserved())) {
                saveIfChanged(order, Order.OrderStatus.CONFIRMED, Order.PaymentStatus.PAID);
            } else if (Boolean.FALSE.equals(order.getInventoryReserved())) {
                log.error("REFUND NEEDED: inventory already failed for paid orderId={}", order.getId());
                saveIfChanged(order, Order.OrderStatus.CANCELLED, Order.PaymentStatus.PAID);
            } else {
                order.setPaymentStatus(Order.PaymentStatus.PAID);
                orderRepository.save(order);
            }
            return;
        }

        if (Boolean.TRUE.equals(order.getInventoryReserved())) {
            publishOrderCancelledEvent(order, "Payment failed, rollback inventory");
        }
        saveIfChanged(order, Order.OrderStatus.CANCELLED, Order.PaymentStatus.FAILED);
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
                "Saga event routed to DLT receivedTopic={} originalTopic={} partition={} offset={} error={} payload={}",
                receivedTopic, originalTopic, originalPartition, originalOffset, exceptionMessage, payload
        );
    }

    private InventoryReservationResultEvent parseInventoryEvent(String payload) {
        try {
            return objectMapper.readValue(payload, InventoryReservationResultEvent.class);
        } catch (Exception e) {
            throw new IllegalStateException("Invalid InventoryReservationResultEvent payload", e);
        }
    }

    private PaymentResultEvent parsePaymentEvent(String payload) {
        try {
            return objectMapper.readValue(payload, PaymentResultEvent.class);
        } catch (Exception e) {
            throw new IllegalStateException("Invalid PaymentResultEvent payload", e);
        }
    }

    private boolean isTerminal(Order.OrderStatus status) {
        return status == Order.OrderStatus.CANCELLED
                || status == Order.OrderStatus.DELIVERED
                || status == Order.OrderStatus.RETURNED;
    }

    private Order.PaymentStatus resolveFailurePaymentStatus(Order order) {
        return order.getPaymentMethod() == Order.PaymentMethod.COD
                ? Order.PaymentStatus.UNPAID
                : Order.PaymentStatus.FAILED;
    }

    private void saveIfChanged(Order order, Order.OrderStatus targetStatus, Order.PaymentStatus targetPaymentStatus) {
        if (order.getStatus() == targetStatus && order.getPaymentStatus() == targetPaymentStatus) {
            return;
        }
        order.setStatus(targetStatus);
        order.setPaymentStatus(targetPaymentStatus);
        orderRepository.save(order);
    }

    private void publishOrderCancelledEvent(Order order, String reason) {
        try {
            List<OrderItemEvent> itemEvents = order.getItems().stream()
                    .map(item -> OrderItemEvent.builder()
                            .productId(item.getProductId())
                            .color(item.getColor())
                            .size(item.getSize())
                            .quantity(item.getQuantity())
                            .build())
                    .collect(Collectors.toList());

            sagaEventPublisher.publishOrderCancelled(OrderCancelledEvent.builder()
                    .orderId(order.getId())
                    .reason(reason)
                    .items(itemEvents)
                    .userId(order.getUserId())
                    .usedPoints(order.getUsedPoints())
                    .build());
        } catch (Exception e) {
            log.error("Failed to publish ORDER_CANCELLED for orderId={}", order.getId(), e);
        }
    }
}
