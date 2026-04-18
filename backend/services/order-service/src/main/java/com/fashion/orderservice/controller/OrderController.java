package com.fashion.orderservice.controller;

import com.fashion.orderservice.dto.request.OrderRequest;
import com.fashion.orderservice.dto.response.OrderResponse;
import com.fashion.orderservice.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    /**
     * POST /api/v1/orders
     * Create a new order. All business logic (product validation, loyalty redeem,
     * Saga event publishing) is handled by OrderServiceImpl.
     */
    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @Valid @RequestBody OrderRequest request) {
        return ResponseEntity.ok(orderService.createOrder(userId, request));
    }

    /**
     * GET /api/v1/orders
     * Get paginated order history for the authenticated user.
     */
    @GetMapping
    public ResponseEntity<Page<OrderResponse>> getUserOrders(
            @RequestHeader("X-User-Id") String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(
                orderService.getUserOrders(userId, PageRequest.of(page, size, Sort.by("createdAt").descending())));
    }

    /**
     * GET /api/v1/orders/{id}
     * Get order detail by ID for the authenticated user.
     */
    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrder(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") String userId) {
        return orderService.getOrderForUser(id, userId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * GET /api/v1/orders/by-number/{orderNumber}
     * Get full order detail by order number (used after payment success).
     */
    @GetMapping("/by-number/{orderNumber}")
    public ResponseEntity<OrderResponse> getOrderByNumber(@PathVariable String orderNumber) {
        return orderService.getOrderByNumber(orderNumber)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * GET /api/v1/orders/detail/{id}
     * Get full order detail by ID (used by order detail page).
     */
    @GetMapping("/detail/{id}")
    public ResponseEntity<OrderResponse> getOrderDetail(@PathVariable Long id) {
        return orderService.getOrderDetail(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * PATCH /api/v1/orders/{id}/cancel
     * Cancel a PENDING order. Handles loyalty refund and inventory restoration
     * via OrderServiceImpl.
     */
    @PatchMapping("/{id}/cancel")
    public ResponseEntity<OrderResponse> cancelOrder(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") String userId) {
        return ResponseEntity.ok(orderService.cancelOrder(id, userId));
    }
}
