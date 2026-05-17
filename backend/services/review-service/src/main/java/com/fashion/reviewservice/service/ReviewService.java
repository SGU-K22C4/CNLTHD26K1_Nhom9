package com.fashion.reviewservice.service;

import com.fashion.reviewservice.client.LoyaltyServiceClient;
import com.fashion.reviewservice.client.OrderServiceClient;
import com.fashion.reviewservice.client.dto.OrderSummary;
import com.fashion.reviewservice.dto.request.CreateReviewRequest;
import com.fashion.reviewservice.dto.response.ReviewResponse;
import com.fashion.reviewservice.dto.response.ReviewStatsResponse;
import com.fashion.reviewservice.entity.Review;
import com.fashion.reviewservice.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final OrderServiceClient orderServiceClient;
    private final LoyaltyServiceClient loyaltyServiceClient;

    public Page<ReviewResponse> getByProduct(String productId, int page, int size, Integer star) {
        String normalizedProductId = requireNonBlank(productId, "productId không được để trống");
        int normalizedPage = Math.max(0, page);
        int normalizedSize = Math.min(Math.max(size, 1), 100);

        if (star != null && (star < 1 || star > 5)) {
            throw new IllegalArgumentException("Bộ lọc số sao chỉ nhận giá trị từ 1 đến 5");
        }

        Pageable pageable = PageRequest.of(
                normalizedPage,
                normalizedSize,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Page<Review> result = star == null
                ? reviewRepository.findByProductIdAndVisibleTrue(normalizedProductId, pageable)
                : reviewRepository.findByProductIdAndVisibleTrueAndStar(normalizedProductId, star, pageable);

        return result.map(ReviewResponse::from);
    }

    public ReviewStatsResponse getStats(String productId) {
        String normalizedProductId = requireNonBlank(productId, "productId không được để trống");
        List<Review> reviews = reviewRepository.findByProductIdAndVisibleTrue(normalizedProductId);

        long totalReviews = reviews.size();
        double averageRating = totalReviews == 0
                ? 0.0
                : reviews.stream().mapToInt(Review::getStar).average().orElse(0.0);

        Map<Integer, Long> starDistribution = new LinkedHashMap<>();
        for (int star = 1; star <= 5; star++) {
            starDistribution.put(star, 0L);
        }
        reviews.forEach(review -> starDistribution.computeIfPresent(review.getStar(), (k, value) -> value + 1));

        return ReviewStatsResponse.builder()
                .averageRating(averageRating)
                .totalReviews(totalReviews)
                .starDistribution(starDistribution)
                .build();
    }

    public List<ReviewResponse> getMine(String userId) {
        String normalizedUserId = requireNonBlank(userId, "Thiếu thông tin người dùng");
        return reviewRepository.findByUserIdOrderByCreatedAtDesc(normalizedUserId)
                .stream()
                .map(ReviewResponse::from)
                .toList();
    }

    public ReviewResponse create(String userId, CreateReviewRequest request) {
        String normalizedUserId = requireNonBlank(userId, "Thiếu thông tin người dùng");
        String normalizedOrderId = requireNonBlank(request.getOrderId(), "orderId không được để trống");
        String normalizedProductId = requireNonBlank(request.getProductId(), "productId không được để trống");

        int star = request.getStar();
        String content = requireNonBlank(request.getContent(), "Nội dung đánh giá không được để trống");
        String title = normalizeTitle(request.getTitle(), star);
        List<String> images = normalizeImages(request.getImages());

        OrderSummary order = orderServiceClient.findOrderForUser(normalizedOrderId, normalizedUserId)
                .orElseThrow(() -> new IllegalArgumentException("Đơn hàng không tồn tại hoặc không thuộc tài khoản của bạn"));

        validateOrderEligibility(order, normalizedProductId);

        if (reviewRepository.existsByUserIdAndOrderIdAndProductId(normalizedUserId, normalizedOrderId, normalizedProductId)) {
            throw new IllegalArgumentException("Bạn đã đánh giá sản phẩm này trong đơn hàng đã chọn");
        }

        // Use Instant so MongoDB BSON Date maps directly without reflective access
        // into java.time.LocalDateTime on Java 21+.
        Instant now = Instant.now();
        Review review = Review.builder()
                .reviewId(UUID.randomUUID().toString())
                .userId(normalizedUserId)
                .productId(normalizedProductId)
                .orderId(normalizedOrderId)
                .star(star)
                .title(title)
                .content(content)
                .images(images)
                .visible(true)
                .createdAt(now)
                .updatedAt(now)
                .build();

        Review savedReview;
        try {
            savedReview = reviewRepository.save(review);
        } catch (DuplicateKeyException ex) {
            throw new IllegalArgumentException("Bạn đã đánh giá sản phẩm này trong đơn hàng đã chọn");
        }

        try {
            loyaltyServiceClient.earnReviewPoints(normalizedUserId, savedReview.getReviewId());
        } catch (RuntimeException ex) {
            reviewRepository.deleteById(savedReview.getId());
            throw ex;
        }

        return ReviewResponse.from(savedReview);
    }

    private void validateOrderEligibility(OrderSummary order, String productId) {
        if (order.getStatus() == null || !"DELIVERED".equalsIgnoreCase(order.getStatus())) {
            throw new IllegalArgumentException("Chỉ đơn hàng ở trạng thái DELIVERED mới được đánh giá");
        }

        boolean productInOrder = order.getItems() != null
                && order.getItems().stream()
                .map(OrderSummary.OrderItemSummary::getProductId)
                .filter(Objects::nonNull)
                .map(String::trim)
                .anyMatch(productId::equals);

        if (!productInOrder) {
            throw new IllegalArgumentException("Sản phẩm không thuộc đơn hàng này");
        }
    }

    private String requireNonBlank(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    private String normalizeTitle(String title, int star) {
        if (title == null || title.isBlank()) {
            return "Đánh giá " + star + " sao";
        }
        String normalized = title.trim();
        if (normalized.length() > 120) {
            throw new IllegalArgumentException("Tiêu đề tối đa 120 ký tự");
        }
        return normalized;
    }

    private List<String> normalizeImages(List<String> images) {
        if (images == null || images.isEmpty()) {
            return List.of();
        }

        List<String> normalized = images.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .toList();

        if (normalized.size() > 4) {
            throw new IllegalArgumentException("Tối đa 4 hình ảnh cho mỗi đánh giá");
        }

        return normalized;
    }
}
