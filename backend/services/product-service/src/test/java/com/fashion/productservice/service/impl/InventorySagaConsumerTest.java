package com.fashion.productservice.saga;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fashion.common.event.InventoryReservationResultEvent;
import com.fashion.common.event.OrderCreatedEvent;
import com.fashion.common.event.OrderItemEvent;
import com.fashion.productservice.entity.VariantSize;
import com.fashion.productservice.repository.saga.VariantSizeSagaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventorySagaConsumerTest {

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper(); // Dùng Spy để parse JSON thật

    @Mock
    private VariantSizeSagaRepository variantSizeSagaRepository;

    @Mock
    private SagaEventPublisher sagaEventPublisher;

    @InjectMocks
    private InventorySagaConsumer inventorySagaConsumer;

    @Test
    @DisplayName("Test trừ tồn kho thành công và bắn event Success")
    void testHandleOrderCreated_Success() throws Exception {
        // 1. Arrange (Chuẩn bị dữ liệu JSON giả lập từ Kafka)
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

        // Giả lập tìm thấy size trong DB với số lượng 10
        VariantSize mockSize = VariantSize.builder()
                .id("SIZE1")
                .quantity(10)
                .status("Con hang")
                .build();
        
        when(variantSizeSagaRepository.findForUpdate("PROD1", "Red", "M"))
                .thenReturn(List.of(mockSize));

        // 2. Act
        inventorySagaConsumer.handleOrderCreated(payload);

        // 3. Assert
        assertEquals(8, mockSize.getQuantity()); // 10 - 2 = 8
        verify(variantSizeSagaRepository).saveAll(any()); // Phải gọi saveAll
        
        // Kiểm tra xem có bắn event thành công lên Kafka không
        ArgumentCaptor<InventoryReservationResultEvent> eventCaptor = ArgumentCaptor.forClass(InventoryReservationResultEvent.class);
        verify(sagaEventPublisher).publishInventoryReservationResult(eventCaptor.capture());
        assertTrue(eventCaptor.getValue().isSuccess());
        assertEquals(100L, eventCaptor.getValue().getOrderId());
    }

    @Test
    @DisplayName("Test lỗi hết hàng - Phải bắn event Failure")
    void testHandleOrderCreated_InsufficientStock() throws Exception {
        // Arrange (Order 10 cái nhưng kho chỉ còn 5)
        OrderItemEvent item = OrderItemEvent.builder().productId("P1").quantity(10).size("M").build();
        OrderCreatedEvent event = OrderCreatedEvent.builder().orderId(101L).items(List.of(item)).build();
        String payload = objectMapper.writeValueAsString(event);

        VariantSize mockSize = VariantSize.builder().quantity(5).build();
        when(variantSizeSagaRepository.findForUpdateWithoutColor(anyString(), anyString()))
                .thenReturn(List.of(mockSize));

        // Act
        inventorySagaConsumer.handleOrderCreated(payload);

        // Assert
        verify(variantSizeSagaRepository, never()).saveAll(any()); // Không được save nếu lỗi
        
        ArgumentCaptor<InventoryReservationResultEvent> eventCaptor = ArgumentCaptor.forClass(InventoryReservationResultEvent.class);
        verify(sagaEventPublisher).publishInventoryReservationResult(eventCaptor.capture());
        assertFalse(eventCaptor.getValue().isSuccess());
        assertTrue(eventCaptor.getValue().getReason().contains("Insufficient inventory"));
    }

    @Test
    @DisplayName("Test không tìm thấy biến thể sản phẩm")
    void testHandleOrderCreated_NotFound() throws Exception {
        // Arrange
        OrderItemEvent item = OrderItemEvent.builder().productId("P1").size("M").build();
        OrderCreatedEvent event = OrderCreatedEvent.builder().orderId(102L).items(List.of(item)).build();
        String payload = objectMapper.writeValueAsString(event);

        when(variantSizeSagaRepository.findForUpdateWithoutColor(anyString(), anyString()))
                .thenReturn(List.of()); // Trả về list rỗng

        // Act
        inventorySagaConsumer.handleOrderCreated(payload);

        // Assert
        ArgumentCaptor<InventoryReservationResultEvent> eventCaptor = ArgumentCaptor.forClass(InventoryReservationResultEvent.class);
        verify(sagaEventPublisher).publishInventoryReservationResult(eventCaptor.capture());
        assertFalse(eventCaptor.getValue().isSuccess());
        assertTrue(eventCaptor.getValue().getReason().contains("Variant size not found"));
    }
}