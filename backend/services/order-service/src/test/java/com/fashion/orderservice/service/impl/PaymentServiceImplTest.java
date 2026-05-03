package com.fashion.orderservice.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fashion.orderservice.entity.Order;
import com.fashion.orderservice.repository.OrderRepository;
import com.fashion.orderservice.saga.SagaEventPublisher;
import com.fashion.orderservice.service.VNPayService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    private StubVNPayService vnPayService;
    private CapturingSagaEventPublisher sagaEventPublisher;
    private PaymentServiceImpl paymentService;

    @BeforeEach
    void setUp() {
        vnPayService = new StubVNPayService();
        sagaEventPublisher = new CapturingSagaEventPublisher();
        paymentService = new PaymentServiceImpl(vnPayService, orderRepository, sagaEventPublisher);
    }

    @Test
    void should_CreatePaymentUrl_When_OrderBelongsToUserAndUsesVnpay() {
        Order order = new Order();
        order.setId(1L);
        order.setUserId("user-123");
        order.setTotal(BigDecimal.valueOf(100000));
        order.setOrderNumber("ORD-TEST-123");
        order.setPaymentMethod(Order.PaymentMethod.VNPAY);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        vnPayService.nextPaymentUrl = "https://vnpay.vn/mock-url";

        String paymentUrl = paymentService.createPaymentUrl(1L, "user-123", "127.0.0.1");

        assertEquals("https://vnpay.vn/mock-url", paymentUrl);
        assertEquals(1L, vnPayService.lastOrderId);
        assertEquals(100000L, vnPayService.lastTotalAmount);
        assertEquals("Thanh toan don hang ORD-TEST-123", vnPayService.lastOrderInfo);
        assertEquals("127.0.0.1", vnPayService.lastIpAddress);
    }

    @Test
    void should_ThrowIllegalArgumentException_When_UserDoesNotOwnOrder() {
        Order order = new Order();
        order.setId(1L);
        order.setUserId("owner-1");
        order.setPaymentMethod(Order.PaymentMethod.VNPAY);
        order.setTotal(BigDecimal.valueOf(100000));

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        assertThrows(IllegalArgumentException.class, () -> paymentService.createPaymentUrl(1L, "user-123", "127.0.0.1"));
    }

    @Test
    void should_ThrowIllegalArgumentException_When_OrderDoesNotUseVnpay() {
        Order order = new Order();
        order.setId(1L);
        order.setUserId("user-123");
        order.setPaymentMethod(Order.PaymentMethod.COD);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        assertThrows(IllegalArgumentException.class, () -> paymentService.createPaymentUrl(1L, "user-123", "127.0.0.1"));
    }

    @Test
    void should_ThrowIllegalArgumentException_When_SignatureIsInvalid() {
        Map<String, String> params = Map.of("vnp_SecureHash", "invalid");
        vnPayService.signatureValid = false;

        assertThrows(IllegalArgumentException.class, () -> paymentService.processPaymentReturn(params));
        assertNull(sagaEventPublisher.lastPaymentResult);
    }

    @Test
    void should_PublishSuccessfulPaymentResult_When_VnpayResponseCodeIs00() {
        Map<String, String> params = Map.of(
                "vnp_TxnRef", "1",
                "vnp_ResponseCode", "00",
                "vnp_TransactionNo", "123456");
        Order order = new Order();
        order.setId(1L);
        order.setOrderNumber("ORD-123");

        vnPayService.signatureValid = true;
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        Map<String, Object> result = paymentService.processPaymentReturn(params);

        assertTrue((Boolean) result.get("success"));
        assertTrue(sagaEventPublisher.lastPaymentResult.isSuccess());
        assertEquals(1L, sagaEventPublisher.lastPaymentResult.getOrderId());
        assertEquals("123456", sagaEventPublisher.lastPaymentResult.getTransactionNo());
    }

    @Test
    void should_PublishFailedPaymentResult_When_VnpayResponseCodeIsNot00() {
        Map<String, String> params = Map.of(
                "vnp_TxnRef", "1",
                "vnp_ResponseCode", "99",
                "vnp_TransactionNo", "000000");
        Order order = new Order();
        order.setId(1L);
        order.setOrderNumber("ORD-123");

        vnPayService.signatureValid = true;
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        Map<String, Object> result = paymentService.processPaymentReturn(params);

        assertFalse((Boolean) result.get("success"));
        assertFalse(sagaEventPublisher.lastPaymentResult.isSuccess());
        assertTrue(sagaEventPublisher.lastPaymentResult.getReason().contains("99"));
    }

    private static class StubVNPayService extends VNPayService {
        private String nextPaymentUrl;
        private boolean signatureValid;
        private Long lastOrderId;
        private long lastTotalAmount;
        private String lastOrderInfo;
        private String lastIpAddress;

        StubVNPayService() {
            super(null);
        }

        @Override
        public String createPaymentUrl(Long orderId, long totalAmount, String orderInfo, String ipAddress) {
            lastOrderId = orderId;
            lastTotalAmount = totalAmount;
            lastOrderInfo = orderInfo;
            lastIpAddress = ipAddress;
            return nextPaymentUrl;
        }

        @Override
        public boolean validateSignature(Map<String, String> params) {
            return signatureValid;
        }
    }

    private static class CapturingSagaEventPublisher extends SagaEventPublisher {
        private com.fashion.common.event.PaymentResultEvent lastPaymentResult;

        CapturingSagaEventPublisher() {
            super(null, new ObjectMapper());
        }

        @Override
        public void publishPaymentResult(com.fashion.common.event.PaymentResultEvent event) {
            this.lastPaymentResult = event;
        }
    }
}
