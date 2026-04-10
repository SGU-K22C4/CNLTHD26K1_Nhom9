package com.fashion.orderservice.controller;

import com.fashion.common.event.OrderCreatedEvent;
import com.fashion.common.event.OrderItemEvent;
import com.fashion.orderservice.client.LoyaltyServiceClient;
import com.fashion.orderservice.client.dto.LoyaltyMutationResponse;
import com.fashion.orderservice.entity.Order;
import com.fashion.orderservice.repository.OrderRepository;
import com.fashion.orderservice.saga.SagaEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import com.fashion.orderservice.dto.request.OrderRequest;
import com.fashion.orderservice.dto.request.OrderItemRequest;
import com.fashion.orderservice.entity.OrderItem;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.UUID;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@SuppressWarnings("null")
public class OrderController {

    private final OrderRepository orderRepository;
    private final JdbcTemplate jdbcTemplate;
    private final SagaEventPublisher sagaEventPublisher;
    private final LoyaltyServiceClient loyaltyServiceClient;
    private final SagaEventPublisher sagaEventPublisher;

    @PostMapping
    public ResponseEntity<?> createOrder(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestBody OrderRequest request) {

        // "guest-" (6 chars) + UUID (30 chars) = 36 characters total to fit VARCHAR(36)
        String effectiveUserId = userId != null ? userId : ("guest-" + UUID.randomUUID().toString()).substring(0, 36);

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

        int requestedPoints = request.getUsedPoints() == null ? 0 : request.getUsedPoints();
        if (requestedPoints < 0) {
            return ResponseEntity.badRequest().body(Map.of("error", "usedPoints must be non-negative"));
        }
        if (requestedPoints > 0 && (userId == null || userId.isBlank() || userId.startsWith("guest-"))) {
            return ResponseEntity.badRequest().body(Map.of("error", "Guest user cannot redeem loyalty points"));
        }

        BigDecimal subtotal = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>();

        if (request.getItems() != null) {
            for (OrderItemRequest itemReq : request.getItems()) {
                String productId = itemReq.getProductId();
                if (productId == null || productId.isBlank()) {
                    return ResponseEntity.badRequest().body(Map.of("error", "productId is required"));
                }

                Boolean exists = jdbcTemplate.queryForObject(
                        "SELECT EXISTS(SELECT 1 FROM fashion_product_db.products WHERE id = ?)",
                        Boolean.class,
                        productId);
                if (!Boolean.TRUE.equals(exists)) {
                    return ResponseEntity.badRequest().body(Map.of(
                            "error", "Invalid productId",
                            "productId", productId));
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

                subtotal = subtotal.add(itemTotal);
                orderItems.add(item);
            }
        }

        order.setSubtotal(subtotal);
        order.setTotal(subtotal.add(order.getShippingFee()).subtract(order.getDiscount()));
        order.setItems(orderItems);
        // Saga-first flow: order starts as pending and will be progressed by inventory/payment events.
        order.setStatus(Order.OrderStatus.PENDING);
        order.setPaymentStatus(Order.PaymentStatus.PENDING);

        Order savedOrder = orderRepository.save(order);

        if (requestedPoints > 0) {
            try {
                LoyaltyMutationResponse redeem = loyaltyServiceClient.redeemPoints(
                        effectiveUserId,
                        String.valueOf(savedOrder.getId()),
                        savedOrder.getTotal(),
                        requestedPoints
                );

                int appliedPoints = redeem != null && redeem.getAppliedPoints() != null ? redeem.getAppliedPoints() : 0;
                BigDecimal loyaltyDiscount = redeem != null && redeem.getDiscountAmount() != null
                        ? redeem.getDiscountAmount()
                        : BigDecimal.ZERO;

                if (appliedPoints <= 0 || loyaltyDiscount.compareTo(BigDecimal.ZERO) <= 0) {
                    orderRepository.deleteById(savedOrder.getId());
                    return ResponseEntity.badRequest().body(Map.of("error", "Requested points are not applicable"));
                }

                BigDecimal finalTotal = savedOrder.getTotal().subtract(loyaltyDiscount);
                if (finalTotal.compareTo(BigDecimal.ZERO) < 0) {
                    finalTotal = BigDecimal.ZERO;
                }

                savedOrder.setUsedPoints(appliedPoints);
                savedOrder.setLoyaltyDiscount(loyaltyDiscount);
                savedOrder.setTotal(finalTotal);
                savedOrder = orderRepository.save(savedOrder);
            } catch (IllegalArgumentException ex) {
                orderRepository.deleteById(savedOrder.getId());
                return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
            } catch (IllegalStateException ex) {
                orderRepository.deleteById(savedOrder.getId());
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("error", ex.getMessage()));
            }
        }

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

        // Best effort award for successful COD order; idempotent by order id in loyalty service.
        if (savedOrder.getPaymentMethod() == Order.PaymentMethod.COD
                && savedOrder.getPaymentStatus() == Order.PaymentStatus.PAID) {
            try {
                loyaltyServiceClient.earnPointsFromOrder(
                        effectiveUserId,
                        String.valueOf(savedOrder.getId()),
                        savedOrder.getTotal()
                );
            } catch (RuntimeException ignored) {
                // Keep order flow stable if loyalty awarding is temporarily unavailable.
            }
        }

        return ResponseEntity.ok(savedOrder);
    }

    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<Page<Order>> getUserOrders(
            @RequestHeader("X-User-Id") String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<Order> orderPage = orderRepository.findByUserId(
                userId,
                PageRequest.of(page, size, Sort.by("createdAt").descending()));
        orderPage.getContent().forEach(this::initializeItems);
        return ResponseEntity.ok(orderPage);
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public ResponseEntity<Order> getOrder(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") String userId) {
        return orderRepository.findByIdAndUserId(id, userId)
                .map(this::initializeItems)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * GET /api/v1/orders/by-number/{orderNumber}
     * Get full order detail by order number (used after payment success).
     */
    @GetMapping("/by-number/{orderNumber}")
    @Transactional(readOnly = true)
    public ResponseEntity<Order> getOrderByNumber(@PathVariable String orderNumber) {
        return orderRepository.findByOrderNumber(orderNumber)
                .map(this::initializeItems)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * GET /api/v1/orders/detail/{id}
     * Get full order detail by ID (used by order detail page).
     */
    @GetMapping("/detail/{id}")
    @Transactional(readOnly = true)
    public ResponseEntity<Order> getOrderDetail(@PathVariable Long id) {
        return orderRepository.findById(id)
                .map(this::initializeItems)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<Order> cancelOrder(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") String userId) {
        Order order = orderRepository.findByIdAndUserId(id, userId).orElse(null);
        if (order == null) {
            return ResponseEntity.notFound().build();
        }

        if (order.getStatus() == Order.OrderStatus.PENDING) {
            if (order.getUsedPoints() != null && order.getUsedPoints() > 0) {
                try {
                    loyaltyServiceClient.refundPoints(order.getUserId(), String.valueOf(order.getId()));
                } catch (IllegalArgumentException ex) {
                    return ResponseEntity.badRequest().build();
                } catch (IllegalStateException ex) {
                    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
                }
            }
            order.setStatus(Order.OrderStatus.CANCELLED);
            return ResponseEntity.ok(orderRepository.save(order));
        }

        return ResponseEntity.badRequest().build();
    }

    private Order initializeItems(Order order) {
        if (order.getItems() != null) {
            order.getItems().size();
        }
        return order;
    }
}
