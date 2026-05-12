package com.fashion.orderservice.repository;

import com.fashion.orderservice.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    Page<Order> findByUserId(String userId, Pageable pageable);
    Optional<Order> findByOrderNumber(String orderNumber);
    Optional<Order> findByOrderNumberAndUserId(String orderNumber, String userId);
    Optional<Order> findByIdAndUserId(Long id, String userId);

    /**
     * Finds COD orders that are still PENDING but have their inventory reserved
     * and were created before the given cutoff time (grace period expired).
     * Used by the auto-confirm scheduled job.
     */
    @Query("SELECT o FROM Order o WHERE o.status = 'PENDING' " +
           "AND o.paymentMethod = 'COD' " +
           "AND o.inventoryReserved = true " +
           "AND o.createdAt <= :cutoff")
    List<Order> findCodOrdersReadyForAutoConfirm(@Param("cutoff") LocalDateTime cutoff);
}
