package com.fashion.promotionservice.controller;

// import com.fashion.promotionservice.entity.Coupon;
import com.fashion.promotionservice.repository.CouponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/promotions")
@RequiredArgsConstructor
public class PromotionController {

    private final CouponRepository couponRepository;

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
