package com.fashion.chatbotservice.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Cấu hình Rate Limiting cho LLM calls — Phase 3B.
 *
 * <p>Hai tầng rate limit:
 * <ul>
 *   <li>Global: toàn bộ service (200 req/phút mặc định)</li>
 *   <li>Per-user: mỗi userId tối đa 20 req/phút</li>
 * </ul>
 *
 * <p><b>Bug fix so với phiên bản trước:</b>
 * <ul>
 *   <li>Per-user limiters trước đây dùng {@code ConcurrentHashMap} không có eviction
 *       → accumulate vô hạn theo số users. Fix: dùng Caffeine cache với TTL 1 giờ.</li>
 *   <li>Guest users (userId null/blank) trước đây bỏ qua rate limiting hoàn toàn.
 *       Fix: fallback về IP-based key "guest-{sessionId}" hoặc "guest-unknown".</li>
 * </ul>
 */
@Configuration
@Slf4j
public class RateLimitConfig {

    @Value("${chatbot.ratelimit.user-requests-per-minute:20}")
    private int userRequestsPerMinute;

    @Value("${chatbot.ratelimit.global-requests-per-minute:200}")
    private int globalRequestsPerMinute;

    @Value("${chatbot.ratelimit.timeout-ms:1000}")
    private long timeoutMs;

    /**
     * Per-user RateLimiter store với Caffeine eviction.
     *
     * <p>TTL 1 giờ: limiter tự động bị xóa sau 1 giờ không có request.
     * Max size 10000: đủ cho lượng users đồng thời, tránh memory unbounded.
     */
    private final Cache<String, RateLimiter> userRateLimiters = Caffeine.newBuilder()
            .expireAfterAccess(1, TimeUnit.HOURS)
            .maximumSize(10_000)
            .removalListener((key, value, cause) ->
                    log.debug("Per-user RateLimiter evicted: key={}, cause={}", key, cause))
            .build();

    @Bean
    public RateLimiterRegistry rateLimiterRegistry() {
        RateLimiterConfig globalConfig = RateLimiterConfig.custom()
                .limitForPeriod(globalRequestsPerMinute)
                .limitRefreshPeriod(Duration.ofMinutes(1))
                .timeoutDuration(Duration.ofMillis(timeoutMs))
                .build();
        return RateLimiterRegistry.of(globalConfig);
    }

    @Bean
    public RateLimiter globalRateLimiter(RateLimiterRegistry registry) {
        return registry.rateLimiter("chatbot-global");
    }

    /**
     * Resolve rate limit key từ userId và sessionId.
     *
     * <p>Guest users (null/blank userId) không còn bị bỏ qua: dùng sessionId
     * làm key để vẫn có rate limit theo session.
     *
     * @param userId    X-User-Id header (có thể null với guest)
     * @param sessionId session ID (dùng làm fallback cho guest)
     * @return key duy nhất để tra cứu rate limiter
     */
    public String resolveRateLimitKey(String userId, String sessionId) {
        if (userId != null && !userId.isBlank()) {
            return "user:" + userId;
        }
        if (sessionId != null && !sessionId.isBlank()) {
            return "guest:" + sessionId;
        }
        return "guest:unknown";
    }

    /**
     * Lấy hoặc tạo per-user/per-session RateLimiter với Caffeine eviction.
     */
    public RateLimiter getUserRateLimiter(String rateLimitKey) {
        return userRateLimiters.get(rateLimitKey, key -> {
            RateLimiterConfig config = RateLimiterConfig.custom()
                    .limitForPeriod(userRequestsPerMinute)
                    .limitRefreshPeriod(Duration.ofMinutes(1))
                    .timeoutDuration(Duration.ofMillis(timeoutMs))
                    .build();
            log.debug("Created rate limiter for key: {}", key);
            return RateLimiter.of("ratelimit-" + key, config);
        });
    }

    /**
     * Kiểm tra và consume 1 permit cho user/session.
     *
     * @param userId    X-User-Id header (có thể null)
     * @param sessionId session ID
     * @return true nếu cho phép, false nếu đã vượt giới hạn
     */
    public boolean tryAcquire(String userId, String sessionId) {
        String key = resolveRateLimitKey(userId, sessionId);
        RateLimiter limiter = getUserRateLimiter(key);
        boolean permitted = limiter.acquirePermission();
        if (!permitted) {
            log.warn("Rate limit exceeded for key: {} (userId={}, sessionId={})", key, userId, sessionId);
        }
        return permitted;
    }
}
