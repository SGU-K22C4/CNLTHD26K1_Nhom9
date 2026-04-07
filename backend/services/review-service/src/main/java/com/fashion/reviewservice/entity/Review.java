package com.fashion.reviewservice.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "reviews")
@CompoundIndexes({
        @CompoundIndex(name = "uniq_user_order_product", def = "{'user_id': 1, 'order_id': 1, 'product_id': 1}", unique = true)
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Review {

    @Id
    private String id;

    @Field("review_id")
    private String reviewId;

    @Field("user_id")
    @Indexed(name = "idx_user_id")
    private String userId;

    @Field("product_id")
    @Indexed(name = "idx_product_id")
    private String productId;

    @Field("order_id")
    @Indexed(name = "idx_order_id")
    private String orderId;

    @Indexed(name = "idx_star")
    private int star;

    private String title;

    private String content;

    @Builder.Default
    private List<String> images = new ArrayList<>();

    @Field("is_visible")
    @Builder.Default
    private boolean visible = true;

    @Field("created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Field("updated_at")
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
}
