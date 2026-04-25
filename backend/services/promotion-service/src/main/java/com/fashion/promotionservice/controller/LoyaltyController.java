package com.fashion.promotionservice.controller;

import com.fashion.promotionservice.dto.request.EarnOrderPointsRequest;
import com.fashion.promotionservice.dto.request.EarnReviewPointsRequest;
import com.fashion.promotionservice.dto.request.RedeemPreviewRequest;
import com.fashion.promotionservice.dto.request.RedeemRequest;
import com.fashion.promotionservice.dto.request.RefundRequest;
import com.fashion.promotionservice.dto.response.LoyaltyMutationResponse;
import com.fashion.promotionservice.dto.response.LoyaltyWalletResponse;
import com.fashion.promotionservice.dto.response.PointTransactionResponse;
import com.fashion.promotionservice.dto.response.RedeemPreviewResponse;
import com.fashion.promotionservice.service.LoyaltyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/promotions/loyalty")
@RequiredArgsConstructor
public class LoyaltyController {

    private final LoyaltyService loyaltyService;

    @GetMapping("/wallet")
    public ResponseEntity<LoyaltyWalletResponse> getWallet(
            @RequestHeader("X-User-Id") String userId) {
        return ResponseEntity.ok(loyaltyService.getWallet(userId));
    }

    @GetMapping("/transactions")
    public ResponseEntity<List<PointTransactionResponse>> getTransactions(
            @RequestHeader("X-User-Id") String userId,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(loyaltyService.getTransactions(userId, size));
    }

    @PostMapping("/redeem/preview")
    public ResponseEntity<RedeemPreviewResponse> previewRedeem(
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody RedeemPreviewRequest request) {
        return ResponseEntity.ok(loyaltyService.previewRedeem(request, userId));
    }

    @PostMapping("/redeem")
    public ResponseEntity<LoyaltyMutationResponse> redeem(
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody RedeemRequest request) {
        return ResponseEntity.ok(loyaltyService.redeem(request, userId));
    }

    @PostMapping("/refund")
    public ResponseEntity<LoyaltyMutationResponse> refund(
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody RefundRequest request) {
        return ResponseEntity.ok(loyaltyService.refund(request, userId));
    }

    @PostMapping("/earn/order")
    public ResponseEntity<LoyaltyMutationResponse> earnFromOrder(
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody EarnOrderPointsRequest request) {
        return ResponseEntity.ok(loyaltyService.earnFromOrder(request, userId));
    }

    @PostMapping("/earn/review")
    public ResponseEntity<LoyaltyMutationResponse> earnFromReview(
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody EarnReviewPointsRequest request) {
        return ResponseEntity.ok(loyaltyService.earnFromReview(request, userId));
    }
}
