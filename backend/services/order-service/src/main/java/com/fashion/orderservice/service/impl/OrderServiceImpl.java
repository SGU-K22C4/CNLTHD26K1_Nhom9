package com.fashion.orderservice.service.impl;

import com.fashion.common.event.OrderCancelledEvent;
import com.fashion.common.event.OrderCreatedEvent;
import com.fashion.common.event.OrderDeliveredEvent;
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
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
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
        order.setPaymentStatus(resolveInitialPaymentStatus(order.getPaymentMethod()));

        Order savedOrder = orderRepository.save(order);
        if (requestedPoints > 0) {
            savedOrder = applyLoyaltyRedeem(savedOrder, effectiveUserId, requestedPoints);
        }

        if (savedOrder.getPaymentMethod() != Order.PaymentMethod.COD) {
            publishInventoryReservationEvent(savedOrder);
        }

        return orderMapper.toResponse(savedOrder);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponse> getUserOrders(String userId, Pageable pageable) {
        Page<Order> orderPage = orderRepository.findByUserId(userId, pageable);
        orderPage.getContent().forEach(this::initializeItems);
        return orderPage.map(orderMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponse> getAdminOrders(String keyword, String status, Pageable pageable) {
        Page<Order> orderPage = orderRepository.searchAdmin(normalizeKeyword(keyword), parseOrderStatus(status), pageable);
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
    public Optional<OrderResponse> getOrderByNumber(String orderNumber, String userId) {
        return orderRepository.findByOrderNumberAndUserId(orderNumber, userId)
                .map(this::initializeItems)
                .map(orderMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<OrderResponse> getOrderDetail(Long id, String userId) {
        return orderRepository.findByIdAndUserId(id, userId)
                .map(this::initializeItems)
                .map(orderMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<OrderResponse> getOrderDetailForAdmin(Long id) {
        return orderRepository.findById(id)
                .map(this::initializeItems)
                .map(orderMapper::toResponse);
    }

    @Override
    @Transactional
    public OrderResponse cancelOrder(Long id, String userId) {
        Order order = orderRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));
        return cancelPendingOrder(order, "User cancelled order");
    }

    @Override
    @Transactional
    public OrderResponse cancelOrderAsAdmin(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));
        return cancelPendingOrder(order, "Admin cancelled order");
    }

    @Override
    @Transactional
    public OrderResponse updateStatusAsAdmin(Long id, Order.OrderStatus targetStatus) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));

        applyAdminTransition(order, targetStatus);
        Order savedOrder = orderRepository.save(order);
        return orderMapper.toResponse(savedOrder);
    }

    private void applyAdminTransition(Order order, Order.OrderStatus targetStatus) {
        Order.OrderStatus currentStatus = order.getStatus();

        if (currentStatus == targetStatus) {
            return;
        }

        switch (targetStatus) {
            case CONFIRMED -> confirmOrder(order);
            case PROCESSING -> moveToProcessing(order);
            case SHIPPED -> moveToShipped(order);
            case DELIVERED -> moveToDelivered(order);
            default -> throw new IllegalArgumentException("Unsupported admin transition target: " + targetStatus);
        }
    }

    private void confirmOrder(Order order) {
        ensureStatus(order, Order.OrderStatus.PENDING, "Only pending orders can be confirmed");

        order.setStatus(Order.OrderStatus.CONFIRMED);
        if (order.getPaymentMethod() == Order.PaymentMethod.COD) {
            order.setPaymentStatus(Order.PaymentStatus.UNPAID);
            if (!Boolean.TRUE.equals(order.getInventoryReserved())) {
                publishInventoryReservationEvent(order);
            }
        }
    }

    private void moveToProcessing(Order order) {
        ensureStatus(order, Order.OrderStatus.CONFIRMED, "Only confirmed orders can move to processing");
        order.setStatus(Order.OrderStatus.PROCESSING);
    }

    private void moveToShipped(Order order) {
        ensureStatus(order, Order.OrderStatus.PROCESSING, "Only processing orders can be shipped");
        order.setStatus(Order.OrderStatus.SHIPPED);
    }

    private void moveToDelivered(Order order) {
        ensureStatus(order, Order.OrderStatus.SHIPPED, "Only shipped orders can be marked as delivered");
        order.setStatus(Order.OrderStatus.DELIVERED);
        if (order.getPaymentMethod() == Order.PaymentMethod.COD) {
            order.setPaymentStatus(Order.PaymentStatus.PAID);
        }
        publishOrderDeliveredEvent(order);
    }

    private void ensureStatus(Order order, Order.OrderStatus expected, String message) {
        if (order.getStatus() != expected) {
            throw new IllegalArgumentException(message);
        }
    }

    private Order.PaymentStatus resolveInitialPaymentStatus(Order.PaymentMethod paymentMethod) {
        return paymentMethod == Order.PaymentMethod.COD
                ? Order.PaymentStatus.UNPAID
                : Order.PaymentStatus.PENDING;
    }

    private String resolveUserId(String userId) {
        if (userId != null && !userId.isBlank()) {
            return userId;
        }
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
        order.setOrderNumber("ORD-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 6));
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

        Set<String> validatedProductIds = new HashSet<>();
        for (OrderItemRequest itemReq : itemRequests) {
            String productId = itemReq.getProductId();
            if (productId == null || productId.isBlank()) {
                throw new IllegalArgumentException("productId is required");
            }
            if (validatedProductIds.add(productId) && !productServiceClient.productExists(productId)) {
                throw new IllegalArgumentException("Invalid productId: " + productId);
            }
        }

        List<OrderItem> items = new ArrayList<>();
        for (OrderItemRequest itemReq : itemRequests) {
            String productId = itemReq.getProductId();
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
            item.setTotalPrice(itemReq.getUnitPrice().multiply(BigDecimal.valueOf(itemReq.getQuantity())));
            items.add(item);
        }
        return items;
    }

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
            orderRepository.deleteById(savedOrder.getId());
            throw ex;
        }
    }

    private void publishInventoryReservationEvent(Order order) {
        List<OrderItemEvent> itemEvents = toOrderItemEvents(order.getItems());
        sagaEventPublisher.publishOrderCreated(OrderCreatedEvent.builder()
                .orderId(order.getId())
                .orderNumber(order.getOrderNumber())
                .paymentMethod(order.getPaymentMethod().name())
                .items(itemEvents)
                .build());
    }

    private void publishOrderDeliveredEvent(Order order) {
        try {
            sagaEventPublisher.publishOrderDelivered(OrderDeliveredEvent.builder()
                    .orderId(order.getId())
                    .orderNumber(order.getOrderNumber())
                    .userId(order.getUserId())
                    .paymentMethod(order.getPaymentMethod().name())
                    .netAmount(order.getTotal())
                    .build());
        } catch (Exception e) {
            log.error("Failed to publish ORDER_DELIVERED for orderId={}", order.getId(), e);
        }
    }

    private void publishOrderCancelledEvent(Order order, String reason) {
        try {
            List<OrderItemEvent> itemEvents = toOrderItemEvents(order.getItems());
            sagaEventPublisher.publishOrderCancelled(OrderCancelledEvent.builder()
                    .orderId(order.getId())
                    .reason(reason)
                    .items(itemEvents)
                    .userId(order.getUserId())
                    .usedPoints(order.getUsedPoints())
                    .build());
        } catch (Exception e) {
            log.error("Failed to publish ORDER_CANCELLED for orderId={}", order.getId(), e);
        }
    }

    private List<OrderItemEvent> toOrderItemEvents(List<OrderItem> items) {
        return items.stream()
                .map(item -> OrderItemEvent.builder()
                        .productId(item.getProductId())
                        .color(item.getColor())
                        .size(item.getSize())
                        .quantity(item.getQuantity())
                        .build())
                .collect(Collectors.toList());
    }

    private Order initializeItems(Order order) {
        if (order.getItems() != null) {
            order.getItems().size();
        }
        return order;
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        return keyword.trim();
    }

    private Order.OrderStatus parseOrderStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return Order.OrderStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid order status: " + status);
        }
    }

    private OrderResponse cancelPendingOrder(Order order, String reason) {
        if (order.getStatus() != Order.OrderStatus.PENDING) {
            throw new IllegalArgumentException("Only PENDING orders can be cancelled");
        }
        order.setStatus(Order.OrderStatus.CANCELLED);
        order.setPaymentStatus(Order.PaymentStatus.FAILED);
        Order cancelledOrder = orderRepository.save(order);
        publishOrderCancelledEvent(cancelledOrder, reason);
        return orderMapper.toResponse(cancelledOrder);
    }
}
