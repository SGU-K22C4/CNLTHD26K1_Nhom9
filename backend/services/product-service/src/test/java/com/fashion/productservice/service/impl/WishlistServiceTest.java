package com.fashion.productservice.service.impl;

import com.fashion.productservice.dto.response.ProductResponse;
import com.fashion.productservice.entity.Product;
import com.fashion.productservice.entity.Wishlist;
import com.fashion.productservice.mapper.ProductMapper;
import com.fashion.productservice.repository.ProductRepository;
import com.fashion.productservice.repository.WishlistRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WishlistServiceTest {

    @Mock
    private WishlistRepository wishlistRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductMapper productMapper;

    @InjectMocks
    private WishlistServiceImpl wishlistService;

@Test
@DisplayName("Test lấy danh sách Wishlist")
void testGetWishlist_Success() {
    String userId = "user123";
    Product product = Product.builder().id("P1").name("Váy").build();
    Wishlist wishlist = Wishlist.builder().product(product).userId(userId).build();
    
    // Dùng mock để né lỗi 'is not public'
    ProductResponse mockResponse = mock(ProductResponse.class);
    lenient().when(mockResponse.getId()).thenReturn("P1");
    lenient().when(mockResponse.getName()).thenReturn("Váy");

    lenient().when(wishlistRepository.findByUserId(userId, PageRequest.of(0, 10)))
        .thenReturn(new PageImpl<>(List.of(wishlist)));
    lenient().when(productMapper.toResponse(product)).thenReturn(mockResponse);

    Page<ProductResponse> result = wishlistService.getWishlist(userId, PageRequest.of(0, 10));

    assertNotNull(result);
    assertEquals("Váy", result.getContent().get(0).getName());
}
@Test
    @DisplayName("Test thêm vào Wishlist lần đầu thành công")
    void testAddToWishlist_Success() {
        // Arrange
        String userId = "user123";
        String productId = "P1";
        
        // Giả lập chưa có trong wishlist
        when(wishlistRepository.existsByUserIdAndProduct_Id(userId, productId)).thenReturn(false);
        // Giả lập tìm thấy sản phẩm trong kho
        when(productRepository.findById(productId)).thenReturn(Optional.of(new Product()));

        // Act
        wishlistService.addToWishlist(userId, productId);

        // Assert
        verify(wishlistRepository, times(1)).save(any(Wishlist.class));
        verify(productRepository, times(1)).findById(productId);
    }

    @Test
    @DisplayName("Test thêm vào Wishlist khi đã tồn tại (không làm gì thêm)")
    void testAddToWishlist_AlreadyExists() {
        // Arrange
        String userId = "user123";
        String productId = "P1";
        
        // Giả lập sản phẩm ĐÃ tồn tại trong wishlist
        when(wishlistRepository.existsByUserIdAndProduct_Id(userId, productId)).thenReturn(true);

        // Act
        wishlistService.addToWishlist(userId, productId);

        // Assert
        // Đảm bảo không gọi repo để tìm sản phẩm hay lưu mới (Idempotent)
        verify(productRepository, never()).findById(anyString());
        verify(wishlistRepository, never()).save(any(Wishlist.class));
    }

    @Test
    @DisplayName("Test thêm vào Wishlist nhưng sản phẩm không tồn tại")
    void testAddToWishlist_ProductNotFound() {
        // Arrange
        String userId = "user123";
        String productId = "UNKNOWN";

        when(wishlistRepository.existsByUserIdAndProduct_Id(userId, productId)).thenReturn(false);
        when(productRepository.findById(productId)).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> wishlistService.addToWishlist(userId, productId));
        
        assertTrue(exception.getMessage().contains("Product not found"));
    }

    @Test
    @DisplayName("Test xóa sản phẩm khỏi Wishlist")
    void testRemoveFromWishlist_Success() {
        // Arrange
        String userId = "user123";
        String productId = "P1";

        // Act
        wishlistService.removeFromWishlist(userId, productId);

        // Assert
        verify(wishlistRepository, times(1)).deleteByUserIdAndProduct_Id(userId, productId);
    }

    @Test
    @DisplayName("Test lấy danh sách ID sản phẩm yêu thích")
    void testGetWishlistProductIds_Success() {
        // Arrange
        String userId = "user123";
        List<String> mockIds = List.of("P1", "P2");
        when(wishlistRepository.findProductIdsByUserId(userId)).thenReturn(mockIds);

        // Act
        List<String> result = wishlistService.getWishlistProductIds(userId);

        // Assert
        assertEquals(2, result.size());
        assertTrue(result.contains("P1"));
        verify(wishlistRepository).findProductIdsByUserId(userId);
    }
}