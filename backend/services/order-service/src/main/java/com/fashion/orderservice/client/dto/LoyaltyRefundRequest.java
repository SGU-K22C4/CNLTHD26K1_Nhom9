package com.fashion.orderservice.client.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LoyaltyRefundRequest {
    private String userId;
    private String refId;
    private String description;
}
