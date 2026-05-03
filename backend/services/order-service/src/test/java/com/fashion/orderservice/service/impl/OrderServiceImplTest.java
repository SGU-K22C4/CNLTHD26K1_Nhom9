package com.fashion.orderservice.service.impl;

import com.fashion.orderservice.client.LoyaltyServiceClient;
import com.fashion.orderservice.client.ProductServiceClient;
import com.fashion.orderservice.client.dto.LoyaltyMutationResponse;
import com.fashion.orderservice.dto.request.OrderItemRequest;
import com.fashion.orderservice.dto.request.OrderRequest;
import com.fashion.orderservice.dto.response.OrderResponse;
import com.fashion.orderservice.entity.Order;
import com.fashion.orderservice.mapper.OrderMapper;
import com.fashion.orderservice.repository.OrderRepository;
import com.fashion.orderservice.saga.SagaEventPublisher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock private OrderRepository orderRepository;
    @Mock private ProductServiceClient productServiceClient;
    @Mock private LoyaltyServiceClient loyaltyServiceClient;
    @Mock private SagaEventPublisher sagaEventPublisher;
    @Mock private OrderMapper orderMapper;

    @InjectMocks
    private OrderServiceImpl orderService;

    @Test
    @DisplayName("Tạo đơn hàng thành công - Luồng cơ bản không dùng điểm")
    void createOrder_Success_NoLoyalty() {
        // Arrange
        OrderRequest request = new OrderRequest();
        request.setItems(List.of(createItemRequest("P1", 2, 100.0)));
        request.setPaymentMethod(Order.PaymentMethod.VNPAY);
        request.setShippingFee(BigDecimal.valueOf(20));

        when(productServiceClient.productExists("P1")).thenReturn(true);
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> {
            Order o = i.getArgument(0);
            o.setId(1L); // Giả lập DB sinh ID
            return o;
        });

        // Act
        orderService.createOrder("user123", request);

        // Assert
        verify(orderRepository, times(1)).save(any(Order.class));
        verify(sagaEventPublisher).publishOrderCreated(any());
        verify(loyaltyServiceClient, never()).redeemPoints(any(), any(), any(), anyInt());
    }

    @Test
    @DisplayName("Tạo đơn hàng thất bại - Sản phẩm không tồn tại")
    void createOrder_ProductNotFound() {
        // Arrange
        OrderRequest request = new OrderRequest();
        request.setItems(List.of(createItemRequest("INVALID", 1, 100.0)));
        when(productServiceClient.productExists("INVALID")).thenReturn(false);

        // Act & Assert
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, 
            () -> orderService.createOrder("user123", request));
        assertTrue(ex.getMessage().contains("Invalid productId"));
    }

    @Test
    @DisplayName("Tạo đơn hàng kèm dùng điểm - Thành công")
    void createOrder_WithLoyalty_Success() {
        // Arrange
        OrderRequest request = new OrderRequest();
        request.setItems(List.of(createItemRequest("P1", 1, 1000.0)));
        request.setUsedPoints(100);
        request.setPaymentMethod(Order.PaymentMethod.COD);

        when(productServiceClient.productExists("P1")).thenReturn(true);
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> {
            Order o = i.getArgument(0);
            o.setId(1L);
            return o;
        });

        LoyaltyMutationResponse loyaltyResp = new LoyaltyMutationResponse();
        loyaltyResp.setAppliedPoints(100);
        loyaltyResp.setDiscountAmount(BigDecimal.valueOf(50));
        when(loyaltyServiceClient.redeemPoints(anyString(), anyString(), any(), anyInt()))
                .thenReturn(loyaltyResp);

        // Act
        orderService.createOrder("user123", request);

        // Assert
        // Phải save 2 lần: lần 1 tạo order, lần 2 update tiền sau khi trừ điểm
        verify(orderRepository, times(2)).save(any(Order.class));
        verify(loyaltyServiceClient).redeemPoints(eq("user123"), anyString(), any(), eq(100));
    }

    @Test
    @DisplayName("Tạo đơn hàng - Lỗi đổi điểm dẫn đến Rollback xóa Order")
    void createOrder_LoyaltyFailed_RollbackOrder() {
        // Arrange
        OrderRequest request = new OrderRequest();
        request.setItems(List.of(createItemRequest("P1", 1, 500.0)));
        request.setUsedPoints(100);

        when(productServiceClient.productExists("P1")).thenReturn(true);
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> {
            Order o = i.getArgument(0);
            o.setId(99L);
            return o;
        });

        // Giả lập Loyalty Service ném lỗi
        when(loyaltyServiceClient.redeemPoints(any(), any(), any(), anyInt()))
                .thenThrow(new RuntimeException("Loyalty Service Down"));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> orderService.createOrder("user123", request));

        // Kiểm tra logic rollback thủ công của bạn
        verify(orderRepository).deleteById(99L);
    }

    @Test
    @DisplayName("Hủy đơn hàng thành công")
    void cancelOrder_Success() {
        // Arrange
        Order order = new Order();
        order.setId(1L);
        order.setUserId("user123");
        order.setStatus(Order.OrderStatus.PENDING);
        order.setUsedPoints(100);
        order.setItems(List.of());

        when(orderRepository.findByIdAndUserId(1L, "user123")).thenReturn(Optional.of(order));
        when(orderRepository.save(any())).thenReturn(order);

        // Act
        orderService.cancelOrder(1L, "user123");

        // Assert
        assertEquals(Order.OrderStatus.CANCELLED, order.getStatus());
        verify(loyaltyServiceClient).refundPoints("user123", "1");
        verify(sagaEventPublisher).publishOrderCancelled(any());
    }

    // Helper tạo data nhanh
    private OrderItemRequest createItemRequest(String id, int qty, double price) {
        OrderItemRequest item = new OrderItemRequest();
        item.setProductId(id);
        item.setQuantity(qty);
        item.setUnitPrice(BigDecimal.valueOf(price));
        return item;
    }
}