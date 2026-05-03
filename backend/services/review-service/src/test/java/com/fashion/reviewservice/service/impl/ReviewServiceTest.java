package com.fashion.reviewservice.service;

import com.fashion.reviewservice.client.LoyaltyServiceClient;
import com.fashion.reviewservice.client.OrderServiceClient;
import com.fashion.reviewservice.client.dto.OrderSummary;
import com.fashion.reviewservice.dto.request.CreateReviewRequest;
import com.fashion.reviewservice.dto.response.ReviewResponse;
import com.fashion.reviewservice.dto.response.ReviewStatsResponse;
import com.fashion.reviewservice.entity.Review;
import com.fashion.reviewservice.repository.ReviewRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock private ReviewRepository reviewRepository;
    @Mock private OrderServiceClient orderServiceClient;
    @Mock private LoyaltyServiceClient loyaltyServiceClient;

    @InjectMocks
    private ReviewService reviewService;

    @Test
    @DisplayName("Tạo Review - Thành công và tặng điểm thưởng")
    void createReview_Success() {
        // Arrange
        String userId = "user-123";
        CreateReviewRequest request = new CreateReviewRequest("1001", "PROD-A", 5, "Good", "Love it", List.of("img1.jpg"));

        OrderSummary.OrderItemSummary item = new OrderSummary.OrderItemSummary();
        item.setProductId("PROD-A");
        OrderSummary order = new OrderSummary();
        order.setStatus("DELIVERED");
        order.setItems(List.of(item));

        when(orderServiceClient.findOrderForUser("1001", userId)).thenReturn(Optional.of(order));
        when(reviewRepository.existsByUserIdAndOrderIdAndProductId(any(), any(), any())).thenReturn(false);
        when(reviewRepository.save(any(Review.class))).thenAnswer(i -> {
            Review r = i.getArgument(0);
            r.setId("mongo-id");
            return r;
        });

        // Act
        ReviewResponse response = reviewService.create(userId, request);

        // Assert
        assertNotNull(response);
        assertEquals(5, response.getStar());
        verify(loyaltyServiceClient).earnReviewPoints(eq(userId), anyString());
        verify(reviewRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("Tạo Review - Thất bại do đơn hàng chưa giao (Not DELIVERED)")
    void createReview_Fail_OrderNotDelivered() {
        // Arrange
        String userId = "user-123";
        CreateReviewRequest request = new CreateReviewRequest("1001", "PROD-A", 5, "Title", "Content", null);
        
        OrderSummary order = new OrderSummary();
        order.setStatus("SHIPPING"); // Chưa hoàn thành

        when(orderServiceClient.findOrderForUser("1001", userId)).thenReturn(Optional.of(order));

        // Act & Assert
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, 
                () -> reviewService.create(userId, request));
        assertTrue(ex.getMessage().contains("DELIVERED"));
    }

    @Test
    @DisplayName("Tạo Review - Thất bại do sản phẩm không có trong đơn hàng")
    void createReview_Fail_ProductNotInOrder() {
        // Arrange
        String userId = "user-123";
        CreateReviewRequest request = new CreateReviewRequest("1001", "PROD-B", 5, "Title", "Content", null);
        
        OrderSummary.OrderItemSummary item = new OrderSummary.OrderItemSummary();
        item.setProductId("PROD-A"); // Đơn hàng chỉ có PROD-A
        OrderSummary order = new OrderSummary();
        order.setStatus("DELIVERED");
        order.setItems(List.of(item));

        when(orderServiceClient.findOrderForUser("1001", userId)).thenReturn(Optional.of(order));

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> reviewService.create(userId, request));
    }

    @Test
    @DisplayName("Tính toán thống kê - Kiểm tra trung bình sao và phân phối")
    void getStats_ShouldCalculateCorrectly() {
        // Arrange
        String productId = "PROD-1";
        List<Review> reviews = List.of(
            Review.builder().star(5).build(),
            Review.builder().star(5).build(),
            Review.builder().star(2).build()
        );
        when(reviewRepository.findByProductIdAndVisibleTrue(productId)).thenReturn(reviews);

        // Act
        ReviewStatsResponse stats = reviewService.getStats(productId);

        // Assert
        assertEquals(3, stats.getTotalReviews());
        assertEquals(4.0, stats.getAverageRating()); // (5+5+2)/3 = 4.0
        assertEquals(2, stats.getStarDistribution().get(5));
        assertEquals(1, stats.getStarDistribution().get(2));
        assertEquals(0, stats.getStarDistribution().get(1));
    }

    @Test
    @DisplayName("Rollback Review - Khi tặng điểm thưởng bị lỗi")
    void createReview_Rollback_WhenLoyaltyFails() {
        // Arrange
        String userId = "user-123";
        CreateReviewRequest request = new CreateReviewRequest("1001", "PROD-A", 5, "T", "C", null);
        
        OrderSummary.OrderItemSummary item = new OrderSummary.OrderItemSummary();
        item.setProductId("PROD-A");
        OrderSummary order = new OrderSummary();
        order.setStatus("DELIVERED");
        order.setItems(List.of(item));

        Review savedReview = Review.builder().id("mongo-id").reviewId("rev-123").build();

        when(orderServiceClient.findOrderForUser(any(), any())).thenReturn(Optional.of(order));
        when(reviewRepository.save(any())).thenReturn(savedReview);
        
        // Giả lập lỗi từ Promotion Service
        doThrow(new RuntimeException("Loyalty Service Down"))
            .when(loyaltyServiceClient).earnReviewPoints(any(), any());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> reviewService.create(userId, request));
        
        // Quan trọng: Kiểm tra xem có gọi lệnh xóa để rollback không
        verify(reviewRepository).deleteById("mongo-id");
    }
}