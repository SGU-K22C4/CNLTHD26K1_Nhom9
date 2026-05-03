package com.fashion.orderservice.dto.request;

import com.fashion.orderservice.entity.Order.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class OrderRequest {
    @NotBlank(message = "recipientName is required")
    private String recipientName;

    @NotBlank(message = "recipientPhone is required")
    private String recipientPhone;

    @NotBlank(message = "shippingAddress is required")
    private String shippingAddress;

    @NotNull(message = "paymentMethod is required")
    private PaymentMethod paymentMethod;

    private String note;
    private String email;
    private String couponCode;
    private BigDecimal discount;
    private BigDecimal shippingFee;
    private Integer usedPoints;

    @NotEmpty(message = "items must not be empty")
    @Valid
    private List<OrderItemRequest> items;
}
