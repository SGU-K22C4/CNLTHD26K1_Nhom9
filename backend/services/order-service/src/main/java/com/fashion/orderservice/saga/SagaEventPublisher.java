package com.fashion.orderservice.saga;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fashion.common.event.OrderCancelledEvent;
import com.fashion.common.event.OrderCreatedEvent;
import com.fashion.common.event.OrderDeliveredEvent;
import com.fashion.common.event.PaymentResultEvent;
import com.fashion.common.event.SagaTopics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SagaEventPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public void publishOrderCreated(OrderCreatedEvent event) {
        publish(SagaTopics.ORDER_CREATED, String.valueOf(event.getOrderId()), event);
    }

    public void publishPaymentResult(PaymentResultEvent event) {
        publish(SagaTopics.PAYMENT_RESULT, String.valueOf(event.getOrderId()), event);
    }

    public void publishOrderCancelled(OrderCancelledEvent event) {
        publish(SagaTopics.ORDER_CANCELLED, String.valueOf(event.getOrderId()), event);
    }

    public void publishOrderDelivered(OrderDeliveredEvent event) {
        publish(SagaTopics.ORDER_DELIVERED, String.valueOf(event.getOrderId()), event);
    }

    private void publish(String topic, String key, Object payload) {
        try {
            String message = objectMapper.writeValueAsString(payload);
            kafkaTemplate.send(topic, key, message);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize saga event for topic {}", topic, e);
            throw new IllegalStateException("Cannot serialize saga event", e);
        }
    }
}
