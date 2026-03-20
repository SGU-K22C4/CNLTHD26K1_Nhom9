package com.fashion.productservice.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "variant_sizes")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VariantSize {

    @Id
    @Column(length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private ProductVariant variant;

    @Column(name = "size_name", nullable = false, length = 10)
    private String sizeName;

    @Builder.Default
    private int quantity = 0;

    @Column(length = 20)
    @Builder.Default
    private String status = "Con hang";
}
