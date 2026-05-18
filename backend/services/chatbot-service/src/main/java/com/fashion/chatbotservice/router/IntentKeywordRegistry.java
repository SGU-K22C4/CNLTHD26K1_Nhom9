package com.fashion.chatbotservice.router;

import com.fashion.chatbotservice.service.IntentClassifierService;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Registry chứa danh sách keywords đại diện cho từng intent.
 * Keywords được sắp xếp theo trọng số: prime keywords (đầu list) có trọng số cao hơn.
 *
 * <p>Được dùng bởi {@link SemanticIntentRouter} để tính similarity score mà không
 * cần gọi external embedding API.
 */
@Component
public class IntentKeywordRegistry {

    private final Map<String, List<String>> intentKeywords;

    public IntentKeywordRegistry() {
        intentKeywords = new LinkedHashMap<>();

        // SEARCH_PRODUCT — tìm sản phẩm
        intentKeywords.put(IntentClassifierService.SEARCH_PRODUCT, List.of(
                "tim", "mua", "co ban", "san pham", "ao", "quan", "vay", "dam",
                "ao thun", "ao so mi", "ao khoac", "quan jean", "gia", "mau",
                "hang", "chon", "goi y", "loai nao", "co gi"
        ));

        // CONSULT_SIZE — tư vấn size
        intentKeywords.put(IntentClassifierService.CONSULT_SIZE, List.of(
                "size", "so do", "chieu cao", "can nang", "cm", "kg",
                "mac size nao", "nen chon size", "phu hop", "vua",
                "lon hon", "nho hon", "oversize", "slim"
        ));

        // CONSULT_SEASON — tư vấn outfit theo mùa/dịp
        intentKeywords.put(IntentClassifierService.CONSULT_SEASON, List.of(
                "mac gi", "phoi do", "outfit", "set do", "hop voi",
                "di lam mac", "di tiec mac", "du lich mac", "goi y do",
                "phong cach", "mac the nao", "ket hop"
        ));

        // ASK_PROMOTION — hỏi khuyến mãi
        intentKeywords.put(IntentClassifierService.ASK_PROMOTION, List.of(
                "khuyen mai", "voucher", "giam gia", "uu dai", "ma giam",
                "sale", "discount", "khuyen mai gi", "co voucher", "ma code",
                "giam them", "freeship"
        ));

        // ASK_POLICY — hỏi chính sách
        intentKeywords.put(IntentClassifierService.ASK_POLICY, List.of(
                "chinh sach", "doi tra", "bao hanh", "giao hang", "thanh toan",
                "hoan tien", "nguyen tac", "quy dinh", "giao hang mat bao lau",
                "doi hang", "tra hang", "phi ship"
        ));

        // CHECK_ORDER — kiểm tra đơn hàng
        intentKeywords.put(IntentClassifierService.CHECK_ORDER, List.of(
                "don hang", "ORD", "ma don", "trang thai don", "theo doi don",
                "don chua den", "giao hang bao lau", "kiem tra don",
                "don dau roi", "huy don"
        ));

        // WISHLIST_RECOMMENDATION — wishlist
        intentKeywords.put(IntentClassifierService.WISHLIST_RECOMMENDATION, List.of(
                "wishlist", "da luu", "san pham da luu", "yeu thich",
                "danh sach yeu thich", "luu do", "xem lai do da luu"
        ));

        // LOYALTY_BENEFIT — điểm thưởng
        intentKeywords.put(IntentClassifierService.LOYALTY_BENEFIT, List.of(
                "diem thuong", "thanh vien", "loyalty", "tier", "vip",
                "diem tich luy", "quyen loi", "diem hien co", "nap diem",
                "doi diem", "uu dai thanh vien"
        ));

        // GREETING — chào hỏi
        intentKeywords.put(IntentClassifierService.GREETING, List.of(
                "xin chao", "hello", "chao", "hi", "hey",
                "cam on", "thank", "tam biet", "bye", "bạn ơi"
        ));

        // OUT_OF_DOMAIN — ngoài phạm vi
        intentKeywords.put(IntentClassifierService.OUT_OF_DOMAIN, List.of(
                "thoi tiet", "bong da", "nau an", "lich su", "tin tuc",
                "choi game", "am nhac", "phim", "loan", "chinh tri",
                "toan hoc", "vat ly"
        ));
    }

    public Map<String, List<String>> getIntentKeywords() {
        return Collections.unmodifiableMap(intentKeywords);
    }
}
