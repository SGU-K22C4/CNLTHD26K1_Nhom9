package com.fashion.reviewservice.repository;

import com.fashion.reviewservice.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewRepository extends MongoRepository<Review, String> {
    Page<Review> findByProductIdAndVisibleTrue(String productId, Pageable pageable);

    Page<Review> findByProductIdAndVisibleTrueAndStar(String productId, int star, Pageable pageable);

    List<Review> findByProductIdAndVisibleTrue(String productId);

    List<Review> findByUserIdOrderByCreatedAtDesc(String userId);

    boolean existsByUserIdAndOrderIdAndProductId(String userId, String orderId, String productId);
}
