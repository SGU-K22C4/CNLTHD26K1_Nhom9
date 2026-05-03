package com.fashion.cartservice.service;

import com.fashion.cartservice.dto.response.CartItemResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private HashOperations<String, Object, Object> hashOperations;

    @InjectMocks
    private CartService cartService;

    private final String userId = "user123";
    private final String cartKey = "cart:user123";
    private final String variantId = "VAR1";

    @BeforeEach
    void setUp() {
        // Vì CartService gọi redisTemplate.opsForHash() nhiều lần, ta mock nó trả về hashOperations giả
        lenient().when(redisTemplate.opsForHash()).thenReturn(hashOperations);
    }

    @Test
    @DisplayName("Test lấy giỏ hàng thành công")
    void getCart_Success() {
        // Arrange
        Map<Object, Object> mockEntries = Map.of(variantId, "2");
        when(hashOperations.entries(cartKey)).thenReturn(mockEntries);

        // Act
        List<CartItemResponse> result = cartService.getCart(userId);

        // Assert
        assertEquals(1, result.size());
        assertEquals(variantId, result.get(0).getVariantSizeId());
        assertEquals(2, result.get(0).getQuantity());
    }

    @Test
    @DisplayName("Test thêm sản phẩm vào giỏ (Increment)")
    void addItem_Success() {
        // Arrange
        // Khi addItem gọi getCart ở cuối, trả về list có 1 item
        when(hashOperations.entries(cartKey)).thenReturn(Map.of(variantId, "5"));

        // Act
        List<CartItemResponse> result = cartService.addItem(userId, variantId, 5);

        // Assert
        verify(hashOperations).increment(cartKey, variantId, 5); // Quan trọng nhất: check lệnh tăng số lượng
        verify(redisTemplate).expire(eq(cartKey), anyLong(), eq(TimeUnit.SECONDS)); // Check lệnh gia hạn TTL
        assertEquals(1, result.size());
        assertEquals(5, result.get(0).getQuantity());
    }

    @Test
    @DisplayName("Test cập nhật số lượng khi item có tồn tại")
    void updateQuantity_Success() {
        // Arrange
        when(hashOperations.get(cartKey, variantId)).thenReturn("2"); // Giả lập item đang có trong giỏ
        when(hashOperations.entries(cartKey)).thenReturn(Map.of(variantId, "10"));

        // Act
        List<CartItemResponse> result = cartService.updateQuantity(userId, variantId, 10);

        // Assert
        verify(hashOperations).put(cartKey, variantId, "10"); // Check lệnh đặt giá trị mới
        assertEquals(10, result.get(0).getQuantity());
    }

    @Test
    @DisplayName("Test cập nhật số lượng khi item KHÔNG tồn tại - Phải ném lỗi")
    void updateQuantity_NotFound() {
        // Arrange
        when(hashOperations.get(cartKey, variantId)).thenReturn(null);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> 
            cartService.updateQuantity(userId, variantId, 10)
        );
    }

    @Test
    @DisplayName("Test xóa một item khỏi giỏ")
    void removeItem_Success() {
        // Arrange
        when(hashOperations.delete(cartKey, variantId)).thenReturn(1L); // Trả về 1 nghĩa là đã xóa
        when(hashOperations.entries(cartKey)).thenReturn(Collections.emptyMap());

        // Act
        List<CartItemResponse> result = cartService.removeItem(userId, variantId);

        // Assert
        verify(hashOperations).delete(cartKey, variantId);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Test đếm tổng số lượng item (Sum các value)")
    void getItemCount_Success() {
        // Arrange
        List<Object> mockValues = List.of("2", "3", "5"); // Giỏ hàng có 3 loại hàng, tổng là 10 món
        when(hashOperations.values(cartKey)).thenReturn(mockValues);

        // Act
        int count = cartService.getItemCount(userId);

        // Assert
        assertEquals(10, count);
    }

    @Test
    @DisplayName("Test xóa sạch giỏ hàng")
    void clearCart_Success() {
        // Act
        cartService.clearCart(userId);

        // Assert
        verify(redisTemplate).delete(cartKey);
    }
}