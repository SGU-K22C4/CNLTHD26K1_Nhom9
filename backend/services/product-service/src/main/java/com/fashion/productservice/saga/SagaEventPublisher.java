package com.fashion.productservice.saga;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fashion.common.event.InventoryReservationResultEvent;
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

    public void publishInventoryReservationResult(InventoryReservationResultEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(SagaTopics.INVENTORY_RESERVATION_RESULT, String.valueOf(event.getOrderId()), payload);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize inventory reservation result", e);
            throw new IllegalStateException("Cannot serialize inventory reservation result", e);
        }
    }
}
