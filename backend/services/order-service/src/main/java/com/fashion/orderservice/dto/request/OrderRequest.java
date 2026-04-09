package com.fashion.orderservice.dto.request;

import com.fashion.orderservice.entity.Order.PaymentMethod;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class OrderRequest {
    private String recipientName;
    private String recipientPhone;
    private String shippingAddress;
    private PaymentMethod paymentMethod;
    private String note;
    private String email;
    private String couponCode;
    private BigDecimal discount;
    private BigDecimal shippingFee;
    private Integer usedPoints;
    private List<OrderItemRequest> items;
}
