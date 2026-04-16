package com.fashion.orderservice.service;

import java.util.Map;

public interface PaymentService {

    /**
     * Create a VNPay payment URL for the given order.
     *
     * @param orderId   the order's database ID
     * @param ipAddress the client's IP address
     * @return the VNPay redirect URL
     * @throws IllegalArgumentException if order not found or payment method is not
     *                                  VNPAY
     */
    String createPaymentUrl(Long orderId, String ipAddress);

    /**
     * Process VNPay payment callback: validate signature, publish payment result
     * event.
     *
     * @param params all query parameters from VNPay redirect
     * @return result map containing success status, orderId, orderNumber, etc.
     * @throws IllegalArgumentException if signature invalid, order not found, or
     *                                  txnRef invalid
     */
    Map<String, Object> processPaymentReturn(Map<String, String> params);
}
