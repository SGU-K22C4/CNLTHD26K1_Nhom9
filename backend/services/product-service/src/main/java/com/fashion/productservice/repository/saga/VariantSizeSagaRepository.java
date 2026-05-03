package com.fashion.productservice.repository.saga;

import com.fashion.productservice.entity.VariantSize;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface VariantSizeSagaRepository extends JpaRepository<VariantSize, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT vs
            FROM VariantSize vs
            JOIN vs.variant v
            JOIN v.product p
            WHERE p.id = :productId
              AND LOWER(v.colorName) = LOWER(:color)
              AND LOWER(vs.sizeName) = LOWER(:size)
            ORDER BY vs.id
            """)
    List<VariantSize> findForUpdate(@Param("productId") String productId,
                                    @Param("color") String color,
                                    @Param("size") String size);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT vs
            FROM VariantSize vs
            JOIN vs.variant v
            JOIN v.product p
            WHERE p.id = :productId
              AND LOWER(vs.sizeName) = LOWER(:size)
            ORDER BY vs.id
            """)
    List<VariantSize> findForUpdateWithoutColor(@Param("productId") String productId,
                                                @Param("size") String size);
}
