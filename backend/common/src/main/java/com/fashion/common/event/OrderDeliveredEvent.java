package com.fashion.common.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderDeliveredEvent {
    private Long orderId;
    private String orderNumber;
    private String userId;
    private String paymentMethod;
    private BigDecimal netAmount;
}
