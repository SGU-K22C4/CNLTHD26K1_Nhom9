package com.fashion.orderservice.config;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VNPayConfigTest {

    @Mock
    private HttpServletRequest request;

    @Test
    @DisplayName("Test generate HMAC SHA512 thành công")
    void hmacSHA512_Success() {
        // Arrange
        String key = "secret_key";
        String data = "amount=10000&orderId=123";

        // Act
        String hash = VNPayConfig.hmacSHA512(key, data);

        // Assert
        assertNotNull(hash);
        assertEquals(128, hash.length()); // SHA-512 hex string luôn dài 128 ký tự
    }

    @Test
    @DisplayName("Test lấy IP Address từ X-Forwarded-For (Reverse Proxy)")
    void getIpAddress_FromXForwardedFor() {
        // Arrange
        when(request.getHeader("X-Forwarded-For")).thenReturn("192.168.1.1, 10.0.0.1");

        // Act
        String ip = VNPayConfig.getIpAddress(request);

        // Assert
        assertEquals("192.168.1.1", ip); // Phải lấy IP đầu tiên trong danh sách
    }

    @Test
    @DisplayName("Test lấy IP Address mặc định khi không có header")
    void getIpAddress_DefaultRemoteAddr() {
        // Arrange
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        // Act
        String ip = VNPayConfig.getIpAddress(request);

        // Assert
        assertEquals("127.0.0.1", ip);
    }
    @Test
    @DisplayName("GET /create-payment - Trả về link VNPay")
    void createPayment_Success() throws Exception {
        String mockUrl = "https://sandbox.vnpayment.vn/payment";
        when(paymentService.createPaymentUrl(anyLong(), anyString())).thenReturn(mockUrl);

        mockMvc.perform(get("/api/v1/payments/vnpay/create-payment")
                .param("orderId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentUrl").value(mockUrl));
    }

    @Test
    @DisplayName("GET /payment-return - Xử lý callback từ VNPay")
    void paymentReturn_Success() throws Exception {
        Map<String, Object> mockResult = Map.of("status", "00", "message", "Success");
        when(paymentService.processPaymentReturn(any())).thenReturn(mockResult);

        mockMvc.perform(get("/api/v1/payments/vnpay/payment-return")
                .param("vnp_ResponseCode", "00")
                .param("vnp_TxnRef", "ORD-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("00"));
    }
}