package com.fashion.orderservice.dto.request;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class OrderItemRequest {
    private String productId;
    private String productName;
    private String productSlug;
    private String imageUrl;
    private String color;
    private String size;
    private int quantity;
    private BigDecimal unitPrice;
}
