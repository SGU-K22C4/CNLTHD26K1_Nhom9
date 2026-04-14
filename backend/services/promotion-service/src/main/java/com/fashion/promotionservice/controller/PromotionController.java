package com.fashion.promotionservice.controller;

import com.fashion.promotionservice.dto.response.ActivePromotionResponse;
import com.fashion.promotionservice.repository.CouponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/promotions")
@RequiredArgsConstructor
public class PromotionController {

    private static final DateTimeFormatter DATE_FORMATTER =
        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale.forLanguageTag("vi-VN"));

    private final CouponRepository couponRepository;

    @GetMapping("/active")
    public ResponseEntity<List<ActivePromotionResponse>> activePromotions() {
    LocalDateTime now = LocalDateTime.now();

    List<ActivePromotionResponse> promotions = couponRepository
        .findByActiveTrueAndStartDateBeforeAndEndDateAfterOrderByEndDateAsc(now, now)
        .stream()
        .map(coupon -> ActivePromotionResponse.builder()
            .code(coupon.getCode())
            .discountType(coupon.getDiscountType().name())
            .discountValue(coupon.getDiscountValue().stripTrailingZeros().toPlainString())
            .minOrderAmount(coupon.getMinOrderAmount() == null
                ? "0"
                : coupon.getMinOrderAmount().stripTrailingZeros().toPlainString())
            .maxDiscountAmount(coupon.getMaxDiscountAmount() == null
                ? ""
                : coupon.getMaxDiscountAmount().stripTrailingZeros().toPlainString())
            .endDate(coupon.getEndDate().format(DATE_FORMATTER))
            .build())
        .toList();

    return ResponseEntity.ok(promotions);
    }

    @PostMapping("/validate")
    public ResponseEntity<Map<String, Object>> validate(
            @RequestParam String code,
            @RequestParam BigDecimal orderAmount) {
        LocalDateTime now = LocalDateTime.now();
        return couponRepository
                .findByCodeAndActiveTrueAndStartDateBeforeAndEndDateAfter(code, now, now)
                .map(coupon -> {
                    if (coupon.getUsageLimit() != null && coupon.getUsedCount() >= coupon.getUsageLimit()) {
                        return ResponseEntity
                                .ok(Map.<String, Object>of("valid", false, "reason", "Coupon đã hết lượt sử dụng"));
                    }
                    if (coupon.getMinOrderAmount() != null && orderAmount.compareTo(coupon.getMinOrderAmount()) < 0) {
                        return ResponseEntity.ok(Map.<String, Object>of("valid", false, "reason",
                                "Đơn hàng tối thiểu " + coupon.getMinOrderAmount()));
                    }
                    BigDecimal discount = coupon.calculate(orderAmount);
                    return ResponseEntity.ok(Map.<String, Object>of(
                            "valid", true,
                            "discount", discount,
                            "code", coupon.getCode(),
                            "type", coupon.getDiscountType()));
                })
                .orElse(ResponseEntity.ok(Map.of("valid", false, "reason", "Mã không hợp lệ hoặc đã hết hạn")));
    }
}
