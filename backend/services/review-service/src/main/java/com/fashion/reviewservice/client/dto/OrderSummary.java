package com.fashion.reviewservice.client.dto;

import lombok.Data;

import java.util.List;

@Data
public class OrderSummary {
    private Long id;
    private String status;
    private List<OrderItemSummary> items;

    @Data
    public static class OrderItemSummary {
        private String productId;
    }
}
