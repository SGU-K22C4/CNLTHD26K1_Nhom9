package com.fashion.productservice.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class VariantResponse {
    private String id;
    private String colorName;
    private BigDecimal price;
    private String compositionDetail;
    private String productUrl;
    private List<ImageResponse> images;
    private List<SizeResponse> sizes;
}
