package com.fashion.promotionservice.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class RedeemPreviewResponse {

    private boolean valid;

    private Integer requestedPoints;

    private Integer appliedPoints;

    private Integer currentPoints;

    private Integer pointToVnd;

    private BigDecimal discountAmount;

    private String message;
}
