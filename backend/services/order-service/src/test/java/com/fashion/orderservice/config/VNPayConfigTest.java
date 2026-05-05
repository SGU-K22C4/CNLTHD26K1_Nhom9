package com.fashion.orderservice.config;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VNPayConfigTest {

    @Mock
    private HttpServletRequest request;

    @Test
    void should_GenerateSha512Hash_When_KeyAndDataAreProvided() {
        String hash = VNPayConfig.hmacSHA512("secret_key", "amount=10000&orderId=123");

        assertNotNull(hash);
        assertEquals(128, hash.length());
    }

    @Test
    void should_ReturnFirstForwardedIp_When_XForwardedForContainsMultipleValues() {
        when(request.getHeader("X-Forwarded-For")).thenReturn("192.168.1.1, 10.0.0.1");

        String ipAddress = VNPayConfig.getIpAddress(request);

        assertEquals("192.168.1.1", ipAddress);
    }

    @Test
    void should_FallbackToRemoteAddr_When_ForwardedHeadersAreMissing() {
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getHeader("Proxy-Client-IP")).thenReturn(null);
        when(request.getHeader("WL-Proxy-Client-IP")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        String ipAddress = VNPayConfig.getIpAddress(request);

        assertEquals("127.0.0.1", ipAddress);
    }
}
