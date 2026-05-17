package com.fashion.chatbotservice.config;

import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Keeps resilience policy in one place so downstream behavior is consistent
 * across product/promotion/order/review/cart calls without scattering magic
 * numbers inside tool methods.
 */
@Configuration
public class ResilienceConfig {

    @Value("${chatbot.resilience.failure-rate-threshold:50}")
    private float failureRateThreshold;

    @Value("${chatbot.resilience.sliding-window-size:10}")
    private int slidingWindowSize;

    @Value("${chatbot.resilience.minimum-number-of-calls:5}")
    private int minimumNumberOfCalls;

    @Value("${chatbot.resilience.wait-duration-ms:250}")
    private long waitDurationMs;

    @Value("${chatbot.resilience.retry.max-attempts:2}")
    private int maxAttempts;

    @Value("${chatbot.resilience.timeout-ms:3000}")
    private long timeoutMs;

    @Value("${chatbot.resilience.bulkhead.max-concurrent-calls:12}")
    private int maxConcurrentCalls;

    @Bean(destroyMethod = "shutdown")
    public ExecutorService resilienceExecutorService() {
        return Executors.newFixedThreadPool(8);
    }

    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .failureRateThreshold(failureRateThreshold)
                .slidingWindowSize(slidingWindowSize)
                .minimumNumberOfCalls(minimumNumberOfCalls)
                .waitDurationInOpenState(Duration.ofMillis(waitDurationMs))
                .build();
        return CircuitBreakerRegistry.of(config);
    }

    @Bean
    public RetryRegistry retryRegistry() {
        RetryConfig config = RetryConfig.custom()
                .maxAttempts(maxAttempts)
                .waitDuration(Duration.ofMillis(waitDurationMs))
                .failAfterMaxAttempts(true)
                .build();
        return RetryRegistry.of(config);
    }

    @Bean
    public TimeLimiterRegistry timeLimiterRegistry() {
        TimeLimiterConfig config = TimeLimiterConfig.custom()
                .timeoutDuration(Duration.ofMillis(timeoutMs))
                .cancelRunningFuture(true)
                .build();
        return TimeLimiterRegistry.of(config);
    }

    @Bean
    public BulkheadRegistry bulkheadRegistry() {
        BulkheadConfig config = BulkheadConfig.custom()
                .maxConcurrentCalls(maxConcurrentCalls)
                .maxWaitDuration(Duration.ofMillis(waitDurationMs))
                .build();
        return BulkheadRegistry.of(config);
    }
}
