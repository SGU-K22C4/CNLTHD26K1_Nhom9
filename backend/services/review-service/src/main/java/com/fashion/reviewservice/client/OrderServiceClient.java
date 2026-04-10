package com.fashion.reviewservice.client;

import com.fashion.reviewservice.client.dto.OrderSummary;
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

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class OrderServiceClient {

    private final RestTemplate restTemplate;

    @Value("${review.integration.order-service-url:http://localhost:8084}")
    private String orderServiceUrl;

    public Optional<OrderSummary> findOrderForUser(String orderId, String userId) {
        long numericOrderId = parseOrderId(orderId);
        String url = orderServiceUrl + "/api/v1/orders/" + numericOrderId;

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-User-Id", userId);

        try {
            ResponseEntity<OrderSummary> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    OrderSummary.class
            );
            return Optional.ofNullable(response.getBody());
        } catch (HttpClientErrorException.NotFound ex) {
            return Optional.empty();
        } catch (HttpClientErrorException ex) {
            throw new IllegalStateException("Không thể xác thực đơn hàng lúc này. Vui lòng thử lại.");
        } catch (RestClientException ex) {
            throw new IllegalStateException("Không thể kết nối order-service để kiểm tra điều kiện đánh giá.");
        }
    }

    private long parseOrderId(String orderId) {
        try {
            return Long.parseLong(orderId);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("orderId phải là số hợp lệ");
        }
    }
}
