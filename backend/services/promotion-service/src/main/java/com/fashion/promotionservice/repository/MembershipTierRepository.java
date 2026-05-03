package com.fashion.promotionservice.repository;

import com.fashion.promotionservice.entity.MembershipTier;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MembershipTierRepository extends JpaRepository<MembershipTier, String> {
    List<MembershipTier> findAllByOrderByMinSpendingDesc();

    Optional<MembershipTier> findFirstByOrderByMinSpendingAsc();
}
