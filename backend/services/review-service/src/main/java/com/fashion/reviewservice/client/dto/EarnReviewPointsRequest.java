package com.fashion.reviewservice.client.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EarnReviewPointsRequest {
    private String userId;
    private String reviewId;
    private String description;
}
