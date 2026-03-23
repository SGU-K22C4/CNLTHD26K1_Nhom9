package com.fashion.productservice.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class VariantRequest {
    @NotBlank
    private String colorName;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = false)
    private BigDecimal price;

    @Size(max = 500)
    private String compositionDetail;

    @Size(max = 500)
    private String productUrl;

    private List<ImageRequest> images;
    private List<SizeRequest> sizes;
}
