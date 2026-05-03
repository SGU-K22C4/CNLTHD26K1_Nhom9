package com.fashion.productservice.service.impl;

import com.fashion.productservice.dto.response.ProductResponse;
import com.fashion.productservice.entity.Product;
import com.fashion.productservice.entity.Wishlist;
import com.fashion.productservice.mapper.ProductMapper;
import com.fashion.productservice.repository.ProductRepository;
import com.fashion.productservice.repository.WishlistRepository;
import com.fashion.productservice.service.WishlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WishlistServiceImpl implements WishlistService {

    private final WishlistRepository wishlistRepository;
    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponse> getWishlist(String userId, Pageable pageable) {
        return wishlistRepository.findByUserId(userId, pageable)
                .map(wishlist -> productMapper.toResponse(wishlist.getProduct()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getWishlistProductIds(String userId) {
        return wishlistRepository.findProductIdsByUserId(userId);
    }

    @Override
    @Transactional
    public void addToWishlist(String userId, String productId) {
        if (wishlistRepository.existsByUserIdAndProduct_Id(userId, productId)) {
            return; // Already wishlisted, idempotent
        }

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found: " + productId));

        Wishlist wishlist = Wishlist.builder()
                .wishlistId(UUID.randomUUID().toString())
                .userId(userId)
                .product(product)
                .build();

        wishlistRepository.save(wishlist);
    }

    @Override
    @Transactional
    public void removeFromWishlist(String userId, String productId) {
        wishlistRepository.deleteByUserIdAndProduct_Id(userId, productId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isWishlisted(String userId, String productId) {
        return wishlistRepository.existsByUserIdAndProduct_Id(userId, productId);
    }
}
