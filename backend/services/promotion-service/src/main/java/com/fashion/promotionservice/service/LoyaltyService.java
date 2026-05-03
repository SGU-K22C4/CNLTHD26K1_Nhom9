package com.fashion.promotionservice.service;

import com.fashion.promotionservice.config.LoyaltyProperties;
import com.fashion.promotionservice.dto.request.EarnOrderPointsRequest;
import com.fashion.promotionservice.dto.request.EarnReviewPointsRequest;
import com.fashion.promotionservice.dto.request.RedeemPreviewRequest;
import com.fashion.promotionservice.dto.request.RedeemRequest;
import com.fashion.promotionservice.dto.request.RefundRequest;
import com.fashion.promotionservice.dto.response.LoyaltyMutationResponse;
import com.fashion.promotionservice.dto.response.LoyaltyWalletResponse;
import com.fashion.promotionservice.dto.response.PointTransactionResponse;
import com.fashion.promotionservice.dto.response.RedeemPreviewResponse;
import com.fashion.promotionservice.entity.MembershipTier;
import com.fashion.promotionservice.entity.PointTransaction;
import com.fashion.promotionservice.entity.PointTransactionType;
import com.fashion.promotionservice.entity.UserLoyalty;
import com.fashion.promotionservice.repository.MembershipTierRepository;
import com.fashion.promotionservice.repository.PointTransactionRepository;
import com.fashion.promotionservice.repository.UserLoyaltyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LoyaltyService {

    private static final int MAX_HISTORY_LIMIT = 100;

    private final LoyaltyProperties loyaltyProperties;
    private final MembershipTierRepository membershipTierRepository;
    private final UserLoyaltyRepository userLoyaltyRepository;
    private final PointTransactionRepository pointTransactionRepository;

    @Transactional(readOnly = true)
    public LoyaltyWalletResponse getWallet(String userId) {
        String normalizedUserId = requireUserId(userId);
        UserLoyalty wallet = getOrCreateWallet(normalizedUserId);
        return toWalletResponse(wallet);
    }

    @Transactional(readOnly = true)
    public List<PointTransactionResponse> getTransactions(String userId, int size) {
        String normalizedUserId = requireUserId(userId);
        int limit = Math.min(Math.max(size, 1), MAX_HISTORY_LIMIT);

        return pointTransactionRepository
                .findByUserIdOrderByCreatedAtDesc(normalizedUserId, PageRequest.of(0, limit))
                .stream()
                .map(this::toPointTransactionResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public RedeemPreviewResponse previewRedeem(RedeemPreviewRequest request, String fallbackUserId) {
        String userId = resolveUserId(request.getUserId(), fallbackUserId);
        if (request.getRequestedPoints() == null || request.getRequestedPoints() <= 0) {
            throw new IllegalArgumentException("requestedPoints must be greater than 0");
        }

        UserLoyalty wallet = getOrCreateWallet(userId);
        int requested = request.getRequestedPoints();
        int current = wallet.getCurrentPoints() == null ? 0 : wallet.getCurrentPoints();

        if (requested > current) {
            return RedeemPreviewResponse.builder()
                    .valid(false)
                    .requestedPoints(requested)
                    .appliedPoints(0)
                    .currentPoints(current)
                    .pointToVnd(loyaltyProperties.getPointToVnd())
                    .discountAmount(BigDecimal.ZERO)
                    .message("Requested points exceed current balance")
                    .build();
        }

        int maxByOrder = calculateMaxPointsByOrder(request.getOrderAmount());
        int applied = Math.min(requested, maxByOrder);
        BigDecimal discount = calculateDiscountFromPoints(applied);

        String message = applied < requested
                ? "Requested points exceed order payable amount, max applicable points were used"
                : "Points are applicable";

        return RedeemPreviewResponse.builder()
                .valid(applied > 0)
                .requestedPoints(requested)
                .appliedPoints(applied)
                .currentPoints(current)
                .pointToVnd(loyaltyProperties.getPointToVnd())
                .discountAmount(discount)
                .message(message)
                .build();
    }

    @Transactional
    public LoyaltyMutationResponse redeem(RedeemRequest request, String fallbackUserId) {
        String userId = resolveUserId(request.getUserId(), fallbackUserId);
        String refId = requireNonBlank(request.getRefId(), "refId is required");

        PointTransaction existing = pointTransactionRepository
                .findByUserIdAndTypeAndRefId(userId, PointTransactionType.REDEEM, refId)
                .orElse(null);
        if (existing != null) {
            int appliedPoints = Math.abs(existing.getPoints());
            return LoyaltyMutationResponse.builder()
                    .appliedPoints(appliedPoints)
                    .currentPoints(getOrCreateWallet(userId).getCurrentPoints())
                    .discountAmount(calculateDiscountFromPoints(appliedPoints))
                    .idempotent(true)
                    .message("Redeem already processed for this reference")
                    .build();
        }

        if (request.getRequestedPoints() == null || request.getRequestedPoints() <= 0) {
            throw new IllegalArgumentException("requestedPoints must be greater than 0");
        }

        UserLoyalty wallet = getOrCreateWalletForUpdate(userId);
        int current = wallet.getCurrentPoints() == null ? 0 : wallet.getCurrentPoints();
        int requested = request.getRequestedPoints();

        if (requested > current) {
            throw new IllegalArgumentException("Requested points exceed current balance");
        }

        int maxByOrder = calculateMaxPointsByOrder(request.getOrderAmount());
        int applied = Math.min(requested, maxByOrder);
        if (applied <= 0) {
            throw new IllegalArgumentException("Requested points are not applicable to current order amount");
        }

        BigDecimal discount = calculateDiscountFromPoints(applied);
        wallet.setCurrentPoints(current - applied);
        userLoyaltyRepository.save(wallet);

        PointTransaction tx = PointTransaction.builder()
                .transactionId(UUID.randomUUID().toString())
                .userId(userId)
                .type(PointTransactionType.REDEEM)
                .points(-applied)
                .refId(refId)
                .description(defaultDescription(request.getDescription(), "Redeem points for order " + refId))
                .build();
        pointTransactionRepository.save(tx);

        return LoyaltyMutationResponse.builder()
                .appliedPoints(applied)
                .currentPoints(wallet.getCurrentPoints())
                .discountAmount(discount)
                .idempotent(false)
                .message("Redeem success")
                .build();
    }

    @Transactional
    public LoyaltyMutationResponse refund(RefundRequest request, String fallbackUserId) {
        String userId = resolveUserId(request.getUserId(), fallbackUserId);
        String refId = requireNonBlank(request.getRefId(), "refId is required");

        PointTransaction existingRefund = pointTransactionRepository
                .findByUserIdAndTypeAndRefId(userId, PointTransactionType.REFUND, refId)
                .orElse(null);
        if (existingRefund != null) {
            int refundedPoints = Math.max(existingRefund.getPoints(), 0);
            return LoyaltyMutationResponse.builder()
                    .appliedPoints(refundedPoints)
                    .currentPoints(getOrCreateWallet(userId).getCurrentPoints())
                    .discountAmount(calculateDiscountFromPoints(refundedPoints))
                    .idempotent(true)
                    .message("Refund already processed for this reference")
                    .build();
        }

        PointTransaction redeemTx = pointTransactionRepository
                .findByUserIdAndTypeAndRefId(userId, PointTransactionType.REDEEM, refId)
                .orElseThrow(() -> new IllegalArgumentException("No redeemed points found for this reference"));

        int refundedPoints = Math.abs(redeemTx.getPoints());
        if (refundedPoints <= 0) {
            throw new IllegalStateException("Invalid redeemed points for refund");
        }

        UserLoyalty wallet = getOrCreateWalletForUpdate(userId);
        int current = wallet.getCurrentPoints() == null ? 0 : wallet.getCurrentPoints();
        wallet.setCurrentPoints(current + refundedPoints);
        userLoyaltyRepository.save(wallet);

        PointTransaction refundTx = PointTransaction.builder()
                .transactionId(UUID.randomUUID().toString())
                .userId(userId)
                .type(PointTransactionType.REFUND)
                .points(refundedPoints)
                .refId(refId)
                .description(defaultDescription(request.getDescription(), "Refund points for cancelled order " + refId))
                .build();
        pointTransactionRepository.save(refundTx);

        return LoyaltyMutationResponse.builder()
                .appliedPoints(refundedPoints)
                .currentPoints(wallet.getCurrentPoints())
                .discountAmount(calculateDiscountFromPoints(refundedPoints))
                .idempotent(false)
                .message("Refund success")
                .build();
    }

    @Transactional
    public LoyaltyMutationResponse earnFromOrder(EarnOrderPointsRequest request, String fallbackUserId) {
        String userId = resolveUserId(request.getUserId(), fallbackUserId);
        String refId = requireNonBlank(request.getOrderId(), "orderId is required");

        PointTransaction existing = pointTransactionRepository
                .findByUserIdAndTypeAndRefId(userId, PointTransactionType.EARN_ORDER, refId)
                .orElse(null);
        if (existing != null) {
            int earned = Math.max(existing.getPoints(), 0);
            return LoyaltyMutationResponse.builder()
                    .appliedPoints(earned)
                    .currentPoints(getOrCreateWallet(userId).getCurrentPoints())
                    .discountAmount(BigDecimal.ZERO)
                    .idempotent(true)
                    .message("Order earning already processed")
                    .build();
        }

        UserLoyalty wallet = getOrCreateWalletForUpdate(userId);
        MembershipTier currentTier = wallet.getTier();
        int earnedPoints = calculateOrderEarnPoints(request.getNetAmount(), currentTier.getPointRate());

        int current = wallet.getCurrentPoints() == null ? 0 : wallet.getCurrentPoints();
        wallet.setCurrentPoints(current + earnedPoints);
        wallet.setTotalSpending(wallet.getTotalSpending().add(request.getNetAmount()));

        MembershipTier newTier = resolveTierBySpending(wallet.getTotalSpending());
        wallet.setTier(newTier);
        userLoyaltyRepository.save(wallet);

        PointTransaction tx = PointTransaction.builder()
                .transactionId(UUID.randomUUID().toString())
                .userId(userId)
                .type(PointTransactionType.EARN_ORDER)
                .points(earnedPoints)
                .refId(refId)
                .description(defaultDescription(request.getDescription(), "Earn points from delivered order " + refId))
                .build();
        pointTransactionRepository.save(tx);

        return LoyaltyMutationResponse.builder()
                .appliedPoints(earnedPoints)
                .currentPoints(wallet.getCurrentPoints())
                .discountAmount(BigDecimal.ZERO)
                .idempotent(false)
                .message("Earn points from order success")
                .build();
    }

    @Transactional
    public LoyaltyMutationResponse earnFromReview(EarnReviewPointsRequest request, String fallbackUserId) {
        String userId = resolveUserId(request.getUserId(), fallbackUserId);
        String refId = requireNonBlank(request.getReviewId(), "reviewId is required");

        PointTransaction existing = pointTransactionRepository
                .findByUserIdAndTypeAndRefId(userId, PointTransactionType.EARN_REVIEW, refId)
                .orElse(null);
        if (existing != null) {
            int earned = Math.max(existing.getPoints(), 0);
            return LoyaltyMutationResponse.builder()
                    .appliedPoints(earned)
                    .currentPoints(getOrCreateWallet(userId).getCurrentPoints())
                    .discountAmount(BigDecimal.ZERO)
                    .idempotent(true)
                    .message("Review earning already processed")
                    .build();
        }

        int earnedPoints = Math.max(loyaltyProperties.getReviewBonusPoints(), 0);
        UserLoyalty wallet = getOrCreateWalletForUpdate(userId);
        int current = wallet.getCurrentPoints() == null ? 0 : wallet.getCurrentPoints();
        wallet.setCurrentPoints(current + earnedPoints);
        userLoyaltyRepository.save(wallet);

        PointTransaction tx = PointTransaction.builder()
                .transactionId(UUID.randomUUID().toString())
                .userId(userId)
                .type(PointTransactionType.EARN_REVIEW)
                .points(earnedPoints)
                .refId(refId)
                .description(defaultDescription(request.getDescription(), "Earn points from approved review " + refId))
                .build();
        pointTransactionRepository.save(tx);

        return LoyaltyMutationResponse.builder()
                .appliedPoints(earnedPoints)
                .currentPoints(wallet.getCurrentPoints())
                .discountAmount(BigDecimal.ZERO)
                .idempotent(false)
                .message("Earn points from review success")
                .build();
    }

    private LoyaltyWalletResponse toWalletResponse(UserLoyalty wallet) {
        MembershipTier tier = wallet.getTier();
        return LoyaltyWalletResponse.builder()
                .userId(wallet.getUserId())
                .currentPoints(wallet.getCurrentPoints())
                .totalSpending(wallet.getTotalSpending())
                .tierId(tier.getTierId())
                .tierName(tier.getName())
                .tierDiscountPercent(tier.getDiscountPercent())
                .tierPointRate(tier.getPointRate())
                .pointToVnd(loyaltyProperties.getPointToVnd())
                .build();
    }

    private PointTransactionResponse toPointTransactionResponse(PointTransaction tx) {
        return PointTransactionResponse.builder()
                .transactionId(tx.getTransactionId())
                .type(tx.getType())
                .points(tx.getPoints())
                .refId(tx.getRefId())
                .description(tx.getDescription())
                .createdAt(tx.getCreatedAt())
                .build();
    }

    private UserLoyalty getOrCreateWallet(String userId) {
        return userLoyaltyRepository.findByUserId(userId)
                .orElseGet(() -> createWallet(userId));
    }

    private UserLoyalty getOrCreateWalletForUpdate(String userId) {
        return userLoyaltyRepository.findByUserIdForUpdate(userId)
                .orElseGet(() -> {
                    try {
                        UserLoyalty created = createWallet(userId);
                        return userLoyaltyRepository.findByUserIdForUpdate(userId).orElse(created);
                    } catch (DataIntegrityViolationException ex) {
                        return userLoyaltyRepository.findByUserIdForUpdate(userId)
                                .orElseThrow(() -> new IllegalStateException("Unable to load loyalty wallet"));
                    }
                });
    }

    private UserLoyalty createWallet(String userId) {
        MembershipTier defaultTier = membershipTierRepository.findFirstByOrderByMinSpendingAsc()
                .orElseThrow(() -> new IllegalStateException("No membership tier configured"));

        UserLoyalty wallet = UserLoyalty.builder()
                .loyaltyId(UUID.randomUUID().toString())
                .userId(userId)
                .tier(defaultTier)
                .currentPoints(0)
                .totalSpending(BigDecimal.ZERO)
                .build();

        return userLoyaltyRepository.save(wallet);
    }

    private MembershipTier resolveTierBySpending(BigDecimal totalSpending) {
        List<MembershipTier> tiers = membershipTierRepository.findAllByOrderByMinSpendingDesc();
        return tiers.stream()
                .filter(tier -> totalSpending.compareTo(tier.getMinSpending()) >= 0)
                .findFirst()
                .orElseGet(() -> membershipTierRepository.findFirstByOrderByMinSpendingAsc()
                        .orElseThrow(() -> new IllegalStateException("No membership tier configured")));
    }

    private int calculateOrderEarnPoints(BigDecimal netAmount, BigDecimal pointRate) {
        BigDecimal points = netAmount
                .multiply(loyaltyProperties.getOrderPointPercent())
                .multiply(pointRate)
                .divide(BigDecimal.valueOf(loyaltyProperties.getPointToVnd()), 0, RoundingMode.DOWN);
        return Math.max(points.intValue(), 0);
    }

    private int calculateMaxPointsByOrder(BigDecimal orderAmount) {
        if (orderAmount == null || orderAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("orderAmount must be greater than 0");
        }
        return orderAmount
                .divide(BigDecimal.valueOf(loyaltyProperties.getPointToVnd()), 0, RoundingMode.DOWN)
                .intValue();
    }

    private BigDecimal calculateDiscountFromPoints(int points) {
        return BigDecimal.valueOf(points)
                .multiply(BigDecimal.valueOf(loyaltyProperties.getPointToVnd()));
    }

    private String resolveUserId(String primaryUserId, String fallbackUserId) {
        String requestUserId = primaryUserId == null ? null : primaryUserId.trim();
        String headerUserId = fallbackUserId == null ? null : fallbackUserId.trim();

        if (headerUserId != null && !headerUserId.isBlank()) {
            if (requestUserId != null && !requestUserId.isBlank() && !headerUserId.equals(requestUserId)) {
                throw new IllegalArgumentException("userId in header does not match request body");
            }
            return requireUserId(headerUserId);
        }

        return requireUserId(requestUserId);
    }

    private String requireUserId(String userId) {
        String normalized = requireNonBlank(userId, "userId is required");
        if (normalized.startsWith("guest-")) {
            throw new IllegalArgumentException("Guest account is not eligible for loyalty points");
        }
        return normalized;
    }

    private String requireNonBlank(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    private String defaultDescription(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }
}
