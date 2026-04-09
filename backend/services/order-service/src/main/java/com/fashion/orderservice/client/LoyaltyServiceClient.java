package com.fashion.orderservice.client;

import com.fashion.orderservice.client.dto.LoyaltyMutationResponse;
import com.fashion.orderservice.client.dto.LoyaltyEarnOrderRequest;
import com.fashion.orderservice.client.dto.LoyaltyRedeemRequest;
import com.fashion.orderservice.client.dto.LoyaltyRefundRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
public class LoyaltyServiceClient {

    private final RestTemplate restTemplate;

    @Value("${order.integration.promotion-service-url:http://localhost:8085}")
    private String promotionServiceUrl;

    public LoyaltyMutationResponse redeemPoints(String userId, String orderId, BigDecimal orderAmount, Integer requestedPoints) {
        LoyaltyRedeemRequest request = LoyaltyRedeemRequest.builder()
                .userId(userId)
                .refId(orderId)
                .orderAmount(orderAmount)
                .requestedPoints(requestedPoints)
                .description("Redeem points from order-service")
                .build();

        String url = promotionServiceUrl + "/api/v1/promotions/loyalty/redeem";
        return post(url, request, "Unable to redeem points at this time");
    }

    public LoyaltyMutationResponse refundPoints(String userId, String orderId) {
        LoyaltyRefundRequest request = LoyaltyRefundRequest.builder()
                .userId(userId)
                .refId(orderId)
                .description("Refund points due to cancelled order")
                .build();

        String url = promotionServiceUrl + "/api/v1/promotions/loyalty/refund";
        return post(url, request, "Unable to refund points at this time");
    }

    public void earnPointsFromOrder(String userId, String orderId, BigDecimal netAmount) {
        LoyaltyEarnOrderRequest request = LoyaltyEarnOrderRequest.builder()
                .userId(userId)
                .orderId(orderId)
                .netAmount(netAmount)
                .description("Earn points from successful order")
                .build();

        String url = promotionServiceUrl + "/api/v1/promotions/loyalty/earn/order";
        post(url, request, "Unable to grant order loyalty points at this time");
    }

    private LoyaltyMutationResponse post(String url, Object body, String fallbackErrorMessage) {
        try {
            ResponseEntity<LoyaltyMutationResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    new HttpEntity<>(body),
                    LoyaltyMutationResponse.class
            );
            return response.getBody();
        } catch (HttpClientErrorException ex) {
            String message = extractMessage(ex.getResponseBodyAsString(), ex.getStatusText());
            throw new IllegalArgumentException(message);
        } catch (RestClientException ex) {
            throw new IllegalStateException(fallbackErrorMessage);
        }
    }

    private String extractMessage(String body, String fallback) {
        if (body == null || body.isBlank()) {
            return fallback;
        }

        String marker = "\"message\":\"";
        int start = body.indexOf(marker);
        if (start < 0) {
            return fallback;
        }
        start += marker.length();
        int end = body.indexOf('"', start);
        if (end <= start) {
            return fallback;
        }
        return body.substring(start, end);
    }
}
