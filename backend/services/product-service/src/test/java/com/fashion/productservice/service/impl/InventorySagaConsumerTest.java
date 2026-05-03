package com.fashion.productservice.saga;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fashion.common.event.InventoryReservationResultEvent;
import com.fashion.common.event.OrderCreatedEvent;
import com.fashion.common.event.OrderItemEvent;
import com.fashion.productservice.entity.VariantSize;
import com.fashion.productservice.repository.saga.VariantSizeSagaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventorySagaConsumerTest {

    @Mock
    private VariantSizeSagaRepository variantSizeSagaRepository;

    private ObjectMapper objectMapper;
    private CapturingSagaEventPublisher sagaEventPublisher;
    private InventorySagaConsumer inventorySagaConsumer;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        sagaEventPublisher = new CapturingSagaEventPublisher();
        inventorySagaConsumer = new InventorySagaConsumer(objectMapper, variantSizeSagaRepository, sagaEventPublisher);
    }

    @Test
    void should_ReserveInventoryAndPublishSuccessEvent_When_StockIsSufficient() throws Exception {
        OrderItemEvent item = OrderItemEvent.builder()
                .productId("PROD1")
                .color("Red")
                .size("M")
                .quantity(2)
                .build();
        OrderCreatedEvent event = OrderCreatedEvent.builder()
                .orderId(100L)
                .items(List.of(item))
                .build();
        String payload = objectMapper.writeValueAsString(event);

        VariantSize variantSize = VariantSize.builder()
                .id("SIZE1")
                .quantity(10)
                .status("Con hang")
                .build();

        when(variantSizeSagaRepository.findForUpdate("PROD1", "Red", "M")).thenReturn(List.of(variantSize));

        inventorySagaConsumer.handleOrderCreated(payload);

        assertEquals(8, variantSize.getQuantity());
        verify(variantSizeSagaRepository).saveAll(List.of(variantSize));
        assertNotNull(sagaEventPublisher.lastEvent);
        assertTrue(sagaEventPublisher.lastEvent.isSuccess());
        assertEquals(100L, sagaEventPublisher.lastEvent.getOrderId());
    }

    @Test
    void should_PublishFailureEvent_When_InventoryIsInsufficient() throws Exception {
        OrderItemEvent item = OrderItemEvent.builder()
                .productId("P1")
                .quantity(10)
                .size("M")
                .build();
        OrderCreatedEvent event = OrderCreatedEvent.builder()
                .orderId(101L)
                .items(List.of(item))
                .build();
        String payload = objectMapper.writeValueAsString(event);

        VariantSize variantSize = VariantSize.builder().quantity(5).build();
        when(variantSizeSagaRepository.findForUpdateWithoutColor(anyString(), anyString()))
                .thenReturn(List.of(variantSize));

        inventorySagaConsumer.handleOrderCreated(payload);

        verify(variantSizeSagaRepository, never()).saveAll(org.mockito.ArgumentMatchers.any());
        assertNotNull(sagaEventPublisher.lastEvent);
        assertFalse(sagaEventPublisher.lastEvent.isSuccess());
        assertTrue(sagaEventPublisher.lastEvent.getReason().contains("Insufficient inventory"));
    }

    @Test
    void should_PublishFailureEvent_When_VariantSizeDoesNotExist() throws Exception {
        OrderItemEvent item = OrderItemEvent.builder()
                .productId("P1")
                .size("M")
                .quantity(1)
                .build();
        OrderCreatedEvent event = OrderCreatedEvent.builder()
                .orderId(102L)
                .items(List.of(item))
                .build();
        String payload = objectMapper.writeValueAsString(event);

        when(variantSizeSagaRepository.findForUpdateWithoutColor(anyString(), anyString()))
                .thenReturn(List.of());

        inventorySagaConsumer.handleOrderCreated(payload);

        assertNotNull(sagaEventPublisher.lastEvent);
        assertFalse(sagaEventPublisher.lastEvent.isSuccess());
        assertTrue(sagaEventPublisher.lastEvent.getReason().contains("Variant size not found"));
    }

    private static class CapturingSagaEventPublisher extends SagaEventPublisher {
        private InventoryReservationResultEvent lastEvent;

        CapturingSagaEventPublisher() {
            super(null, new ObjectMapper());
        }

        @Override
        public void publishInventoryReservationResult(InventoryReservationResultEvent event) {
            this.lastEvent = event;
        }
    }
}
