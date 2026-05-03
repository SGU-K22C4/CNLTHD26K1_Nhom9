package com.fashion.promotionservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class EarnReviewPointsRequest {

    private String userId;

    @NotBlank
    private String reviewId;

    private String description;
}
