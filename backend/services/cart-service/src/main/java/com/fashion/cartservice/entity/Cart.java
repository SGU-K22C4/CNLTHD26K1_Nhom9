package com.fashion.cartservice.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@RedisHash("cart")
public class Cart implements Serializable {

    @Id
    private String userId;

    @Builder.Default
    private List<CartItem> items = new ArrayList<>();

    @TimeToLive(unit = TimeUnit.DAYS)
    @Builder.Default
    private long ttl = 7L;

    public BigDecimal getTotal() {
        return items.stream()
                .map(i -> i.getPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CartItem implements Serializable {
        private Long productId;
        private String productName;
        private String productSlug;
        private String imageUrl;
        private String color;
        private String size;
        private int quantity;
        private BigDecimal price;
        private Long variantId;
    }
}
