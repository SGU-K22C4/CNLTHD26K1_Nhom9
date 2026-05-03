package com.fashion.orderservice.client;

import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * REST client for Product Service.
 * Replaces direct cross-database JdbcTemplate queries to maintain
 * strict Database-per-Service isolation in the Microservices architecture.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ProductServiceClient {

    private final RestTemplate restTemplate;

    @Value("${order.integration.product-service-url:${PRODUCT_SERVICE_URL:http://localhost:8082}}")
    private String productServiceUrl;

    /**
     * Check whether a product exists by calling Product Service's REST API.
     *
     * @param productId the product ID to verify
     * @return true if product exists, false if not found
     * @throws IllegalStateException if Product Service is unreachable
     */
    public boolean productExists(String productId) {
        String url = productServiceUrl + "/api/v1/products/" + productId;
        try {
            ResponseEntity<Object> response = restTemplate.exchange(
                    url, HttpMethod.GET, null, Object.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (HttpClientErrorException.NotFound ex) {
            log.warn("Product not found while verifying productId={} via url={}", productId, url);
            return false;
        } catch (HttpClientErrorException ex) {
            log.warn("Product verification failed with status={} for productId={} via url={} body={}",
                    ex.getStatusCode(), productId, url, ex.getResponseBodyAsString());
            return false;
        } catch (RestClientException ex) {
            log.error("Product service call failed for productId={} via url={}", productId, url, ex);
            throw new IllegalStateException(
                    "Unable to verify product at this time. Product Service may be unavailable.", ex);
        }
    }
}
