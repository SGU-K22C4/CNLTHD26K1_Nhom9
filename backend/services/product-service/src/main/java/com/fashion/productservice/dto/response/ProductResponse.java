package com.fashion.productservice.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class ProductResponse {
    private Long id;
    private String name;
    private String slug;
    private String description;
    private String materials;
    private String careInstructions;
    private BigDecimal price;
    private BigDecimal salePrice;
    private String categoryName;
    private String categorySlug;
    private String status;
    private boolean featured;
    private boolean newArrival;
    private List<ImageDto> images;
    private List<VariantDto> variants;
    private LocalDateTime createdAt;

    @Data
    @Builder
    public static class ImageDto {
        private Long id;
        private String url;
        private String altText;
        private boolean primary;
    }

    @Data
    @Builder
    public static class VariantDto {
        private Long id;
        private String color;
        private String colorHex;
        private String size;
        private int stock;
        private String sku;
    }
}
