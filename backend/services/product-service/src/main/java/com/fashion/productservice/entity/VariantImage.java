package com.fashion.productservice.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "variant_images")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VariantImage {

    @Id
    @Column(length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private ProductVariant variant;

    @Column(name = "image_url", nullable = false, length = 500)
    private String imageUrl;

    @Column(name = "is_primary")
    @Builder.Default
    private boolean primary = false;

    @Column(name = "sort_order")
    @Builder.Default
    private int sortOrder = 0;
}
