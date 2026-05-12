package com.fashion.common.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Published when an order is cancelled AFTER inventory was successfully reserved.
 * Product Service consumes this to restore (re-add) the reserved quantities.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderCancelledEvent {
    private Long orderId;
    private String reason;
    private List<OrderItemEvent> items;

    /** User who placed the order — needed by Promotion Service to refund loyalty points. */
    private String userId;

    /** Number of loyalty points used in this order — 0 or null means no refund needed. */
    private Integer usedPoints;
}
