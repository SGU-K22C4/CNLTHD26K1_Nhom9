package com.fashion.reviewservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fashion.reviewservice.dto.request.CreateReviewRequest;
import com.fashion.reviewservice.dto.response.ReviewResponse;
import com.fashion.reviewservice.dto.response.ReviewStatsResponse;
import com.fashion.reviewservice.service.ReviewService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReviewController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(ReviewControllerIntegrationTest.StubConfig.class)
class ReviewControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void should_ReturnCreated_When_RequestIsValid() throws Exception {
        CreateReviewRequest request = new CreateReviewRequest("1001", "PROD-A", 5, "Great", "Love it", List.of("img1.jpg"));

        mockMvc.perform(post("/api/v1/reviews")
                        .header("X-User-Id", "user-123")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reviewId").value("rev-123"))
                .andExpect(jsonPath("$.productId").value("PROD-A"));
    }

    @Test
    void should_ReturnBadRequest_When_RequestIsInvalid() throws Exception {
        CreateReviewRequest request = new CreateReviewRequest("", "", 6, null, "", null);

        mockMvc.perform(post("/api/v1/reviews")
                        .header("X-User-Id", "user-123")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isBadRequest());
    }

    @TestConfiguration
    static class StubConfig {

        @Bean
        ReviewService reviewService() {
            return new StubReviewService();
        }
    }

    static class StubReviewService extends ReviewService {

        StubReviewService() {
            super(null, null, null);
        }

        @Override
        public ReviewResponse create(String userId, CreateReviewRequest request) {
            return ReviewResponse.builder()
                    .reviewId("rev-123")
                    .productId(request.getProductId())
                    .star(request.getStar())
                    .content(request.getContent())
                    .build();
        }

        @Override
        public Page<ReviewResponse> getByProduct(String productId, int page, int size, Integer star) {
            return new PageImpl<>(List.of());
        }

        @Override
        public ReviewStatsResponse getStats(String productId) {
            return ReviewStatsResponse.builder().averageRating(0.0).totalReviews(0L).build();
        }

        @Override
        public List<ReviewResponse> getMine(String userId) {
            return List.of();
        }
    }
}
