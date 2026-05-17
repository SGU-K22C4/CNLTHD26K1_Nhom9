package com.fashion.orderservice.repository;

import com.fashion.orderservice.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    Page<Order> findByUserId(String userId, Pageable pageable);

    Optional<Order> findByOrderNumber(String orderNumber);

    Optional<Order> findByOrderNumberAndUserId(String orderNumber, String userId);

    Optional<Order> findByIdAndUserId(Long id, String userId);

    @Query("""
            SELECT o FROM Order o
            WHERE (:keyword IS NULL
                OR LOWER(o.orderNumber) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(COALESCE(o.recipientName, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(COALESCE(o.recipientPhone, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(COALESCE(o.userId, '')) LIKE LOWER(CONCAT('%', :keyword, '%')))
              AND (:status IS NULL OR o.status = :status)
            """)
    Page<Order> searchAdmin(
            @Param("keyword") String keyword,
            @Param("status") Order.OrderStatus status,
            Pageable pageable
    );
}
