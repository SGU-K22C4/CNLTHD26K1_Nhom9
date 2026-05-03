package com.fashion.reviewservice.service;

import com.fashion.reviewservice.client.LoyaltyServiceClient;
import com.fashion.reviewservice.client.OrderServiceClient;
import com.fashion.reviewservice.client.dto.OrderSummary;
import com.fashion.reviewservice.dto.request.CreateReviewRequest;
import com.fashion.reviewservice.dto.response.ReviewResponse;
import com.fashion.reviewservice.dto.response.ReviewStatsResponse;
import com.fashion.reviewservice.entity.Review;
import com.fashion.reviewservice.repository.ReviewRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock
    private ReviewRepository reviewRepository;

    private StubOrderServiceClient orderServiceClient;
    private StubLoyaltyServiceClient loyaltyServiceClient;
    private ReviewService reviewService;

    @BeforeEach
    void setUp() {
        orderServiceClient = new StubOrderServiceClient();
        loyaltyServiceClient = new StubLoyaltyServiceClient();
        reviewService = new ReviewService(reviewRepository, orderServiceClient, loyaltyServiceClient);
    }

    @Test
    void should_CreateReviewAndAwardPoints_When_OrderIsDeliveredAndProductBelongsToOrder() {
        String userId = "user-123";
        CreateReviewRequest request = new CreateReviewRequest("1001", "PROD-A", 5, "Good", "Love it", List.of("img1.jpg"));

        orderServiceClient.nextOrder = Optional.of(deliveredOrderWithProduct("PROD-A"));
        when(reviewRepository.existsByUserIdAndOrderIdAndProductId(userId, "1001", "PROD-A")).thenReturn(false);
        when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> {
            Review review = invocation.getArgument(0);
            review.setId("mongo-id");
            return review;
        });

        ReviewResponse response = reviewService.create(userId, request);

        assertEquals(5, response.getStar());
        assertEquals("PROD-A", response.getProductId());
        assertEquals(userId, loyaltyServiceClient.lastUserId);
        assertEquals(response.getReviewId(), loyaltyServiceClient.lastReviewId);
        verify(reviewRepository).save(any(Review.class));
    }

    @Test
    void should_ThrowIllegalArgumentException_When_OrderIsNotDelivered() {
        String userId = "user-123";
        CreateReviewRequest request = new CreateReviewRequest("1001", "PROD-A", 5, "Title", "Content", null);

        OrderSummary order = new OrderSummary();
        order.setStatus("SHIPPING");
        orderServiceClient.nextOrder = Optional.of(order);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> reviewService.create(userId, request));

        assertTrue(exception.getMessage().contains("DELIVERED"));
    }

    @Test
    void should_ThrowIllegalArgumentException_When_ProductDoesNotBelongToOrder() {
        String userId = "user-123";
        CreateReviewRequest request = new CreateReviewRequest("1001", "PROD-B", 5, "Title", "Content", null);
        orderServiceClient.nextOrder = Optional.of(deliveredOrderWithProduct("PROD-A"));

        assertThrows(IllegalArgumentException.class, () -> reviewService.create(userId, request));
    }

    @Test
    void should_CalculateStats_When_ProductHasVisibleReviews() {
        String productId = "PROD-1";
        List<Review> reviews = List.of(
                Review.builder().star(5).build(),
                Review.builder().star(5).build(),
                Review.builder().star(2).build()
        );
        when(reviewRepository.findByProductIdAndVisibleTrue(productId)).thenReturn(reviews);

        ReviewStatsResponse stats = reviewService.getStats(productId);

        assertEquals(3L, stats.getTotalReviews());
        assertEquals(4.0, stats.getAverageRating());
        assertEquals(2L, stats.getStarDistribution().get(5));
        assertEquals(1L, stats.getStarDistribution().get(2));
        assertEquals(0L, stats.getStarDistribution().get(1));
    }

    @Test
    void should_RollbackSavedReview_When_AwardingPointsFails() {
        String userId = "user-123";
        CreateReviewRequest request = new CreateReviewRequest("1001", "PROD-A", 5, "T", "Content", null);
        Review savedReview = Review.builder().id("mongo-id").reviewId("rev-123").build();

        orderServiceClient.nextOrder = Optional.of(deliveredOrderWithProduct("PROD-A"));
        when(reviewRepository.existsByUserIdAndOrderIdAndProductId(userId, "1001", "PROD-A")).thenReturn(false);
        when(reviewRepository.save(any(Review.class))).thenReturn(savedReview);
        loyaltyServiceClient.exceptionToThrow = new RuntimeException("Loyalty Service Down");

        assertThrows(RuntimeException.class, () -> reviewService.create(userId, request));

        verify(reviewRepository).deleteById("mongo-id");
    }

    private OrderSummary deliveredOrderWithProduct(String productId) {
        OrderSummary.OrderItemSummary item = new OrderSummary.OrderItemSummary();
        item.setProductId(productId);

        OrderSummary order = new OrderSummary();
        order.setStatus("DELIVERED");
        order.setItems(List.of(item));
        return order;
    }

    private static class StubOrderServiceClient extends OrderServiceClient {
        private Optional<OrderSummary> nextOrder = Optional.empty();

        StubOrderServiceClient() {
            super(null);
        }

        @Override
        public Optional<OrderSummary> findOrderForUser(String orderId, String userId) {
            return nextOrder;
        }
    }

    private static class StubLoyaltyServiceClient extends LoyaltyServiceClient {
        private String lastUserId;
        private String lastReviewId;
        private RuntimeException exceptionToThrow;

        StubLoyaltyServiceClient() {
            super(null);
        }

        @Override
        public void earnReviewPoints(String userId, String reviewId) {
            lastUserId = userId;
            lastReviewId = reviewId;
            if (exceptionToThrow != null) {
                throw exceptionToThrow;
            }
        }
    }
}
