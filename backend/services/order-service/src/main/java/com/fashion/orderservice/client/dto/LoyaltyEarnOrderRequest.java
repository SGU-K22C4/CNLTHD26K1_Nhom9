package com.fashion.orderservice.client.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class LoyaltyEarnOrderRequest {
    private String userId;
    private String orderId;
    private BigDecimal netAmount;
    private String description;
}
