package com.fashion.cartservice.service;

import com.fashion.cartservice.dto.response.CartItemResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private HashOperations<String, Object, Object> hashOperations;

    private TrackingRedisTemplate redisTemplate;
    private CartService cartService;

    private final String userId = "user123";
    private final String cartKey = "cart:user123";
    private final String variantId = "VAR1";

    @BeforeEach
    void setUp() {
        redisTemplate = new TrackingRedisTemplate(hashOperations);
        cartService = new CartService(redisTemplate);
    }

    @Test
    void should_ReturnCartItems_When_CartContainsEntries() {
        when(hashOperations.entries(cartKey)).thenReturn(Map.of(variantId, "2"));

        List<CartItemResponse> result = cartService.getCart(userId);

        assertEquals(1, result.size());
        assertEquals(variantId, result.get(0).getVariantSizeId());
        assertEquals(2, result.get(0).getQuantity());
    }

    @Test
    void should_AddItemAndRefreshTtl_When_AddingItemToCart() {
        when(hashOperations.entries(cartKey)).thenReturn(Map.of(variantId, "5"));

        List<CartItemResponse> result = cartService.addItem(userId, variantId, 5);

        verify(hashOperations).increment(cartKey, variantId, 5);
        assertEquals(cartKey, redisTemplate.lastExpireKey);
        assertEquals(604800L, redisTemplate.lastExpireTimeout);
        assertEquals(TimeUnit.SECONDS, redisTemplate.lastExpireUnit);
        assertEquals(5, result.get(0).getQuantity());
    }

    @Test
    void should_UpdateQuantityAndRefreshTtl_When_ItemExistsInCart() {
        when(hashOperations.get(cartKey, variantId)).thenReturn("2");
        when(hashOperations.entries(cartKey)).thenReturn(Map.of(variantId, "10"));

        List<CartItemResponse> result = cartService.updateQuantity(userId, variantId, 10);

        verify(hashOperations).put(cartKey, variantId, "10");
        assertEquals(cartKey, redisTemplate.lastExpireKey);
        assertEquals(10, result.get(0).getQuantity());
    }

    @Test
    void should_ThrowIllegalArgumentException_When_UpdatingMissingCartItem() {
        when(hashOperations.get(cartKey, variantId)).thenReturn(null);

        assertThrows(IllegalArgumentException.class, () -> cartService.updateQuantity(userId, variantId, 10));
    }

    @Test
    void should_RemoveItemAndReturnUpdatedCart_When_ItemExists() {
        when(hashOperations.delete(cartKey, variantId)).thenReturn(1L);
        when(hashOperations.entries(cartKey)).thenReturn(Collections.emptyMap());

        List<CartItemResponse> result = cartService.removeItem(userId, variantId);

        verify(hashOperations).delete(cartKey, variantId);
        assertEquals(cartKey, redisTemplate.lastExpireKey);
        assertTrue(result.isEmpty());
    }

    @Test
    void should_ThrowIllegalArgumentException_When_RemovingMissingCartItem() {
        when(hashOperations.delete(cartKey, variantId)).thenReturn(0L);

        assertThrows(IllegalArgumentException.class, () -> cartService.removeItem(userId, variantId));
    }

    @Test
    void should_ReturnTotalItemCount_When_CartContainsMultipleEntries() {
        when(hashOperations.values(cartKey)).thenReturn(List.of("2", "3", "5"));

        int count = cartService.getItemCount(userId);

        assertEquals(10, count);
    }

    @Test
    void should_DeleteCartKey_When_ClearingCart() {
        cartService.clearCart(userId);

        assertEquals(cartKey, redisTemplate.lastDeletedKey);
    }

    private static class TrackingRedisTemplate extends RedisTemplate<String, Object> {
        private final HashOperations<String, Object, Object> hashOperations;
        private String lastExpireKey;
        private long lastExpireTimeout;
        private TimeUnit lastExpireUnit;
        private String lastDeletedKey;

        TrackingRedisTemplate(HashOperations<String, Object, Object> hashOperations) {
            this.hashOperations = hashOperations;
        }

        @Override
        public HashOperations<String, Object, Object> opsForHash() {
            return hashOperations;
        }

        @Override
        public Boolean expire(String key, long timeout, TimeUnit unit) {
            this.lastExpireKey = key;
            this.lastExpireTimeout = timeout;
            this.lastExpireUnit = unit;
            return true;
        }

        @Override
        public Boolean delete(String key) {
            this.lastDeletedKey = key;
            return true;
        }
    }
}
