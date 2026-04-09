package com.fashion.promotionservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class EarnOrderPointsRequest {

    private String userId;

    @NotBlank
    private String orderId;

    @NotNull
    @Positive
    private BigDecimal netAmount;

    private String description;
}
