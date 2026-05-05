package com.fashion.chatbotservice.service;

import com.fashion.chatbotservice.model.ChatSession;

/**
 * Enriches user preference profile từ tin nhắn và lịch sử mua hàng.
 */
public interface ProfileEnrichmentService {

    /**
     * Trích xuất sở thích ẩn từ nội dung tin nhắn vào profile.
     */
    void enrichFromMessage(ChatSession.PreferenceProfile profile, String message);

    /**
     * Hydrate profile từ lịch sử mua hàng (gọi order-service).
     */
    void enrichFromPurchaseHistory(ChatSession.PreferenceProfile profile, String userId);

    /**
     * Hydrate profile từ wishlist (gọi product-service).
     */
    void enrichFromWishlist(ChatSession.PreferenceProfile profile, String userId);

    /**
     * Hydrate profile từ user profile (gọi user-service).
     */
    void enrichFromUserProfile(ChatSession.PreferenceProfile profile, String userId);

    /**
     * Persist profile vào MongoDB để nhớ sở thích user qua nhiều phiên chat.
     * Fire-and-forget: không block request chính.
     */
    void persistProfileAsync(String userId, ChatSession.PreferenceProfile profile);

    /**
     * Load profile đã lưu từ MongoDB cho user đã đăng nhập.
     * Trả về profile đã lưu hoặc PreferenceProfile.empty() nếu chưa có.
     */
    ChatSession.PreferenceProfile loadPersistedProfile(String userId);
}
