package com.fashion.orderservice.client.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class LoyaltyMutationResponse {
    private Integer appliedPoints;
    private Integer currentPoints;
    private BigDecimal discountAmount;
    private boolean idempotent;
    private String message;
}
