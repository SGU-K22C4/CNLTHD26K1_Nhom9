package com.fashion.promotionservice.repository;

import com.fashion.promotionservice.entity.UserLoyalty;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserLoyaltyRepository extends JpaRepository<UserLoyalty, String> {
    Optional<UserLoyalty> findByUserId(String userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select ul from UserLoyalty ul where ul.userId = :userId")
    Optional<UserLoyalty> findByUserIdForUpdate(@Param("userId") String userId);
}
