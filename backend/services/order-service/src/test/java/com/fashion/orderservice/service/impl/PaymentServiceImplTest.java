package com.fashion.orderservice.service.impl;

import com.fashion.common.event.PaymentResultEvent;
import com.fashion.orderservice.entity.Order;
import com.fashion.orderservice.repository.OrderRepository;
import com.fashion.orderservice.saga.SagaEventPublisher;
import com.fashion.orderservice.service.VNPayService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @Mock private VNPayService vnPayService;
    @Mock private OrderRepository orderRepository;
    @Mock private SagaEventPublisher sagaEventPublisher;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    @Test
    @DisplayName("Tạo link thanh toán VNPay thành công")
    void createPaymentUrl_Success() {
        // Arrange
        Order order = new Order();
        order.setId(1L);
        order.setTotal(BigDecimal.valueOf(100000));
        order.setOrderNumber("ORD-TEST-123");
        order.setPaymentMethod(Order.PaymentMethod.VNPAY);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(vnPayService.createPaymentUrl(anyLong(), anyLong(), anyString(), anyString()))
                .thenReturn("https://vnpay.vn/mock-url");

        // Act
        String url = paymentService.createPaymentUrl(1L, "127.0.0.1");

        // Assert
        assertEquals("https://vnpay.vn/mock-url", url);
        verify(vnPayService).createPaymentUrl(eq(1L), eq(100000L), contains("ORD-TEST-123"), eq("127.0.0.1"));
    }

    @Test
    @DisplayName("Tạo link thanh toán thất bại - Đơn hàng không phải phương thức VNPAY")
    void createPaymentUrl_InvalidMethod() {
        // Arrange
        Order order = new Order();
        order.setPaymentMethod(Order.PaymentMethod.COD); // Đơn COD nhưng đòi tạo link VNPAY
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> paymentService.createPaymentUrl(1L, "127.0.0.1"));
    }

    @Test
    @DisplayName("Xử lý VNPay Return - Chặn đứng khi chữ ký không hợp lệ")
    void processPaymentReturn_InvalidSignature() {
        // Arrange
        Map<String, String> params = Map.of("vnp_SecureHash", "fake_hash");
        when(vnPayService.validateSignature(any())).thenReturn(false);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> paymentService.processPaymentReturn(params));
        verify(sagaEventPublisher, never()).publishPaymentResult(any());
    }

    @Test
    @DisplayName("Xử lý VNPay Return - Thanh toán THÀNH CÔNG (Code 00)")
    void processPaymentReturn_PaymentSuccess() {
        // Arrange
        Map<String, String> params = Map.of(
            "vnp_TxnRef", "1",
            "vnp_ResponseCode", "00",
            "vnp_TransactionNo", "123456"
        );
        Order order = new Order();
        order.setId(1L);
        order.setOrderNumber("ORD-123");

        when(vnPayService.validateSignature(any())).thenReturn(true);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        // Act
        Map<String, Object> result = paymentService.processPaymentReturn(params);

        // Assert
        assertTrue((Boolean) result.get("success"));
        verify(sagaEventPublisher).publishPaymentResult(argThat(event -> 
            event.isSuccess() && event.getOrderId().equals(1L) && "123456".equals(event.getTransactionNo())
        ));
    }

    @Test
    @DisplayName("Xử lý VNPay Return - Thanh toán THẤT BẠI (Code khác 00)")
    void processPaymentReturn_PaymentFailed() {
        // Arrange
        Map<String, String> params = Map.of(
            "vnp_TxnRef", "1",
            "vnp_ResponseCode", "99", // Mã lỗi bất kỳ từ VNPay
            "vnp_TransactionNo", "000000"
        );
        Order order = new Order();
        order.setId(1L);

        when(vnPayService.validateSignature(any())).thenReturn(true);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        // Act
        Map<String, Object> result = paymentService.processPaymentReturn(params);

        // Assert
        assertFalse((Boolean) result.get("success"));
        verify(sagaEventPublisher).publishPaymentResult(argThat(event -> 
            !event.isSuccess() && event.getReason().contains("99")
        ));
    }
}