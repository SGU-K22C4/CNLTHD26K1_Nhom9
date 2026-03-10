package com.fashion.promotionservice.repository;

import com.fashion.promotionservice.entity.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface CouponRepository extends JpaRepository<Coupon, Long> {
    Optional<Coupon> findByCodeAndActiveTrue(String code);

    Optional<Coupon> findByCodeAndActiveTrueAndStartDateBeforeAndEndDateAfter(
            String code, LocalDateTime now1, LocalDateTime now2);
}
