package com.fashion.productservice.repository;

import com.fashion.productservice.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;

@Repository
public interface ProductRepository extends JpaRepository<Product, String> {

        Page<Product> findByVisibleTrue(Pageable pageable);

        Page<Product> findByVisibleTrueOrderByCreatedAtDesc(Pageable pageable);

        Page<Product> findByCategoryIdAndVisibleTrue(String categoryId, Pageable pageable);

<<<<<<< HEAD
    @Query("""
                SELECT DISTINCT p FROM Product p
                WHERE p.visible = true
                    AND (:categoryId IS NULL OR p.category.id = :categoryId)
                    AND (:search IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')))
=======
    @Query(value = """
                SELECT DISTINCT p.* FROM products p
                WHERE p.is_visible = true
                    AND (:categoryId IS NULL OR p.category_id = :categoryId)
                    AND (:search IS NULL OR p.name COLLATE utf8mb4_0900_ai_ci LIKE CONCAT('%', :search, '%'))
>>>>>>> 2dd54ea (fix: update product UI and service logic)
                    AND (:minPrice IS NULL OR EXISTS (
                                SELECT v1.id FROM product_variants v1
                                WHERE v1.product_id = p.id AND v1.price >= :minPrice
                    ))
                    AND (:maxPrice IS NULL OR EXISTS (
                                SELECT v2.id FROM product_variants v2
                                WHERE v2.product_id = p.id AND v2.price <= :maxPrice
                    ))
    """,
    countQuery = """
                SELECT COUNT(DISTINCT p.id) FROM products p
                WHERE p.is_visible = true
                    AND (:categoryId IS NULL OR p.category_id = :categoryId)
                    AND (:search IS NULL OR p.name COLLATE utf8mb4_0900_ai_ci LIKE CONCAT('%', :search, '%'))
                    AND (:minPrice IS NULL OR EXISTS (
                                SELECT v1.id FROM product_variants v1
                                WHERE v1.product_id = p.id AND v1.price >= :minPrice
                    ))
                    AND (:maxPrice IS NULL OR EXISTS (
                                SELECT v2.id FROM product_variants v2
                                WHERE v2.product_id = p.id AND v2.price <= :maxPrice
                    ))
    """,
    nativeQuery = true)
    Page<Product> search(
            @Param("categoryId") String categoryId,
            @Param("search") String search,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            Pageable pageable
    );

    @Query("""
                SELECT DISTINCT p FROM Product p
                WHERE (:categoryId IS NULL OR p.category.id = :categoryId)
                    AND (:search IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')))
                    AND (:minPrice IS NULL OR EXISTS (
                                SELECT v1.id FROM ProductVariant v1
                                WHERE v1.product = p AND v1.price >= :minPrice
                    ))
                    AND (:maxPrice IS NULL OR EXISTS (
                                SELECT v2.id FROM ProductVariant v2
                                WHERE v2.product = p AND v2.price <= :maxPrice
                    ))
                    AND (
                        :status IS NULL
                        OR (:status = 'Hidden' AND p.visible = false)
                        OR (:status = 'Active' AND p.visible = true AND EXISTS (
                                SELECT s1.id FROM VariantSize s1
                                JOIN s1.variant v3
                                WHERE v3.product = p AND s1.quantity > 0
                        ))
                        OR (:status = 'Out of stock' AND p.visible = true AND NOT EXISTS (
                                SELECT s2.id FROM VariantSize s2
                                JOIN s2.variant v4
                                WHERE v4.product = p AND s2.quantity > 0
                        ))
                    )
    """)
    Page<Product> searchAdmin(
            @Param("categoryId") String categoryId,
            @Param("search") String search,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            @Param("status") String status,
            Pageable pageable
    );
}
