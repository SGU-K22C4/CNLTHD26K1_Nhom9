package com.fashion.promotionservice.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class LoyaltyWalletResponse {

    private String userId;

    private Integer currentPoints;

    private BigDecimal totalSpending;

    private String tierId;

    private String tierName;

    private BigDecimal tierDiscountPercent;

    private BigDecimal tierPointRate;

    private Integer pointToVnd;
}
