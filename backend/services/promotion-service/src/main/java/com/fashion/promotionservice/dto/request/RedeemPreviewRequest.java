package com.fashion.promotionservice.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class RedeemPreviewRequest {

    private String userId;

    @NotNull
    @Positive
    private BigDecimal orderAmount;

    @NotNull
    @Positive
    private Integer requestedPoints;
}
