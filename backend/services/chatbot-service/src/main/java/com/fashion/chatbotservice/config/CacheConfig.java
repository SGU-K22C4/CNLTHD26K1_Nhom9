package com.fashion.chatbotservice.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Cấu hình Caffeine Cache cho chatbot service — Phase 2A.
 *
 * <p><b>Lỗi trước đó:</b> Dùng {@code CaffeineCacheManager.setCaffeine()} áp dụng
 * <em>cùng một TTL 5 phút</em> cho tất cả cache, bao gồm cả {@code knowledgeBase}
 * vốn cần TTL 15 phút. Review.md ghi nhận đây là bug.
 *
 * <p><b>Fix:</b> Dùng {@code SimpleCacheManager} với danh sách {@code CaffeineCache}
 * được khởi tạo riêng biệt, mỗi cache có TTL và size riêng.
 *
 * <table border="1">
 * <tr><th>Cache name</th><th>TTL</th><th>Max size</th><th>Mục đích</th></tr>
 * <tr><td>productSearch</td><td>5 phút</td><td>500</td><td>Kết quả tìm sản phẩm</td></tr>
 * <tr><td>knowledgeBase</td><td>15 phút</td><td>200</td><td>Knowledge base / FAQ</td></tr>
 * <tr><td>intentEmbeddings</td><td>60 phút</td><td>50</td><td>Intent embedding vectors</td></tr>
 * </table>
 */
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCache productSearchCache = buildCache(
                "productSearch",
                5,  TimeUnit.MINUTES, 500
        );

        CaffeineCache knowledgeBaseCache = buildCache(
                "knowledgeBase",
                15, TimeUnit.MINUTES, 200
        );

        CaffeineCache intentEmbeddingsCache = buildCache(
                "intentEmbeddings",
                60, TimeUnit.MINUTES, 50
        );

        SimpleCacheManager manager = new SimpleCacheManager();
        manager.setCaches(List.of(productSearchCache, knowledgeBaseCache, intentEmbeddingsCache));
        return manager;
    }

    /**
     * Factory helper: tạo CaffeineCache với TTL và max size cụ thể.
     */
    private CaffeineCache buildCache(String name, long ttlValue, TimeUnit ttlUnit, long maxSize) {
        return new CaffeineCache(name,
                Caffeine.newBuilder()
                        .expireAfterWrite(ttlValue, ttlUnit)
                        .maximumSize(maxSize)
                        .recordStats()
                        .build());
    }
}
