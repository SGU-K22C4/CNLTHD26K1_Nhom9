package com.fashion.cartservice.controller;

import com.fashion.cartservice.entity.Cart;
import com.fashion.cartservice.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @GetMapping
    public ResponseEntity<Cart> getCart(@RequestHeader("X-User-Id") String userId) {
        return ResponseEntity.ok(cartService.getCart(userId));
    }

    @PostMapping("/items")
    public ResponseEntity<Cart> addItem(
            @RequestHeader("X-User-Id") String userId,
            @RequestBody Cart.CartItem item) {
        return ResponseEntity.ok(cartService.addItem(userId, item));
    }

    @PatchMapping("/items/{productId}")
    public ResponseEntity<Cart> updateQuantity(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable Long productId,
            @RequestParam(required = false) String color,
            @RequestParam(required = false) String size,
            @RequestParam int quantity) {
        return ResponseEntity.ok(cartService.updateQuantity(userId, productId, color, size, quantity));
    }

    @DeleteMapping("/items/{productId}")
    public ResponseEntity<Cart> removeItem(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable Long productId,
            @RequestParam(required = false) String color,
            @RequestParam(required = false) String size) {
        return ResponseEntity.ok(cartService.removeItem(userId, productId, color, size));
    }

    @DeleteMapping
    public ResponseEntity<Map<String, String>> clearCart(@RequestHeader("X-User-Id") String userId) {
        cartService.clearCart(userId);
        return ResponseEntity.ok(Map.of("message", "Cart cleared"));
    }

    @GetMapping("/count")
    public ResponseEntity<Map<String, Integer>> getCount(@RequestHeader("X-User-Id") String userId) {
        Cart cart = cartService.getCart(userId);
        int count = cart.getItems().stream().mapToInt(Cart.CartItem::getQuantity).sum();
        return ResponseEntity.ok(Map.of("count", count));
    }
}
