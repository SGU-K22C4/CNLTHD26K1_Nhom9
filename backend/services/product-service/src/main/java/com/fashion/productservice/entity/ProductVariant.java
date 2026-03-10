package com.fashion.productservice.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "product_variants")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductVariant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Product product;

    @Column(length = 50)
    private String color;

    @Column(length = 10)
    private String colorHex;

    @Column(length = 20)
    private String size;

    @Column(nullable = false)
    @Builder.Default
    private int stock = 0;

    @Column(unique = true, length = 100)
    private String sku;
}
