package com.fashion.cartservice.controller;

import com.fashion.cartservice.dto.request.AddToCartRequest;
import com.fashion.cartservice.dto.request.UpdateQuantityRequest;
import com.fashion.cartservice.dto.response.CartItemResponse;
import com.fashion.cartservice.service.CartService;
import com.fashion.common.dto.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    /**
     * Lấy toàn bộ giỏ hàng
     * GET /api/v1/cart
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<CartItemResponse>>> getCart(
            @RequestHeader("X-User-Id") String userId) {
        List<CartItemResponse> items = cartService.getCart(userId);
        return ResponseEntity.ok(ApiResponse.success(items));
    }

    /**
     * Thêm sản phẩm vào giỏ hàng
     * POST /api/v1/cart/items
     */
    @PostMapping("/items")
    public ResponseEntity<ApiResponse<List<CartItemResponse>>> addItem(
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody AddToCartRequest request) {
        List<CartItemResponse> items = cartService.addItem(
                userId, request.getVariantSizeId(), request.getQuantity());
        return ResponseEntity.ok(ApiResponse.success("Item added to cart", items));
    }

    /**
     * Cập nhật số lượng sản phẩm
     * PATCH /api/v1/cart/items/{variantSizeId}
     */
    @PatchMapping("/items/{variantSizeId}")
    public ResponseEntity<ApiResponse<List<CartItemResponse>>> updateQuantity(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable String variantSizeId,
            @Valid @RequestBody UpdateQuantityRequest request) {
        List<CartItemResponse> items = cartService.updateQuantity(
                userId, variantSizeId, request.getQuantity());
        return ResponseEntity.ok(ApiResponse.success("Quantity updated", items));
    }

    /**
     * Xóa 1 sản phẩm khỏi giỏ hàng
     * DELETE /api/v1/cart/items/{variantSizeId}
     */
    @DeleteMapping("/items/{variantSizeId}")
    public ResponseEntity<ApiResponse<List<CartItemResponse>>> removeItem(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable String variantSizeId) {
        List<CartItemResponse> items = cartService.removeItem(userId, variantSizeId);
        return ResponseEntity.ok(ApiResponse.success("Item removed from cart", items));
    }

    /**
     * Xóa toàn bộ giỏ hàng
     * DELETE /api/v1/cart
     */
    @DeleteMapping
    public ResponseEntity<ApiResponse<String>> clearCart(
            @RequestHeader("X-User-Id") String userId) {
        cartService.clearCart(userId);
        return ResponseEntity.ok(ApiResponse.success("Cart cleared", null));
    }

    /**
     * Đếm tổng số lượng sản phẩm
     * GET /api/v1/cart/count
     */
    @GetMapping("/count")
    public ResponseEntity<ApiResponse<Integer>> getCount(
            @RequestHeader("X-User-Id") String userId) {
        int count = cartService.getItemCount(userId);
        return ResponseEntity.ok(ApiResponse.success(count));
    }
}
