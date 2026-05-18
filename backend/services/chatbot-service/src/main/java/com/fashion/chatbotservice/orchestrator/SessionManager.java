package com.fashion.chatbotservice.orchestrator;

import com.fashion.chatbotservice.model.ChatSession;
import com.fashion.chatbotservice.repository.ChatSessionRepository;
import com.fashion.chatbotservice.dto.ChatResponse;
import com.fashion.chatbotservice.dto.SessionResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Quản lý session: tạo mới, tìm kiếm, persist messages.
 * Tách ra từ ChatbotServiceImpl (findOrCreateSession, persistMessages, getSession).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SessionManager {

    private final ChatSessionRepository chatSessionRepository;

    /**
     * Tìm session hoặc tạo mới nếu chưa có.
     */
    public ChatSession findOrCreate(String sessionId, String userId) {
        try {
            return chatSessionRepository.findBySessionId(sessionId)
                    .orElseGet(() -> createNewSession(sessionId, userId));
        } catch (Exception ex) {
            log.warn("MongoDB not available, creating in-memory session: {}", ex.getMessage());
            return createNewSession(sessionId, userId);
        }
    }

    /**
     * Giải quyết userId: ưu tiên header, fallback sang guest.
     */
    public String resolveUserId(String userIdHeader, String sessionId) {
        if (userIdHeader != null && !userIdHeader.isBlank()) {
            return userIdHeader;
        }
        return "guest-" + sessionId;
    }

    /**
     * Lưu tin nhắn user và bot vào session.
     */
    public void persistMessages(ChatSession session, String userMessage, ChatResponse botResponse) {
        if (session == null || userMessage == null || botResponse == null) return;

        // User message
        ChatSession.ChatMessage userMsg = ChatSession.ChatMessage.builder()
                .messageId(UUID.randomUUID().toString())
                .sender(ChatSession.Sender.USER)
                .content(userMessage)
                .createdAt(Instant.now())
                .build();

        // Bot message
        List<ChatSession.ProductSuggestionSnapshot> snapshots = botResponse.getSuggestions() == null
                ? List.of()
                : botResponse.getSuggestions().stream()
                        .map(s -> ChatSession.ProductSuggestionSnapshot.builder()
                                .productId(s.getProductId())
                                .name(s.getName())
                                .category(s.getCategory())
                                .categoryGender(s.getCategoryGender())
                                .imageUrl(s.getImageUrl())
                                .link(s.getLink())
                                .price(s.getPrice())
                                .availableSizes(s.getAvailableSizes())
                                .availableColors(s.getAvailableColors())
                                .build())
                        .toList();

        List<ChatSession.PromotionSuggestionSnapshot> promoSnapshots = botResponse.getPromotions() == null
                ? List.of()
                : botResponse.getPromotions().stream()
                        .map(p -> ChatSession.PromotionSuggestionSnapshot.builder()
                                .code(p.getCode())
                                .discountType(p.getDiscountType())
                                .discountValue(p.getDiscountValue())
                                .minOrderAmount(p.getMinOrderAmount())
                                .endDate(p.getEndDate())
                                .build())
                        .toList();

        ChatSession.ChatMessage botMsg = ChatSession.ChatMessage.builder()
                .messageId(UUID.randomUUID().toString())
                .sender(ChatSession.Sender.BOT)
                .content(botResponse.getReply())
                .intent(ChatSession.IntentMeta.builder()
                        .intentName(botResponse.getIntent())
                        .confidence(botResponse.getConfidence())
                        .build())
                .suggestions(snapshots)
                .promotions(promoSnapshots)
                .createdAt(Instant.now())
                .build();

        if (session.getMessages() == null) {
            session.setMessages(new java.util.ArrayList<>());
        }
        session.getMessages().add(userMsg);
        session.getMessages().add(botMsg);

        try {
            chatSessionRepository.save(session);
        } catch (Exception ex) {
            log.warn("Failed to persist chat messages for session {}: {}", session.getSessionId(), ex.getMessage());
        }
    }

    /**
     * Trả về session response cho endpoint GET /session/{sessionId}.
     */
    public SessionResponse getSession(String sessionId) {
        ChatSession session;
        try {
            session = chatSessionRepository.findBySessionId(sessionId).orElse(null);
        } catch (Exception ex) {
            throw new IllegalStateException("MongoDB chưa kết nối, chưa thể lấy lịch sử chat");
        }
        if (session == null) {
            return SessionResponse.builder()
                    .sessionId(sessionId)
                    .messages(List.of())
                    .build();
        }
        return SessionResponse.builder()
                .sessionId(session.getSessionId())
                .userId(session.getUserId())
                .messages(session.getMessages())
                .profile(session.getPreferenceProfile())
                .build();
    }

    // ────────────────────────────────────────────────
    private ChatSession createNewSession(String sessionId, String userId) {
        ChatSession session = ChatSession.builder()
                .sessionId(sessionId)
                .userId(userId)
                .startedAt(Instant.now())
                .preferenceProfile(ChatSession.PreferenceProfile.empty())
                .build();
        try {
            chatSessionRepository.save(session);
        } catch (Exception ex) {
            log.warn("Could not persist new session {} to MongoDB: {}", sessionId, ex.getMessage());
        }
        return session;
    }
}
