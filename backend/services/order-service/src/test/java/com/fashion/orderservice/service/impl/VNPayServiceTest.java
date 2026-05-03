package com.fashion.orderservice.service;

import com.fashion.orderservice.config.VNPayConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VNPayServiceTest {

    @Mock
    private VNPayConfig vnPayConfig;

    @InjectMocks
    private VNPayService vnPayService;

    @BeforeEach
    void setUp() {
        // Cấu hình các giá trị giả lập cho VNPayConfig
        when(vnPayConfig.getTmnCode()).thenReturn("FASHION01");
        when(vnPayConfig.getHashSecret()).thenReturn("SECRET_KEY");
        when(vnPayConfig.getReturnUrl()).thenReturn("http://localhost:8080/return");
        when(vnPayConfig.getPaymentUrl()).thenReturn("https://sandbox.vnpayment.vn/paymentv2/vpcpay.html");
    }

    @Test
    @DisplayName("Tạo Payment URL - Kiểm tra định dạng và tham số bắt buộc")
    void createPaymentUrl_CheckParameters() {
        // Arrange
        Long orderId = 123L;
        long amount = 100000; // 100,000 VND
        String orderInfo = "Thanh toan don hang #123";
        String ipAddress = "127.0.0.1";

        // Act
        String url = vnPayService.createPaymentUrl(orderId, amount, orderInfo, ipAddress);

        // Assert
        assertNotNull(url);
        assertTrue(url.contains("vnp_Amount=10000000")); // Amount * 100
        assertTrue(url.contains("vnp_TmnCode=FASHION01"));
        assertTrue(url.contains("vnp_TxnRef=123"));
        assertTrue(url.contains("vnp_SecureHash="));
        assertTrue(url.startsWith("https://sandbox.vnpayment.vn/paymentv2/vpcpay.html"));
    }

    @Test
    @DisplayName("Xác thực chữ ký - Thành công khi hash khớp")
    void validateSignature_Success() {
        // Arrange
        // Giả lập các params VNPay gửi về sau khi thanh toán
        Map<String, String> params = new HashMap<>();
        params.put("vnp_Amount", "10000000");
        params.put("vnp_OrderInfo", "Thanh toan don hang");
        params.put("vnp_TxnRef", "123");
        
        // Tính toán hash thủ công dựa trên SECRET_KEY của setUp
        // Thứ tự TreeMap: vnp_Amount -> vnp_OrderInfo -> vnp_TxnRef
        String rawHashData = "vnp_Amount=10000000&vnp_OrderInfo=Thanh+toan+don+hang&vnp_TxnRef=123";
        String secureHash = VNPayConfig.hmacSHA512("SECRET_KEY", rawHashData);
        params.put("vnp_SecureHash", secureHash);

        // Act
        boolean isValid = vnPayService.validateSignature(params);

        // Assert
        assertTrue(isValid, "Chữ ký phải khớp với dữ liệu đã băm");
    }

    @Test
    @DisplayName("Xác thực chữ ký - Thất bại khi dữ liệu bị thay đổi (Tampered)")
    void validateSignature_Fail_WhenDataChanged() {
        // Arrange
        Map<String, String> params = new HashMap<>();
        params.put("vnp_Amount", "10000000");
        params.put("vnp_SecureHash", "wrong_hash");

        // Act
        boolean isValid = vnPayService.validateSignature(params);

        // Assert
        assertFalse(isValid, "Chữ ký không được khớp khi hash sai");
    }
}