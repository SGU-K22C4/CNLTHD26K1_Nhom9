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

    @Query(
            value = "SELECT DISTINCT p.* FROM products p\n" +
                    "WHERE p.is_visible = true\n" +
                    "    AND (:categoryId IS NULL OR p.category_id = :categoryId)\n" +
                    "    AND (:search IS NULL OR p.name COLLATE utf8mb4_0900_ai_ci LIKE CONCAT('%', :search, '%'))\n" +
                    "    AND (:minPrice IS NULL OR EXISTS (\n" +
                    "                SELECT v1.id FROM product_variants v1\n" +
                    "                WHERE v1.product_id = p.id AND v1.price >= :minPrice\n" +
                    "    ))\n" +
                    "    AND (:maxPrice IS NULL OR EXISTS (\n" +
                    "                SELECT v2.id FROM product_variants v2\n" +
                    "                WHERE v2.product_id = p.id AND v2.price <= :maxPrice\n" +
                    "    ))",
            countQuery = "SELECT COUNT(DISTINCT p.id) FROM products p\n" +
                    "WHERE p.is_visible = true\n" +
                    "    AND (:categoryId IS NULL OR p.category_id = :categoryId)\n" +
                    "    AND (:search IS NULL OR p.name COLLATE utf8mb4_0900_ai_ci LIKE CONCAT('%', :search, '%'))\n" +
                    "    AND (:minPrice IS NULL OR EXISTS (\n" +
                    "                SELECT v1.id FROM product_variants v1\n" +
                    "                WHERE v1.product_id = p.id AND v1.price >= :minPrice\n" +
                    "    ))\n" +
                    "    AND (:maxPrice IS NULL OR EXISTS (\n" +
                    "                SELECT v2.id FROM product_variants v2\n" +
                    "                WHERE v2.product_id = p.id AND v2.price <= :maxPrice\n" +
                    "    ))",
            nativeQuery = true)
    Page<Product> search(
            @Param("categoryId") String categoryId,
            @Param("search") String search,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            Pageable pageable
    );

    @Query(
            "SELECT DISTINCT p FROM Product p\n" +
                    "WHERE (:categoryId IS NULL OR p.category.id = :categoryId)\n" +
                    "    AND (:search IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')))\n" +
                    "    AND (:minPrice IS NULL OR EXISTS (\n" +
                    "                SELECT v1.id FROM ProductVariant v1\n" +
                    "                WHERE v1.product = p AND v1.price >= :minPrice\n" +
                    "    ))\n" +
                    "    AND (:maxPrice IS NULL OR EXISTS (\n" +
                    "                SELECT v2.id FROM ProductVariant v2\n" +
                    "                WHERE v2.product = p AND v2.price <= :maxPrice\n" +
                    "    ))\n" +
                    "    AND (\n" +
                    "        :status IS NULL\n" +
                    "        OR (:status = 'Hidden' AND p.visible = false)\n" +
                    "        OR (:status = 'Active' AND p.visible = true AND EXISTS (\n" +
                    "                SELECT s1.id FROM VariantSize s1\n" +
                    "                JOIN s1.variant v3\n" +
                    "                WHERE v3.product = p AND s1.quantity > 0\n" +
                    "        ))\n" +
                    "        OR (:status = 'Out of stock' AND p.visible = true AND NOT EXISTS (\n" +
                    "                SELECT s2.id FROM VariantSize s2\n" +
                    "                JOIN s2.variant v4\n" +
                    "                WHERE v4.product = p AND s2.quantity > 0\n" +
                    "        ))\n" +
                    "    )"
    )
    Page<Product> searchAdmin(
            @Param("categoryId") String categoryId,
            @Param("search") String search,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            @Param("status") String status,
            Pageable pageable
    );
}
