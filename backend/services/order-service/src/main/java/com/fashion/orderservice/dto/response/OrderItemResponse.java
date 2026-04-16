package com.fashion.orderservice.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class OrderItemResponse {
    private Long id;
    private String productId;
    private String productName;
    private String productSlug;
    private String imageUrl;
    private String color;
    private String size;
    private int quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalPrice;
}
