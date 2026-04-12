package com.fashion.orderservice.saga;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fashion.common.event.InventoryReservationResultEvent;
import com.fashion.common.event.PaymentResultEvent;
import com.fashion.common.event.SagaTopics;
import com.fashion.orderservice.entity.Order;
import com.fashion.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderSagaConsumer {

    private final ObjectMapper objectMapper;
    private final OrderRepository orderRepository;

    @KafkaListener(topics = SagaTopics.INVENTORY_RESERVATION_RESULT, groupId = "${spring.application.name}-inventory")
    @Transactional
    public void handleInventoryReservationResult(String payload) {
        try {
            InventoryReservationResultEvent event = objectMapper.readValue(payload, InventoryReservationResultEvent.class);
            Order order = orderRepository.findById(event.getOrderId()).orElse(null);
            if (order == null) {
                log.warn("Order not found for inventory result: {}", event.getOrderId());
                return;
            }

            if (!event.isSuccess()) {
                if (order.getStatus() != Order.OrderStatus.CANCELLED) {
                    order.setStatus(Order.OrderStatus.CANCELLED);
                }
                if (order.getPaymentStatus() != Order.PaymentStatus.PAID) {
                    order.setPaymentStatus(Order.PaymentStatus.FAILED);
                }
                orderRepository.save(order);
                return;
            }

            if (order.getPaymentMethod() == Order.PaymentMethod.COD) {
                order.setStatus(Order.OrderStatus.CONFIRMED);
                order.setPaymentStatus(Order.PaymentStatus.PAID);
            } else {
                order.setStatus(Order.OrderStatus.PENDING);
                order.setPaymentStatus(Order.PaymentStatus.PENDING);
            }
            orderRepository.save(order);
        } catch (Exception e) {
            log.error("Failed to handle inventory reservation result payload={}", payload, e);
        }
    }

    @KafkaListener(topics = SagaTopics.PAYMENT_RESULT, groupId = "${spring.application.name}-payment")
    @Transactional
    public void handlePaymentResult(String payload) {
        try {
            PaymentResultEvent event = objectMapper.readValue(payload, PaymentResultEvent.class);
            Order order = orderRepository.findById(event.getOrderId()).orElse(null);
            if (order == null) {
                log.warn("Order not found for payment result: {}", event.getOrderId());
                return;
            }

            if (order.getPaymentMethod() == Order.PaymentMethod.COD) {
                return;
            }

            if (event.isSuccess()) {
                if (order.getPaymentStatus() == Order.PaymentStatus.PAID) {
                    return;
                }
                order.setStatus(Order.OrderStatus.CONFIRMED);
                order.setPaymentStatus(Order.PaymentStatus.PAID);
            } else {
                order.setStatus(Order.OrderStatus.CANCELLED);
                order.setPaymentStatus(Order.PaymentStatus.FAILED);
            }
            orderRepository.save(order);
        } catch (Exception e) {
            log.error("Failed to handle payment result payload={}", payload, e);
        }
    }
}
