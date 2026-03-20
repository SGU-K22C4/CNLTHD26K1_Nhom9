package com.fashion.productservice.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SizeResponse {
    private String id;
    private String sizeName;
    private int quantity;
    private String status;
}
