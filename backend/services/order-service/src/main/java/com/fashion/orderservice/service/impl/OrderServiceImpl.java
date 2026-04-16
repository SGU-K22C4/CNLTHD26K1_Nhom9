package com.fashion.orderservice.service.impl;

import com.fashion.common.event.OrderCancelledEvent;
import com.fashion.common.event.OrderCreatedEvent;
import com.fashion.common.event.OrderItemEvent;
import com.fashion.orderservice.client.LoyaltyServiceClient;
import com.fashion.orderservice.client.ProductServiceClient;
import com.fashion.orderservice.client.dto.LoyaltyMutationResponse;
import com.fashion.orderservice.dto.request.OrderItemRequest;
import com.fashion.orderservice.dto.request.OrderRequest;
import com.fashion.orderservice.dto.response.OrderResponse;
import com.fashion.orderservice.entity.Order;
import com.fashion.orderservice.entity.OrderItem;
import com.fashion.orderservice.mapper.OrderMapper;
import com.fashion.orderservice.repository.OrderRepository;
import com.fashion.orderservice.saga.SagaEventPublisher;
import com.fashion.orderservice.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final ProductServiceClient productServiceClient;
    private final LoyaltyServiceClient loyaltyServiceClient;
    private final SagaEventPublisher sagaEventPublisher;
    private final OrderMapper orderMapper;

    // ── Create Order ──────────────────────────────────────────────────────────

    /**
     * Creates a new order:
     * 1. Resolves userId (authenticated user or guest)
     * 2. Validates all product IDs via Product Service REST API
     * 3. Builds Order + OrderItems and calculates totals
     * 4. Redeems loyalty points if requested
     * 5. Publishes ORDER_CREATED Kafka event to start the Saga flow
     *
     * @throws IllegalArgumentException for validation errors (bad input, invalid
     *                                  product, etc.)
     * @throws IllegalStateException    if an external service (Product, Loyalty) is
     *                                  unavailable
     */
    @Override
    @Transactional
    public OrderResponse createOrder(String userId, OrderRequest request) {
        String effectiveUserId = resolveUserId(userId);
        int requestedPoints = request.getUsedPoints() == null ? 0 : request.getUsedPoints();

        validatePointsRequest(requestedPoints, userId);

        Order order = buildOrder(effectiveUserId, request);
        List<OrderItem> orderItems = buildOrderItems(order, request.getItems());

        BigDecimal subtotal = orderItems.stream()
                .map(OrderItem::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        order.setSubtotal(subtotal);
        order.setTotal(subtotal.add(order.getShippingFee()).subtract(order.getDiscount()));
        order.setItems(orderItems);
        order.setStatus(Order.OrderStatus.PENDING);
        order.setPaymentStatus(Order.PaymentStatus.PENDING);

        Order savedOrder = orderRepository.save(order);

        if (requestedPoints > 0) {
            savedOrder = applyLoyaltyRedeem(savedOrder, effectiveUserId, requestedPoints);
        }

        publishOrderCreatedEvent(savedOrder);
        return orderMapper.toResponse(savedOrder);
    }

    // ── Query Orders ──────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponse> getUserOrders(String userId, Pageable pageable) {
        Page<Order> orderPage = orderRepository.findByUserId(userId, pageable);
        orderPage.getContent().forEach(this::initializeItems);
        return orderPage.map(orderMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<OrderResponse> getOrderForUser(Long id, String userId) {
        return orderRepository.findByIdAndUserId(id, userId)
                .map(this::initializeItems)
                .map(orderMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<OrderResponse> getOrderByNumber(String orderNumber) {
        return orderRepository.findByOrderNumber(orderNumber)
                .map(this::initializeItems)
                .map(orderMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<OrderResponse> getOrderDetail(Long id) {
        return orderRepository.findById(id)
                .map(this::initializeItems)
                .map(orderMapper::toResponse);
    }

    // ── Cancel Order ──────────────────────────────────────────────────────────

    /**
     * Cancels a PENDING order:
     * 1. Refunds loyalty points if any were used
     * 2. Sets order status to CANCELLED
     * 3. Publishes ORDER_CANCELLED event for VNPAY orders to restore inventory
     *
     * @throws IllegalArgumentException if loyalty refund fails due to bad data
     * @throws IllegalStateException    if Loyalty Service is unavailable
     */
    @Override
    @Transactional
    public OrderResponse cancelOrder(Long id, String userId) {
        Order order = orderRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));

        if (order.getStatus() != Order.OrderStatus.PENDING) {
            throw new IllegalArgumentException("Only PENDING orders can be cancelled");
        }

        // Refund loyalty points if any were used
        if (order.getUsedPoints() != null && order.getUsedPoints() > 0) {
            loyaltyServiceClient.refundPoints(order.getUserId(), String.valueOf(order.getId()));
        }

        order.setStatus(Order.OrderStatus.CANCELLED);
        Order cancelledOrder = orderRepository.save(order);

        // Compensation: restore inventory for any order that had stock reserved
        publishOrderCancelledEvent(cancelledOrder, "User cancelled order");

        return orderMapper.toResponse(cancelledOrder);
    }

    // ── Private Helpers ───────────────────────────────────────────────────────

    private String resolveUserId(String userId) {
        if (userId != null && !userId.isBlank()) {
            return userId;
        }
        // "guest-" (6 chars) + UUID (30 chars) = 36 characters total to fit VARCHAR(36)
        return ("guest-" + UUID.randomUUID().toString()).substring(0, 36);
    }

    private void validatePointsRequest(int requestedPoints, String userId) {
        if (requestedPoints < 0) {
            throw new IllegalArgumentException("usedPoints must be non-negative");
        }
        if (requestedPoints > 0 && (userId == null || userId.isBlank() || userId.startsWith("guest-"))) {
            throw new IllegalArgumentException("Guest user cannot redeem loyalty points");
        }
    }

    private Order buildOrder(String effectiveUserId, OrderRequest request) {
        Order order = new Order();
        order.setUserId(effectiveUserId);
        order.setOrderNumber("ORD-" + System.currentTimeMillis());
        order.setRecipientName(request.getRecipientName());
        order.setRecipientPhone(request.getRecipientPhone());
        order.setShippingAddress(request.getShippingAddress());
        order.setPaymentMethod(request.getPaymentMethod());
        order.setNote(request.getNote());
        order.setCouponCode(request.getCouponCode());
        order.setDiscount(request.getDiscount() != null ? request.getDiscount() : BigDecimal.ZERO);
        order.setShippingFee(request.getShippingFee() != null ? request.getShippingFee() : BigDecimal.ZERO);
        order.setLoyaltyDiscount(BigDecimal.ZERO);
        order.setUsedPoints(0);
        return order;
    }

    private List<OrderItem> buildOrderItems(Order order, List<OrderItemRequest> itemRequests) {
        if (itemRequests == null || itemRequests.isEmpty()) {
            return new ArrayList<>();
        }

        List<OrderItem> items = new ArrayList<>();
        for (OrderItemRequest itemReq : itemRequests) {
            String productId = itemReq.getProductId();
            if (productId == null || productId.isBlank()) {
                throw new IllegalArgumentException("productId is required");
            }

            // Validate product existence via REST call to Product Service
            if (!productServiceClient.productExists(productId)) {
                throw new IllegalArgumentException("Invalid productId: " + productId);
            }

            if (itemReq.getQuantity() <= 0) {
                throw new IllegalArgumentException("quantity must be greater than 0");
            }
            if (itemReq.getUnitPrice() == null) {
                throw new IllegalArgumentException("unitPrice is required");
            }

            OrderItem item = new OrderItem();
            item.setOrder(order);
            item.setProductId(productId);
            item.setProductName(itemReq.getProductName());
            item.setProductSlug(itemReq.getProductSlug());
            item.setImageUrl(itemReq.getImageUrl());
            item.setColor(itemReq.getColor());
            item.setSize(itemReq.getSize());
            item.setQuantity(itemReq.getQuantity());
            item.setUnitPrice(itemReq.getUnitPrice());

            BigDecimal itemTotal = itemReq.getUnitPrice().multiply(BigDecimal.valueOf(itemReq.getQuantity()));
            item.setTotalPrice(itemTotal);

            items.add(item);
        }
        return items;
    }

    /**
     * Redeems loyalty points for the order. Rolls back the order if redeem fails.
     */
    private Order applyLoyaltyRedeem(Order savedOrder, String userId, int requestedPoints) {
        try {
            LoyaltyMutationResponse redeem = loyaltyServiceClient.redeemPoints(
                    userId,
                    String.valueOf(savedOrder.getId()),
                    savedOrder.getTotal(),
                    requestedPoints);

            int appliedPoints = redeem != null && redeem.getAppliedPoints() != null ? redeem.getAppliedPoints() : 0;
            BigDecimal loyaltyDiscount = redeem != null && redeem.getDiscountAmount() != null
                    ? redeem.getDiscountAmount()
                    : BigDecimal.ZERO;

            if (appliedPoints <= 0 || loyaltyDiscount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Requested points are not applicable");
            }

            BigDecimal finalTotal = savedOrder.getTotal().subtract(loyaltyDiscount);
            if (finalTotal.compareTo(BigDecimal.ZERO) < 0) {
                finalTotal = BigDecimal.ZERO;
            }

            savedOrder.setUsedPoints(appliedPoints);
            savedOrder.setLoyaltyDiscount(loyaltyDiscount);
            savedOrder.setTotal(finalTotal);
            return orderRepository.save(savedOrder);

        } catch (RuntimeException ex) {
            // Rollback: delete the order that was just created since loyalty redeem failed
            orderRepository.deleteById(savedOrder.getId());
            throw ex;
        }
    }

    private void publishOrderCreatedEvent(Order savedOrder) {
        List<OrderItemEvent> itemEvents = savedOrder.getItems().stream()
                .map(item -> OrderItemEvent.builder()
                        .productId(item.getProductId())
                        .color(item.getColor())
                        .size(item.getSize())
                        .quantity(item.getQuantity())
                        .build())
                .collect(Collectors.toList());

        sagaEventPublisher.publishOrderCreated(OrderCreatedEvent.builder()
                .orderId(savedOrder.getId())
                .orderNumber(savedOrder.getOrderNumber())
                .paymentMethod(savedOrder.getPaymentMethod().name())
                .items(itemEvents)
                .build());
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
                    .build());
        } catch (Exception e) {
            log.error("Failed to publish ORDER_CANCELLED for orderId={}", order.getId(), e);
        }
    }

    private Order initializeItems(Order order) {
        if (order.getItems() != null) {
            order.getItems().size();
        }
        return order;
    }
}
