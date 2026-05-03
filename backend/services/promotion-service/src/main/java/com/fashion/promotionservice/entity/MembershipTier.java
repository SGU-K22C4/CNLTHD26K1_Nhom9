package com.fashion.promotionservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "membership_tiers")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MembershipTier {

    @Id
    @Column(name = "tier_id", length = 36)
    private String tierId;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(name = "min_spending", nullable = false, precision = 15, scale = 2)
    private BigDecimal minSpending;

    @Column(name = "discount_percent", nullable = false, precision = 5, scale = 2)
    private BigDecimal discountPercent;

    @Column(name = "point_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal pointRate;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
