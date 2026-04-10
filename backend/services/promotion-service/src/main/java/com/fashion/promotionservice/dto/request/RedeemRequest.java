package com.fashion.promotionservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.EqualsAndHashCode;
import lombok.Data;

@Data
@EqualsAndHashCode(callSuper = true)
public class RedeemRequest extends RedeemPreviewRequest {

    @NotBlank
    private String refId;

    private String description;
}
