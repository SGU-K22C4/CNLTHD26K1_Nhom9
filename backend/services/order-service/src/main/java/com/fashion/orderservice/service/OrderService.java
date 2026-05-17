package com.fashion.orderservice.service;

import com.fashion.orderservice.dto.request.OrderRequest;
import com.fashion.orderservice.dto.response.OrderResponse;
import com.fashion.orderservice.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface OrderService {
    OrderResponse createOrder(String userId, OrderRequest request);

    Page<OrderResponse> getUserOrders(String userId, Pageable pageable);

    Page<OrderResponse> getAdminOrders(String keyword, String status, Pageable pageable);

    Optional<OrderResponse> getOrderForUser(Long id, String userId);

    Optional<OrderResponse> getOrderByNumber(String orderNumber, String userId);

    Optional<OrderResponse> getOrderDetail(Long id, String userId);

    Optional<OrderResponse> getOrderDetailForAdmin(Long id);

    OrderResponse cancelOrder(Long id, String userId);

    OrderResponse cancelOrderAsAdmin(Long id);

    OrderResponse updateStatusAsAdmin(Long id, Order.OrderStatus targetStatus);
}
