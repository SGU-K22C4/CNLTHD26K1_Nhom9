package com.fashion.orderservice.service.impl;

import com.fashion.common.event.PaymentResultEvent;
import com.fashion.orderservice.entity.Order;
import com.fashion.orderservice.repository.OrderRepository;
import com.fashion.orderservice.saga.SagaEventPublisher;
import com.fashion.orderservice.service.PaymentService;
import com.fashion.orderservice.service.VNPayService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private final VNPayService vnPayService;
    private final OrderRepository orderRepository;
    private final SagaEventPublisher sagaEventPublisher;

    @Override
    public String createPaymentUrl(Long orderId, String ipAddress) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));

        if (order.getPaymentMethod() != Order.PaymentMethod.VNPAY) {
            throw new IllegalArgumentException("Order payment method is not VNPAY");
        }

        long totalAmount = order.getTotal().longValue();
        String orderInfo = "Thanh toan don hang " + order.getOrderNumber();

        return vnPayService.createPaymentUrl(order.getId(), totalAmount, orderInfo, ipAddress);
    }

    @Override
    public Map<String, Object> processPaymentReturn(Map<String, String> params) {
        Map<String, Object> result = new HashMap<>();

        boolean isValid = vnPayService.validateSignature(params);
        if (!isValid) {
            throw new IllegalArgumentException("Invalid signature");
        }

        String txnRef = params.get("vnp_TxnRef");
        String responseCode = params.get("vnp_ResponseCode");
        String transactionNo = params.get("vnp_TransactionNo");

        Long orderId;
        try {
            orderId = Long.parseLong(txnRef);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid transaction reference");
        }

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));

        boolean success = "00".equals(responseCode);
        sagaEventPublisher.publishPaymentResult(PaymentResultEvent.builder()
                .orderId(order.getId())
                .success(success)
                .transactionNo(transactionNo)
                .provider("VNPAY")
                .reason(success ? null : "VNPAY response code: " + responseCode)
                .build());

        result.put("success", success);
        result.put("message", "Payment callback received and queued for async processing");
        result.put("orderId", order.getId());
        result.put("orderNumber", order.getOrderNumber());
        result.put("transactionNo", transactionNo);
        result.put("processingAsync", true);
        result.put("responseCode", responseCode);

        return result;
    }
}
