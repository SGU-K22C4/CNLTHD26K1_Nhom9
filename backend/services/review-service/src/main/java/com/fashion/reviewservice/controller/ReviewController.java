package com.fashion.reviewservice.controller;

import com.fashion.reviewservice.dto.request.CreateReviewRequest;
import com.fashion.reviewservice.dto.response.ReviewResponse;
import com.fashion.reviewservice.dto.response.ReviewStatsResponse;
import com.fashion.reviewservice.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @GetMapping("/product/{productId}")
    public ResponseEntity<Page<ReviewResponse>> getByProduct(
            @PathVariable String productId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer star) {
        return ResponseEntity.ok(reviewService.getByProduct(productId, page, size, star));
    }

    @GetMapping("/product/{productId}/stats")
    public ResponseEntity<ReviewStatsResponse> getStats(@PathVariable String productId) {
        return ResponseEntity.ok(reviewService.getStats(productId));
    }

    @GetMapping("/mine")
    public ResponseEntity<List<ReviewResponse>> getMine(@RequestHeader("X-User-Id") String userId) {
        return ResponseEntity.ok(reviewService.getMine(userId));
    }

    @PostMapping
    public ResponseEntity<ReviewResponse> create(
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody CreateReviewRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(reviewService.create(userId, request));
    }
}
