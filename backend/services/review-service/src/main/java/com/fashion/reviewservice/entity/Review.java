package com.fashion.reviewservice.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "reviews")
@CompoundIndex(name = "uniq_user_product", def = "{'userId': 1, 'productId': 1}", unique = true)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Review {

    @Id
    private String id;

    private String userId;

    private Long productId;

    private int rating; // 1-5

    private String comment;

    @Builder.Default
    private boolean approved = false;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
