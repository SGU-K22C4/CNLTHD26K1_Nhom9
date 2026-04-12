package com.fashion.productservice.saga;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fashion.common.event.InventoryReservationResultEvent;
import com.fashion.common.event.OrderCreatedEvent;
import com.fashion.common.event.OrderItemEvent;
import com.fashion.common.event.SagaTopics;
import com.fashion.productservice.entity.VariantSize;
import com.fashion.productservice.repository.saga.VariantSizeSagaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
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

    @KafkaListener(topics = SagaTopics.ORDER_CREATED, groupId = "${spring.application.name}-inventory")
    @Transactional
    public void handleOrderCreated(String payload) {
        try {
            OrderCreatedEvent event = objectMapper.readValue(payload, OrderCreatedEvent.class);
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
        } catch (Exception e) {
            log.error("Failed to handle OrderCreated saga event payload={}", payload, e);
            try {
                OrderCreatedEvent event = objectMapper.readValue(payload, OrderCreatedEvent.class);
                publishFailure(event.getOrderId(), "Inventory reservation exception: " + e.getMessage());
            } catch (Exception parseError) {
                log.error("Failed to parse payload for failure publish", parseError);
            }
        }
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
}
