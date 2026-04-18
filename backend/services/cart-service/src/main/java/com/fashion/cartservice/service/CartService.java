package com.fashion.cartservice.service;

import com.fashion.cartservice.dto.response.CartItemResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class CartService {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String CART_KEY_PREFIX = "cart:";
    private static final long CART_TTL_SECONDS = 604800; // 7 ngày

    /**
     * Lấy toàn bộ giỏ hàng của user
     * Redis: HGETALL cart:{userId}
     */
    public List<CartItemResponse> getCart(String userId) {
        String key = buildKey(userId);
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(key);

        if (entries.isEmpty()) {
            return Collections.emptyList();
        }

        List<CartItemResponse> items = new ArrayList<>();
        entries.forEach((field, value) -> items.add(
                CartItemResponse.builder()
                        .variantSizeId(field.toString())
                        .quantity(Integer.parseInt(value.toString()))
                        .build()
        ));
        return items;
    }

    /**
     * Thêm sản phẩm vào giỏ hàng (cộng dồn số lượng nếu đã tồn tại)
     * Redis: HINCRBY cart:{userId} {variantSizeId} {quantity}
     */
    public List<CartItemResponse> addItem(String userId, String variantSizeId, int quantity) {
        String key = buildKey(userId);
        redisTemplate.opsForHash().increment(key, variantSizeId, quantity);
        refreshTtl(key);
        return getCart(userId);
    }

    /**
     * Cập nhật số lượng sản phẩm (đặt lại giá trị mới)
     * Redis: HSET cart:{userId} {variantSizeId} {quantity}
     */
    public List<CartItemResponse> updateQuantity(String userId, String variantSizeId, int quantity) {
        String key = buildKey(userId);
        Object existing = redisTemplate.opsForHash().get(key, variantSizeId);
        if (existing == null) {
            throw new IllegalArgumentException("Item not found in cart: " + variantSizeId);
        }
        redisTemplate.opsForHash().put(key, variantSizeId, String.valueOf(quantity));
        refreshTtl(key);
        return getCart(userId);
    }

    /**
     * Xóa 1 sản phẩm khỏi giỏ hàng
     * Redis: HDEL cart:{userId} {variantSizeId}
     */
    public List<CartItemResponse> removeItem(String userId, String variantSizeId) {
        String key = buildKey(userId);
        Long removed = redisTemplate.opsForHash().delete(key, variantSizeId);
        if (removed == 0) {
            throw new IllegalArgumentException("Item not found in cart: " + variantSizeId);
        }
        refreshTtl(key);
        return getCart(userId);
    }

    /**
     * Xóa toàn bộ giỏ hàng
     * Redis: DEL cart:{userId}
     */
    public void clearCart(String userId) {
        redisTemplate.delete(buildKey(userId));
    }

    /**
     * Đếm tổng số lượng sản phẩm trong giỏ hàng
     * Redis: HVALS cart:{userId} + sum
     */
    public int getItemCount(String userId) {
        String key = buildKey(userId);
        List<Object> values = redisTemplate.opsForHash().values(key);
        return values.stream()
                .mapToInt(v -> Integer.parseInt(v.toString()))
                .sum();
    }

    /**
     * Gia hạn TTL 7 ngày sau mỗi lần tương tác
     * Redis: EXPIRE cart:{userId} 604800
     */
    private void refreshTtl(String key) {
        redisTemplate.expire(key, CART_TTL_SECONDS, TimeUnit.SECONDS);
    }

    private String buildKey(String userId) {
        return CART_KEY_PREFIX + userId;
    }
}
