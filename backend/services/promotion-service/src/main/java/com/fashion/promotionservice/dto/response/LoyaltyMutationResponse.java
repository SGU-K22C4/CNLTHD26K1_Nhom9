package com.fashion.promotionservice.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class LoyaltyMutationResponse {

    private Integer appliedPoints;

    private Integer currentPoints;

    private BigDecimal discountAmount;

    private boolean idempotent;

    private String message;
}
