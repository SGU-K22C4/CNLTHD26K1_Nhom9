package com.fashion.productservice.service;

import com.fashion.productservice.dto.response.ProductResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

@SuppressWarnings("null")
public interface WishlistService {

    Page<ProductResponse> getWishlist(String userId, Pageable pageable);

    List<String> getWishlistProductIds(String userId);

    void addToWishlist(String userId, String productId);

    void removeFromWishlist(String userId, String productId);

    boolean isWishlisted(String userId, String productId);
}
