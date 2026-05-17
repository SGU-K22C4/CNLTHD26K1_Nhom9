package com.fashion.orderservice.controller;

import com.fashion.orderservice.dto.request.UpdateOrderStatusRequest;
import com.fashion.orderservice.dto.response.OrderResponse;
import com.fashion.orderservice.entity.Order;
import com.fashion.orderservice.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/admin/orders")
@RequiredArgsConstructor
public class AdminOrderController {
    private final OrderService orderService;

    @GetMapping
    public ResponseEntity<Page<OrderResponse>> getAdminOrders(
            @RequestHeader("X-User-Role") String userRole,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        assertAdminRole(userRole);
        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        return ResponseEntity.ok(orderService.getAdminOrders(keyword, status, PageRequest.of(page, size, sort)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getAdminOrderDetail(
            @PathVariable Long id,
            @RequestHeader("X-User-Role") String userRole
    ) {
        assertAdminRole(userRole);
        return orderService.getOrderDetailForAdmin(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<OrderResponse> cancelOrderAsAdmin(
            @PathVariable Long id,
            @RequestHeader("X-User-Role") String userRole
    ) {
        assertAdminRole(userRole);
        return ResponseEntity.ok(orderService.cancelOrderAsAdmin(id));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<OrderResponse> updateStatusAsAdmin(
            @PathVariable Long id,
            @RequestHeader("X-User-Role") String userRole,
            @Valid @RequestBody UpdateOrderStatusRequest request
    ) {
        assertAdminRole(userRole);
        try {
            Order.OrderStatus targetStatus = Order.OrderStatus.valueOf(request.getStatus().trim().toUpperCase());
            return ResponseEntity.ok(orderService.updateStatusAsAdmin(id, targetStatus));
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }

    private void assertAdminRole(String userRole) {
        if (userRole == null || !"ADMIN".equalsIgnoreCase(userRole.trim())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin role is required");
        }
    }
}
