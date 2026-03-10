package com.fashion.reviewservice.controller;

import com.fashion.reviewservice.entity.Review;
import com.fashion.reviewservice.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewRepository reviewRepository;

    @GetMapping("/product/{productId}")
    public ResponseEntity<Page<Review>> getByProduct(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(
                reviewRepository.findByProductIdAndApprovedTrue(
                        productId, PageRequest.of(page, size, Sort.by("createdAt").descending())));
    }

    @GetMapping("/product/{productId}/stats")
    public ResponseEntity<Map<String, Object>> getStats(@PathVariable Long productId) {
        Double avg = reviewRepository.getAverageRating(productId);
        long count = reviewRepository.countByProductIdAndApprovedTrue(productId);
        return ResponseEntity.ok(Map.of(
                "averageRating", avg != null ? avg : 0.0,
                "totalReviews", count));
    }

    @PostMapping
    public ResponseEntity<Review> create(
            @RequestHeader("X-User-Id") String userId,
            @RequestBody Review review) {
        review.setUserId(userId);
        review.setApproved(false);
        return ResponseEntity.status(HttpStatus.CREATED).body(reviewRepository.save(review));
    }
}
