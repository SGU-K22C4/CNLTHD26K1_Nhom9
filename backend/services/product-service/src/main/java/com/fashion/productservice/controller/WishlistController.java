package com.fashion.productservice.controller;

import com.fashion.productservice.dto.response.ProductResponse;
import com.fashion.productservice.service.WishlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/wishlists")
@RequiredArgsConstructor
public class WishlistController {

    private final WishlistService wishlistService;

    /**
     * GET /api/v1/wishlists — Lấy danh sách sản phẩm yêu thích (paginated)
     */
    @GetMapping
    public ResponseEntity<Page<ProductResponse>> getWishlist(
            @RequestHeader("X-User-Id") String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(
                wishlistService.getWishlist(userId, PageRequest.of(page, size, Sort.by("createdAt").descending()))
        );
    }

    /**
     * GET /api/v1/wishlists/ids — Lấy list product IDs trong wishlist (cho frontend check trạng thái heart)
     */
    @GetMapping("/ids")
    public ResponseEntity<List<String>> getWishlistIds(
            @RequestHeader("X-User-Id") String userId
    ) {
        return ResponseEntity.ok(wishlistService.getWishlistProductIds(userId));
    }

    /**
     * POST /api/v1/wishlists/{productId} — Thêm sản phẩm vào wishlist
     */
    @PostMapping("/{productId}")
    public ResponseEntity<Map<String, String>> addToWishlist(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable String productId
    ) {
        wishlistService.addToWishlist(userId, productId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("message", "Product added to wishlist"));
    }

    /**
     * DELETE /api/v1/wishlists/{productId} — Xóa sản phẩm khỏi wishlist
     */
    @DeleteMapping("/{productId}")
    public ResponseEntity<Map<String, String>> removeFromWishlist(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable String productId
    ) {
        wishlistService.removeFromWishlist(userId, productId);
        return ResponseEntity.ok(Map.of("message", "Product removed from wishlist"));
    }

    /**
     * GET /api/v1/wishlists/{productId}/check — Kiểm tra sản phẩm có trong wishlist không
     */
    @GetMapping("/{productId}/check")
    public ResponseEntity<Map<String, Boolean>> checkWishlisted(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable String productId
    ) {
        return ResponseEntity.ok(Map.of("wishlisted", wishlistService.isWishlisted(userId, productId)));
    }
}
