package com.fashion.orderservice.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class OrderResponse {
    private Long id;
    private String orderNumber;
    private String userId;
    private String status;
    private String paymentMethod;
    private String paymentStatus;

    // Pricing
    private BigDecimal subtotal;
    private BigDecimal shippingFee;
    private BigDecimal discount;
    private BigDecimal loyaltyDiscount;
    private Integer usedPoints;
    private BigDecimal total;
    private String couponCode;

    // Shipping info
    private String recipientName;
    private String recipientPhone;
    private String shippingAddress;

    private String note;

    private List<OrderItemResponse> items;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
