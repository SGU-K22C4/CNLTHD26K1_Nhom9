package com.fashion.orderservice.client;

import com.fashion.orderservice.client.dto.LoyaltyMutationResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoyaltyServiceClientTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private LoyaltyServiceClient loyaltyServiceClient;

    @BeforeEach
    void setUp() {
        // Inject giá trị cho @Value biển url
        ReflectionTestUtils.setField(loyaltyServiceClient, "promotionServiceUrl", "http://localhost:8085");
    }

    @Test
    @DisplayName("Redeem points thành công")
    void redeemPoints_Success() {
        // Arrange
        LoyaltyMutationResponse mockResponse = new LoyaltyMutationResponse();
        mockResponse.setAppliedPoints(100);
        
        ResponseEntity<LoyaltyMutationResponse> responseEntity = new ResponseEntity<>(mockResponse, HttpStatus.OK);
        
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(LoyaltyMutationResponse.class)))
                .thenReturn(responseEntity);

        // Act
        LoyaltyMutationResponse result = loyaltyServiceClient.redeemPoints("user1", "order1", BigDecimal.valueOf(1000), 100);

        // Assert
        assertNotNull(result);
        assertEquals(100, result.getAppliedPoints());
    }

    @Test
    @DisplayName("Redeem points thất bại do lỗi 400 và parse được message lỗi")
    void redeemPoints_BadRequest_ExtractMessage() {
        // Arrange
        String errorJson = "{\"message\":\"Not enough points\"}";
        HttpClientErrorException ex = HttpClientErrorException.create(HttpStatus.BAD_REQUEST, "Bad Request", null, errorJson.getBytes(), null);

        when(restTemplate.exchange(anyString(), any(), any(), eq(LoyaltyMutationResponse.class)))
                .thenThrow(ex);

        // Act & Assert
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> 
            loyaltyServiceClient.redeemPoints("user1", "order1", BigDecimal.TEN, 100)
        );
        assertEquals("Not enough points", thrown.getMessage());
    }

    @Test
    @DisplayName("Earn points thất bại do lỗi kết nối (500/Timeout)")
    void earnPoints_ServerDown_ThrowsIllegalState() {
        // Arrange
        when(restTemplate.exchange(anyString(), any(), any(), any(Class.class)))
                .thenThrow(new RestClientException("Connection refused"));

        // Act & Assert
        IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> 
            loyaltyServiceClient.earnPointsFromOrder("user1", "order1", BigDecimal.TEN)
        );
        assertTrue(thrown.getMessage().contains("Unable to grant order loyalty points"));
    }
}