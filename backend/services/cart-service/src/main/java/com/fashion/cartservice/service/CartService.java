package com.fashion.cartservice.service;

import com.fashion.cartservice.entity.Cart;
import com.fashion.cartservice.repository.CartRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;

    public Cart getCart(String userId) {
        return cartRepository.findById(userId)
                .orElse(Cart.builder().userId(userId).build());
    }

    public Cart addItem(String userId, Cart.CartItem item) {
        Cart cart = getCart(userId);
        Optional<Cart.CartItem> existing = cart.getItems().stream()
                .filter(i -> i.getProductId().equals(item.getProductId())
                        && matches(i.getColor(), item.getColor())
                        && matches(i.getSize(), item.getSize()))
                .findFirst();

        if (existing.isPresent()) {
            existing.get().setQuantity(existing.get().getQuantity() + item.getQuantity());
        } else {
            cart.getItems().add(item);
        }
        return cartRepository.save(cart);
    }

    public Cart updateQuantity(String userId, Long productId, String color, String size, int quantity) {
        Cart cart = getCart(userId);
        cart.getItems().stream()
                .filter(i -> i.getProductId().equals(productId)
                        && matches(i.getColor(), color)
                        && matches(i.getSize(), size))
                .findFirst()
                .ifPresent(i -> {
                    if (quantity <= 0)
                        cart.getItems().remove(i);
                    else
                        i.setQuantity(quantity);
                });
        return cartRepository.save(cart);
    }

    public Cart removeItem(String userId, Long productId, String color, String size) {
        Cart cart = getCart(userId);
        cart.getItems().removeIf(i -> i.getProductId().equals(productId)
                && matches(i.getColor(), color)
                && matches(i.getSize(), size));
        return cartRepository.save(cart);
    }

    public void clearCart(String userId) {
        cartRepository.deleteById(userId);
    }

    private boolean matches(String a, String b) {
        if (a == null && b == null)
            return true;
        if (a == null || b == null)
            return false;
        return a.equals(b);
    }
}
