package com.fashion.orderservice.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderMapper orderMapper;

    private StubProductServiceClient productServiceClient;
    private StubLoyaltyServiceClient loyaltyServiceClient;
    private CapturingSagaEventPublisher sagaEventPublisher;
    private OrderServiceImpl orderService;

    @BeforeEach
    void setUp() {
        productServiceClient = new StubProductServiceClient();
        loyaltyServiceClient = new StubLoyaltyServiceClient();
        sagaEventPublisher = new CapturingSagaEventPublisher();
        orderService = new OrderServiceImpl(
                orderRepository,
                productServiceClient,
                loyaltyServiceClient,
                sagaEventPublisher,
                orderMapper
        );
    }

    @Test
    void should_CreateOrderWithoutLoyalty_When_RequestIsValid() {
        OrderRequest request = createOrderRequest(List.of(createItemRequest("P1", 2, 100.0)));
        request.setShippingFee(BigDecimal.valueOf(20));
        productServiceClient.existingProductIds.add("P1");

        OrderResponse expectedResponse = OrderResponse.builder().id(1L).orderNumber("ORD-1").build();

        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            if (order.getId() == null) {
                order.setId(1L);
                order.setOrderNumber("ORD-1");
            }
            return order;
        });
        when(orderMapper.toResponse(any(Order.class))).thenReturn(expectedResponse);

        OrderResponse response = orderService.createOrder("user123", request);

        assertEquals(1L, response.getId());
        assertEquals("ORD-1", response.getOrderNumber());
        assertNotNull(sagaEventPublisher.createdEvent);
        assertEquals(1L, sagaEventPublisher.createdEvent.getOrderId());
        assertEquals(0, loyaltyServiceClient.redeemCallCount);
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    void should_ThrowIllegalArgumentException_When_ProductDoesNotExist() {
        OrderRequest request = createOrderRequest(List.of(createItemRequest("INVALID", 1, 100.0)));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> orderService.createOrder("user123", request));

        assertEquals("Invalid productId: INVALID", exception.getMessage());
    }

    @Test
    void should_CreateOrderWithLoyalty_When_RedeemSucceeds() {
        OrderRequest request = createOrderRequest(List.of(createItemRequest("P1", 1, 1000.0)));
        request.setPaymentMethod(Order.PaymentMethod.COD);
        request.setUsedPoints(100);
        productServiceClient.existingProductIds.add("P1");

        LoyaltyMutationResponse loyaltyResponse = new LoyaltyMutationResponse();
        loyaltyResponse.setAppliedPoints(100);
        loyaltyResponse.setDiscountAmount(BigDecimal.valueOf(50));
        loyaltyServiceClient.redeemResponse = loyaltyResponse;

        OrderResponse expectedResponse = OrderResponse.builder().id(1L).total(BigDecimal.valueOf(950)).build();

        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            if (order.getId() == null) {
                order.setId(1L);
            }
            return order;
        });
        when(orderMapper.toResponse(any(Order.class))).thenReturn(expectedResponse);

        OrderResponse response = orderService.createOrder("user123", request);

        assertEquals(BigDecimal.valueOf(950), response.getTotal());
        assertEquals("user123", loyaltyServiceClient.lastRedeemUserId);
        assertEquals(100, loyaltyServiceClient.lastRequestedPoints);
    }

    @Test
    void should_RollbackOrder_When_LoyaltyRedeemFails() {
        OrderRequest request = createOrderRequest(List.of(createItemRequest("P1", 1, 500.0)));
        request.setUsedPoints(100);
        productServiceClient.existingProductIds.add("P1");
        loyaltyServiceClient.redeemException = new RuntimeException("Loyalty Service Down");

        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(99L);
            return order;
        });

        assertThrows(RuntimeException.class, () -> orderService.createOrder("user123", request));

        verify(orderRepository).deleteById(99L);
    }

    @Test
    void should_ThrowIllegalArgumentException_When_GuestUserRedeemsPoints() {
        OrderRequest request = createOrderRequest(List.of(createItemRequest("P1", 1, 500.0)));
        request.setUsedPoints(10);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> orderService.createOrder("", request));

        assertEquals("Guest user cannot redeem loyalty points", exception.getMessage());
    }

    @Test
    void should_CancelPendingOrderAndRefundPoints_When_OrderIsPending() {
        Order order = new Order();
        order.setId(1L);
        order.setUserId("user123");
        order.setStatus(Order.OrderStatus.PENDING);
        order.setUsedPoints(100);
        order.setItems(List.of());

        OrderResponse expectedResponse = OrderResponse.builder().id(1L).status("CANCELLED").build();

        when(orderRepository.findByIdAndUserId(1L, "user123")).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenReturn(order);
        when(orderMapper.toResponse(order)).thenReturn(expectedResponse);

        OrderResponse response = orderService.cancelOrder(1L, "user123");

        assertEquals("CANCELLED", response.getStatus());
        assertNotNull(sagaEventPublisher.cancelledEvent);
        assertEquals(1L, sagaEventPublisher.cancelledEvent.getOrderId());
        assertEquals("user123", sagaEventPublisher.cancelledEvent.getUserId());
        assertEquals(100, sagaEventPublisher.cancelledEvent.getUsedPoints());
    }

    @Test
    void should_ThrowIllegalArgumentException_When_CancellingNonPendingOrder() {
        Order order = new Order();
        order.setId(1L);
        order.setUserId("user123");
        order.setStatus(Order.OrderStatus.CANCELLED);

        when(orderRepository.findByIdAndUserId(1L, "user123")).thenReturn(Optional.of(order));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> orderService.cancelOrder(1L, "user123"));

        assertEquals("Only PENDING orders can be cancelled", exception.getMessage());
    }

    private OrderRequest createOrderRequest(List<OrderItemRequest> items) {
        OrderRequest request = new OrderRequest();
        request.setRecipientName("John Doe");
        request.setRecipientPhone("0123456789");
        request.setShippingAddress("123 Test Street");
        request.setPaymentMethod(Order.PaymentMethod.VNPAY);
        request.setItems(items);
        return request;
    }

    private OrderItemRequest createItemRequest(String productId, int quantity, double price) {
        OrderItemRequest item = new OrderItemRequest();
        item.setProductId(productId);
        item.setProductName("Product " + productId);
        item.setColor("Red");
        item.setSize("M");
        item.setQuantity(quantity);
        item.setUnitPrice(BigDecimal.valueOf(price));
        return item;
    }

    private static class StubProductServiceClient extends ProductServiceClient {
        private final Set<String> existingProductIds = new HashSet<>();

        StubProductServiceClient() {
            super(null);
        }

        @Override
        public boolean productExists(String productId) {
            return existingProductIds.contains(productId);
        }
    }

    private static class StubLoyaltyServiceClient extends LoyaltyServiceClient {
        private LoyaltyMutationResponse redeemResponse;
        private RuntimeException redeemException;
        private String lastRedeemUserId;
        private Integer lastRequestedPoints;
        private int redeemCallCount;
        private String lastRefundUserId;
        private String lastRefundOrderId;

        StubLoyaltyServiceClient() {
            super(null);
        }

        @Override
        public LoyaltyMutationResponse redeemPoints(String userId, String orderId, BigDecimal orderAmount, Integer requestedPoints) {
            redeemCallCount++;
            lastRedeemUserId = userId;
            lastRequestedPoints = requestedPoints;
            if (redeemException != null) {
                throw redeemException;
            }
            return redeemResponse;
        }

        @Override
        public LoyaltyMutationResponse refundPoints(String userId, String orderId) {
            lastRefundUserId = userId;
            lastRefundOrderId = orderId;
            return new LoyaltyMutationResponse();
        }
    }

    private static class CapturingSagaEventPublisher extends SagaEventPublisher {
        private com.fashion.common.event.OrderCreatedEvent createdEvent;
        private com.fashion.common.event.OrderCancelledEvent cancelledEvent;

        CapturingSagaEventPublisher() {
            super(null, new ObjectMapper());
        }

        @Override
        public void publishOrderCreated(com.fashion.common.event.OrderCreatedEvent event) {
            this.createdEvent = event;
        }

        @Override
        public void publishOrderCancelled(com.fashion.common.event.OrderCancelledEvent event) {
            this.cancelledEvent = event;
        }
    }
}
