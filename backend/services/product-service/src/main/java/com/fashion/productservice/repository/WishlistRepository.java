package com.fashion.productservice.repository;

import com.fashion.productservice.entity.Wishlist;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WishlistRepository extends JpaRepository<Wishlist, String> {

    Page<Wishlist> findByUserId(String userId, Pageable pageable);

    Optional<Wishlist> findByUserIdAndProduct_Id(String userId, String productId);

    boolean existsByUserIdAndProduct_Id(String userId, String productId);

    void deleteByUserIdAndProduct_Id(String userId, String productId);

    @Query("SELECT w.product.id FROM Wishlist w WHERE w.userId = :userId")
    List<String> findProductIdsByUserId(@Param("userId") String userId);
}
