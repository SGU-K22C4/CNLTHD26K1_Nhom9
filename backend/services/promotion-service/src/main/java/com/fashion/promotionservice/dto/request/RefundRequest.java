package com.fashion.promotionservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RefundRequest {

    private String userId;

    @NotBlank
    private String refId;

    private String description;
}
