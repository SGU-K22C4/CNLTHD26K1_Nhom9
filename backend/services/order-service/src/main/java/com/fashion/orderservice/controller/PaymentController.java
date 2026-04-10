package com.fashion.orderservice.controller;

import com.fashion.common.event.PaymentResultEvent;
import com.fashion.orderservice.client.LoyaltyServiceClient;
import com.fashion.orderservice.config.VNPayConfig;
import com.fashion.orderservice.entity.Order;
import com.fashion.orderservice.repository.OrderRepository;
import com.fashion.orderservice.service.VNPayService;
import com.fashion.orderservice.saga.SagaEventPublisher;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/payments/vnpay")
@RequiredArgsConstructor
@SuppressWarnings("null")
public class PaymentController {

    private final VNPayService vnPayService;
    private final OrderRepository orderRepository;
    private final SagaEventPublisher sagaEventPublisher;
    private final LoyaltyServiceClient loyaltyServiceClient;
    private final SagaEventPublisher sagaEventPublisher;
    private final LoyaltyServiceClient loyaltyServiceClient;

    /**
     * GET /api/v1/payments/vnpay/create-payment?orderId=123
     * Returns a JSON with the VNPay redirect URL.
     */
    @GetMapping("/create-payment")
    public ResponseEntity<Map<String, String>> createPayment(
            @RequestParam Long orderId,
            HttpServletRequest request) {

        Order order = orderRepository.findById(orderId).orElse(null);
        if (order == null) {
            return ResponseEntity.notFound().build();
        }

        // Only allow VNPAY payment method
        if (order.getPaymentMethod() != Order.PaymentMethod.VNPAY) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Order payment method is not VNPAY");
            return ResponseEntity.badRequest().body(error);
        }

        String ipAddress = VNPayConfig.getIpAddress(request);
        long totalAmount = order.getTotal().longValue(); // VND integer
        String orderInfo = "Thanh toan don hang " + order.getOrderNumber();

        String paymentUrl = vnPayService.createPaymentUrl(order.getId(), totalAmount, orderInfo, ipAddress);

        Map<String, String> response = new HashMap<>();
        response.put("paymentUrl", paymentUrl);
        return ResponseEntity.ok(response);
    }

    /**
     * GET
     * /api/v1/payments/vnpay/payment-return?vnp_TxnRef=...&vnp_ResponseCode=...&vnp_SecureHash=...
     * Called by the frontend after VNPay redirects back.
     * Validates signature and updates order status.
     */
    @GetMapping("/payment-return")
    public ResponseEntity<Map<String, Object>> paymentReturn(@RequestParam Map<String, String> params) {

        Map<String, Object> result = new HashMap<>();

        boolean isValid = vnPayService.validateSignature(params);
        if (!isValid) {
            result.put("success", false);
            result.put("message", "Invalid signature");
            return ResponseEntity.badRequest().body(result);
        }

        String txnRef = params.get("vnp_TxnRef");
        String responseCode = params.get("vnp_ResponseCode");
        String transactionNo = params.get("vnp_TransactionNo");

        try {
            Long orderId = Long.parseLong(txnRef);
            Order order = orderRepository.findById(orderId).orElse(null);

            if (order == null) {
                result.put("success", false);
                result.put("message", "Order not found");
                return ResponseEntity.badRequest().body(result);
            }

            boolean success = "00".equals(responseCode);
            sagaEventPublisher.publishPaymentResult(PaymentResultEvent.builder()
                    .orderId(order.getId())
                    .success(success)
                    .transactionNo(transactionNo)
                    .provider("VNPAY")
                    .reason(success ? null : "VNPAY response code: " + responseCode)
                    .build());

            if (success) {
                try {
                    loyaltyServiceClient.earnPointsFromOrder(
                            order.getUserId(),
                            String.valueOf(order.getId()),
                            order.getTotal()
                    );
                } catch (RuntimeException ignored) {
                    // Keep payment success flow stable if loyalty awarding is temporarily unavailable.
                }
            }

            result.put("success", success);
            result.put("message", "Payment callback received and queued for async processing");
            result.put("orderId", order.getId());
            result.put("orderNumber", order.getOrderNumber());
            result.put("transactionNo", transactionNo);
            result.put("processingAsync", true);
            result.put("responseCode", responseCode);
        } catch (NumberFormatException e) {
            result.put("success", false);
            result.put("message", "Invalid transaction reference");
        }

        return ResponseEntity.ok(result);
    }
}
