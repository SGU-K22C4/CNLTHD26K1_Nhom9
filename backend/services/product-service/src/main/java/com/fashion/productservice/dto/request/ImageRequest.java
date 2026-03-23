package com.fashion.productservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ImageRequest {
    @NotBlank
    private String imageUrl;
    private boolean primary;
    private int sortOrder;
}
