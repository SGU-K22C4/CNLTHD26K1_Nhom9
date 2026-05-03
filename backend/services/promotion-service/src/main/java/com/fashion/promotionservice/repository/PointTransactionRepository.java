package com.fashion.promotionservice.repository;

import com.fashion.promotionservice.entity.PointTransaction;
import com.fashion.promotionservice.entity.PointTransactionType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PointTransactionRepository extends JpaRepository<PointTransaction, String> {
    Optional<PointTransaction> findByUserIdAndTypeAndRefId(String userId, PointTransactionType type, String refId);

    List<PointTransaction> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);
}
