package com.fashion.productservice.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ImageResponse {
    private String id;
    private String imageUrl;
    private boolean primary;
    private int sortOrder;
}
