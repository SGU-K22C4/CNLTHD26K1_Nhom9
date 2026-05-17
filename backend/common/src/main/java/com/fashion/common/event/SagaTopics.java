package com.fashion.common.event;

public final class SagaTopics {
    private SagaTopics() {
    }

    public static final String ORDER_CREATED = "order.created.v1";
    public static final String INVENTORY_RESERVATION_RESULT = "inventory.reservation.result.v1";
    public static final String PAYMENT_RESULT = "payment.result.v1";
    public static final String ORDER_CANCELLED = "order.cancelled.v1";
    public static final String ORDER_DELIVERED = "order.delivered.v1";
}
