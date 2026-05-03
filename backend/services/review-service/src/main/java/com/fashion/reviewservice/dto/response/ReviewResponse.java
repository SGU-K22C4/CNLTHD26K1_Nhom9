package com.fashion.reviewservice.dto.response;

import com.fashion.reviewservice.entity.Review;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
public class ReviewResponse {
    private String id;
    private String reviewId;
    private String userId;
    private String productId;
    private String orderId;
    private int star;
    private int rating;
    private String title;
    private String content;
    private String comment;
    private List<String> images;
    private List<String> imageUrls;
    private boolean visible;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ReviewResponse from(Review review) {
        List<String> imageList = review.getImages() == null
                ? List.of()
                : new ArrayList<>(review.getImages());

        String responseId = review.getReviewId() == null || review.getReviewId().isBlank()
                ? review.getId()
                : review.getReviewId();

        return ReviewResponse.builder()
                .id(responseId)
                .reviewId(review.getReviewId())
                .userId(review.getUserId())
                .productId(review.getProductId())
                .orderId(review.getOrderId())
                .star(review.getStar())
                .rating(review.getStar())
                .title(review.getTitle())
                .content(review.getContent())
                .comment(review.getContent())
                .images(imageList)
                .imageUrls(imageList)
                .visible(review.isVisible())
                .createdAt(review.getCreatedAt())
                .updatedAt(review.getUpdatedAt())
                .build();
    }
}
