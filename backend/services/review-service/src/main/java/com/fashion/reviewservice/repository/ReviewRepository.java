package com.fashion.reviewservice.repository;

import com.fashion.reviewservice.entity.Review;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends MongoRepository<Review, String> {
    Page<Review> findByProductIdAndApprovedTrue(Long productId, Pageable pageable);

    Optional<Review> findByUserIdAndProductId(String userId, Long productId);

    List<Review> findByProductIdAndApprovedTrue(Long productId);

    long countByProductIdAndApprovedTrue(Long productId);
}
