package com.fashion.reviewservice.client;

import com.fashion.reviewservice.client.dto.EarnReviewPointsRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
public class LoyaltyServiceClient {

    private final RestTemplate restTemplate;

    @Value("${review.integration.promotion-service-url:http://localhost:8085}")
    private String promotionServiceUrl;

    public void earnReviewPoints(String userId, String reviewId) {
        EarnReviewPointsRequest request = EarnReviewPointsRequest.builder()
                .userId(userId)
                .reviewId(reviewId)
                .description("Earn points from review-service")
                .build();

        String url = promotionServiceUrl + "/api/v1/promotions/loyalty/earn/review";
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-User-Id", userId);

        try {
            ResponseEntity<Object> ignored = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    new HttpEntity<>(request, headers),
                    Object.class
            );
        } catch (HttpClientErrorException ex) {
            String message = extractMessage(ex.getResponseBodyAsString(), "Unable to grant points for review");
            throw new IllegalArgumentException(message);
        } catch (RestClientException ex) {
            throw new IllegalStateException("Unable to connect promotion-service to grant points");
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
