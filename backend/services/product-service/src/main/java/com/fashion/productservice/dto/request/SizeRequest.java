package com.fashion.productservice.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SizeRequest {
    @NotBlank
    @Size(max = 10)
    private String sizeName;

    @Min(0)
    private int quantity;

    @Size(max = 20)
    private String status;
}
