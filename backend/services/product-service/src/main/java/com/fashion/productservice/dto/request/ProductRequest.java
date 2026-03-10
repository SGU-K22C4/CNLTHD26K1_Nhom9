package com.fashion.productservice.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class ProductRequest {

    @NotBlank(message = "Product name is required")
    private String name;

    private String description;
    private String materials;
    private String careInstructions;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Price must be positive")
    private BigDecimal price;

    private BigDecimal salePrice;

    @NotNull(message = "Category ID is required")
    private Long categoryId;

    private boolean featured;
    private boolean newArrival;
    private List<ImageRequest> images;
    private List<VariantRequest> variants;

    @Data
    public static class ImageRequest {
        @NotBlank
        private String url;
        private String altText;
        private boolean primary;
        private int sortOrder;
    }

    @Data
    public static class VariantRequest {
        private String color;
        private String colorHex;
        private String size;
        @Min(0)
        private int stock;
        private String sku;
    }
}
