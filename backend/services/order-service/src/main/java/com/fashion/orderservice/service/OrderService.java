package com.fashion.orderservice.service;

import com.fashion.orderservice.dto.request.OrderRequest;
import com.fashion.orderservice.dto.response.OrderResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface OrderService {

    OrderResponse createOrder(String userId, OrderRequest request);

    Page<OrderResponse> getUserOrders(String userId, Pageable pageable);

    Optional<OrderResponse> getOrderForUser(Long id, String userId);

    Optional<OrderResponse> getOrderByNumber(String orderNumber, String userId);

    Optional<OrderResponse> getOrderDetail(Long id, String userId);

    OrderResponse cancelOrder(Long id, String userId);
}
