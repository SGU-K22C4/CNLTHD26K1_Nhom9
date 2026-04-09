package com.fashion.promotionservice.dto.response;

import com.fashion.promotionservice.entity.PointTransactionType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class PointTransactionResponse {

    private String transactionId;

    private PointTransactionType type;

    private Integer points;

    private String refId;

    private String description;

    private LocalDateTime createdAt;
}
