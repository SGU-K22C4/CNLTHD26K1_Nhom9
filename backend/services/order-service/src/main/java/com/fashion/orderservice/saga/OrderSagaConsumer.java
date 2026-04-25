package com.fashion.orderservice.saga;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fashion.common.event.InventoryReservationResultEvent;
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

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderSagaConsumer {

    private final ObjectMapper objectMapper;
    private final OrderRepository orderRepository;

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

        if (!event.isSuccess()) {
            if (order.getStatus() == Order.OrderStatus.CONFIRMED && order.getPaymentStatus() == Order.PaymentStatus.PAID) {
                log.warn("Ignore stale inventory failure for already confirmed orderId={}", event.getOrderId());
                return;
            }

            Order.PaymentStatus targetPaymentStatus =
                    order.getPaymentStatus() == Order.PaymentStatus.PAID
                            ? Order.PaymentStatus.PAID
                            : Order.PaymentStatus.FAILED;
            saveIfChanged(order, Order.OrderStatus.CANCELLED, targetPaymentStatus);
            return;
        }

        if (order.getStatus() == Order.OrderStatus.CANCELLED || order.getPaymentStatus() == Order.PaymentStatus.FAILED) {
            log.warn("Ignore stale inventory success for cancelled/failed orderId={}", event.getOrderId());
            return;
        }

        if (order.getPaymentMethod() == Order.PaymentMethod.COD) {
            saveIfChanged(order, Order.OrderStatus.CONFIRMED, Order.PaymentStatus.PAID);
            return;
        }

        if (order.getStatus() == Order.OrderStatus.CONFIRMED && order.getPaymentStatus() == Order.PaymentStatus.PAID) {
            log.info("Inventory success received after payment confirmed. Skip orderId={}", event.getOrderId());
            return;
        }

        saveIfChanged(order, Order.OrderStatus.PENDING, Order.PaymentStatus.PENDING);
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

        if (event.isSuccess()) {
            if (order.getStatus() == Order.OrderStatus.CANCELLED || order.getPaymentStatus() == Order.PaymentStatus.FAILED) {
                log.warn("Ignore stale payment success for cancelled/failed orderId={}", event.getOrderId());
                return;
            }
            saveIfChanged(order, Order.OrderStatus.CONFIRMED, Order.PaymentStatus.PAID);
            return;
        }

        if (order.getPaymentStatus() == Order.PaymentStatus.PAID && order.getStatus() == Order.OrderStatus.CONFIRMED) {
            log.info("Ignore stale payment failure for already paid orderId={}", event.getOrderId());
            return;
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

    private void saveIfChanged(Order order, Order.OrderStatus targetStatus, Order.PaymentStatus targetPaymentStatus) {
        if (order.getStatus() == targetStatus && order.getPaymentStatus() == targetPaymentStatus) {
            return;
        }
        order.setStatus(targetStatus);
        order.setPaymentStatus(targetPaymentStatus);
        orderRepository.save(order);
    }
}
