package com.fashion.orderservice.controller;

import com.fashion.orderservice.config.VNPayConfig;
import com.fashion.orderservice.service.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/payments/vnpay")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * GET /api/v1/payments/vnpay/create-payment?orderId=123
     * Returns a JSON with the VNPay redirect URL.
     */
    @GetMapping("/create-payment")
    public ResponseEntity<Map<String, String>> createPayment(
            @RequestParam Long orderId,
            @RequestHeader("X-User-Id") String userId,
            HttpServletRequest request) {

        String ipAddress = VNPayConfig.getIpAddress(request);
        String paymentUrl = paymentService.createPaymentUrl(orderId, userId, ipAddress);

        return ResponseEntity.ok(Map.of("paymentUrl", paymentUrl));
    }

    /**
     * GET /api/v1/payments/vnpay/payment-return?vnp_TxnRef=...&vnp_ResponseCode=...&vnp_SecureHash=...
     * Called by the frontend after VNPay redirects back.
     * Validates signature and publishes payment result event.
     */
    @GetMapping("/payment-return")
    public ResponseEntity<Map<String, Object>> paymentReturn(@RequestParam Map<String, String> params) {
        Map<String, Object> result = paymentService.processPaymentReturn(params);
        return ResponseEntity.ok(result);
    }
}
