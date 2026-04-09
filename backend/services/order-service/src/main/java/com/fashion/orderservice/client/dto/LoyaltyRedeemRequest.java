package com.fashion.orderservice.client.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class LoyaltyRedeemRequest {
    private String userId;
    private String refId;
    private BigDecimal orderAmount;
    private Integer requestedPoints;
    private String description;
}
