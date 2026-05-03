package com.fashion.promotionservice.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ActivePromotionResponse {
    private String code;
    private String discountType;
    private String discountValue;
    private String minOrderAmount;
    private String maxDiscountAmount;
    private String endDate;
}
