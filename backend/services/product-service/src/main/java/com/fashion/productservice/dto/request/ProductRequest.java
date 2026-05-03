package com.fashion.productservice.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.List;

@Data
public class ProductRequest {

    @NotBlank(message = "Product name is required")
    private String name;

    private String description;
    private boolean visible = true;

    @NotNull(message = "Category ID is required")
    private String categoryId;

    private List<VariantRequest> variants;
}
