package com.fashion.productservice.service.impl;

import com.fashion.productservice.dto.response.ProductResponse;
import com.fashion.productservice.entity.Product;
import com.fashion.productservice.entity.Wishlist;
import com.fashion.productservice.mapper.ProductMapper;
import com.fashion.productservice.repository.ProductRepository;
import com.fashion.productservice.repository.WishlistRepository;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
    void should_ReturnMappedWishlist_When_WishlistExists() {
        String userId = "user123";
        Product product = Product.builder().id("P1").name("Vay").build();
        Wishlist wishlist = Wishlist.builder().product(product).userId(userId).build();
        ProductResponse mappedResponse = ProductResponse.builder().id("P1").name("Vay").build();

        when(wishlistRepository.findByUserId(userId, PageRequest.of(0, 10))).thenReturn(new PageImpl<>(List.of(wishlist)));
        when(productMapper.toResponse(product)).thenReturn(mappedResponse);

        Page<ProductResponse> result = wishlistService.getWishlist(userId, PageRequest.of(0, 10));

        assertEquals(1, result.getTotalElements());
        assertEquals("Vay", result.getContent().get(0).getName());
    }

    @Test
    void should_SaveWishlist_When_ProductIsNotYetWishlisted() {
        String userId = "user123";
        String productId = "P1";
        Product product = Product.builder().id(productId).build();

        when(wishlistRepository.existsByUserIdAndProduct_Id(userId, productId)).thenReturn(false);
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        wishlistService.addToWishlist(userId, productId);

        verify(productRepository).findById(productId);
        verify(wishlistRepository).save(any(Wishlist.class));
    }

    @Test
    void should_NotSaveWishlist_When_ProductAlreadyExistsInWishlist() {
        String userId = "user123";
        String productId = "P1";

        when(wishlistRepository.existsByUserIdAndProduct_Id(userId, productId)).thenReturn(true);

        wishlistService.addToWishlist(userId, productId);

        verify(productRepository, never()).findById(productId);
        verify(wishlistRepository, never()).save(any(Wishlist.class));
    }

    @Test
    void should_ThrowRuntimeException_When_ProductDoesNotExist() {
        String userId = "user123";
        String productId = "UNKNOWN";

        when(wishlistRepository.existsByUserIdAndProduct_Id(userId, productId)).thenReturn(false);
        when(productRepository.findById(productId)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> wishlistService.addToWishlist(userId, productId));

        assertEquals("Product not found: UNKNOWN", exception.getMessage());
    }

    @Test
    void should_DeleteWishlistItem_When_RemoveIsRequested() {
        wishlistService.removeFromWishlist("user123", "P1");

        verify(wishlistRepository).deleteByUserIdAndProduct_Id("user123", "P1");
    }

    @Test
    void should_ReturnWishlistProductIds_When_UserHasWishlist() {
        List<String> mockIds = List.of("P1", "P2");
        when(wishlistRepository.findProductIdsByUserId("user123")).thenReturn(mockIds);

        List<String> result = wishlistService.getWishlistProductIds("user123");

        assertEquals(mockIds, result);
        verify(wishlistRepository).findProductIdsByUserId("user123");
    }
}
