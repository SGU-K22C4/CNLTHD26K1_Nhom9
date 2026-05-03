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

    @Query("""
                SELECT DISTINCT p FROM Product p
                WHERE p.visible = true
                    AND (:categoryId IS NULL OR p.category.id = :categoryId)
          AND (:search IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')))
                    AND (:minPrice IS NULL OR EXISTS (
                                SELECT v1.id FROM ProductVariant v1
                                WHERE v1.product = p AND v1.price >= :minPrice
                    ))
                    AND (:maxPrice IS NULL OR EXISTS (
                                SELECT v2.id FROM ProductVariant v2
                                WHERE v2.product = p AND v2.price <= :maxPrice
                    ))
    """)
    Page<Product> search(
                        @Param("categoryId") String categoryId,
            @Param("search") String search,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            Pageable pageable
    );
}
