package com.fashion.orderservice.controller;

import com.fashion.orderservice.entity.Order;
import com.fashion.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
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

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderRepository orderRepository;
    private final JdbcTemplate jdbcTemplate;

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
                        productId
                );
                if (!Boolean.TRUE.equals(exists)) {
                    return ResponseEntity.badRequest().body(Map.of(
                            "error", "Invalid productId",
                            "productId", productId
                    ));
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
        // COD → auto-confirm; other methods stay PENDING until payment verified
        if (request.getPaymentMethod() == Order.PaymentMethod.COD) {
            order.setStatus(Order.OrderStatus.CONFIRMED);
            order.setPaymentStatus(Order.PaymentStatus.PAID);
        } else {
            order.setStatus(Order.OrderStatus.PENDING);
            order.setPaymentStatus(Order.PaymentStatus.PENDING);
        }

        Order savedOrder = orderRepository.save(order);
        return ResponseEntity.ok(savedOrder);
    }

    @GetMapping
    public ResponseEntity<Page<Order>> getUserOrders(
            @RequestHeader("X-User-Id") String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(
                orderRepository.findByUserId(userId, PageRequest.of(page, size, Sort.by("createdAt").descending())));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Order> getOrder(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") String userId) {
        return orderRepository.findByIdAndUserId(id, userId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * GET /api/v1/orders/by-number/{orderNumber}
     * Get full order detail by order number (used after payment success).
     */
    @GetMapping("/by-number/{orderNumber}")
    public ResponseEntity<Order> getOrderByNumber(@PathVariable String orderNumber) {
        return orderRepository.findByOrderNumber(orderNumber)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * GET /api/v1/orders/detail/{id}
     * Get full order detail by ID (used by order detail page).
     */
    @GetMapping("/detail/{id}")
    public ResponseEntity<Order> getOrderDetail(@PathVariable Long id) {
        return orderRepository.findById(id)
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
            order.setStatus(Order.OrderStatus.CANCELLED);
            return ResponseEntity.ok(orderRepository.save(order));
        }

        return ResponseEntity.badRequest().build();
    }
}
