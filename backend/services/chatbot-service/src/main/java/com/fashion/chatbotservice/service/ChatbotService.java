package com.fashion.chatbotservice.service;

import com.fashion.chatbotservice.agent.FashionAgent;
import com.fashion.chatbotservice.agent.FashionTools;
import com.fashion.chatbotservice.agent.ResponseAssembler;
import com.fashion.chatbotservice.agent.ToolResultCollector;
import com.fashion.chatbotservice.dto.ChatRequest;
import com.fashion.chatbotservice.dto.ChatResponse;
import com.fashion.chatbotservice.dto.SessionResponse;
import com.fashion.chatbotservice.model.ChatSession;
import com.fashion.chatbotservice.repository.ChatSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

/**
 * Orchestrator chính: nhận request → enrich profile → gọi agent → assemble response → persist.
 * Hỗ trợ feature flag để chuyển đổi giữa agent mode và heuristic fallback.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChatbotService {

    private final FashionAgent fashionAgent;
    private final FashionTools fashionTools;
    private final ChatSessionRepository chatSessionRepository;
    private final IntentClassifierService intentClassifierService;
    private final ProfileEnrichmentService profileEnrichmentService;
    private final ChatAnalyticsService chatAnalyticsService;
    private final SizeAdvisorService sizeAdvisorService;

    @Value("${chatbot.use-agent:true}")
    private boolean useAgent;

    @PostConstruct
    public void bootstrapTrainingData() {
        try {
            intentClassifierService.bootstrapDefaultIntentsIfNeeded();
        } catch (Exception ex) {
            log.warn("Skip bootstrap training data at startup: {}", ex.getMessage());
        }
    }

    public ChatResponse chat(ChatRequest request, String userIdHeader, String traceId) {
        long startTime = System.currentTimeMillis();

        if (request == null || request.getMessage() == null || request.getMessage().isBlank()) {
            throw new IllegalArgumentException("Nội dung tin nhắn không được để trống");
        }

        String sessionId = Optional.ofNullable(request.getSessionId())
                .filter(s -> !s.isBlank())
                .orElseGet(() -> UUID.randomUUID().toString());

        String userId = resolveUserId(userIdHeader, sessionId);
        ChatSession session = findOrCreateSession(sessionId, userId);
        mergePreferences(session, request.getPreferences());

        // Enrich profile từ message + purchase history
        profileEnrichmentService.enrichFromMessage(session.getPreferenceProfile(), request.getMessage());
        profileEnrichmentService.enrichFromPurchaseHistory(session.getPreferenceProfile(), userId);

        // Execute agent hoặc heuristic fallback
        ChatResponse response;
        ToolResultCollector collector = new ToolResultCollector();

        if (useAgent) {
            response = executeAgent(sessionId, request.getMessage(), session, collector);
        } else {
            response = executeHeuristicFallback(sessionId, request.getMessage(), session, collector);
        }

        // Persist messages to MongoDB
        persistMessages(session, request.getMessage(), response);

        // Analytics
        long totalLatency = System.currentTimeMillis() - startTime;
        chatAnalyticsService.record(traceId, sessionId, userId,
                request.getMessage(), response, collector, totalLatency);

        return response;
    }

    public SessionResponse getSession(String sessionId) {
        ChatSession session;
        try {
            session = chatSessionRepository.findBySessionId(sessionId)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phiên chat"));
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalStateException("MongoDB chưa kết nối, chưa thể lấy lịch sử chat");
        }

        return SessionResponse.builder()
                .sessionId(session.getSessionId())
                .userId(session.getUserId())
                .messages(session.getMessages())
                .profile(session.getPreferenceProfile())
                .build();
    }

    /**
     * Chế độ Agent: LLM tự quyết định gọi tool nào.
     */
    private ChatResponse executeAgent(String sessionId, String message,
                                       ChatSession session, ToolResultCollector collector) {
        try {
            fashionTools.setCollector(collector);
            String llmReply = fashionAgent.chat(sessionId, message);
            return ResponseAssembler.build(sessionId, llmReply, collector, session.getPreferenceProfile());
        } catch (Exception ex) {
            log.error("Agent execution failed, falling back to heuristic: {}", ex.getMessage());
            return executeHeuristicFallback(sessionId, message, session, collector);
        } finally {
            fashionTools.clearCollector();
        }
    }

    /**
     * Chế độ Heuristic fallback: dùng IntentClassifier khi agent lỗi hoặc bị disable.
     * Giờ đây sẽ gọi Tool thật thay vì trả câu tĩnh.
     */
    private ChatResponse executeHeuristicFallback(String sessionId, String message,
                                                   ChatSession session, ToolResultCollector collector) {
        IntentClassifierService.IntentScore intentScore = intentClassifierService.classify(message);
        ChatSession.PreferenceProfile profile = session.getPreferenceProfile();

        String reply = switch (intentScore.intent()) {

            case IntentClassifierService.CONSULT_SIZE -> {
                // Trích xuất số đo mới từ tin nhắn hiện tại
                SizeAdvisorService.Measurements extracted = sizeAdvisorService.extractMeasurements(message);

                // Merge với số đo đã lưu trong session (context memory)
                Integer heightCm = extracted.heightCm() != null ? extracted.heightCm() : profile.getLastHeightCm();
                Integer weightKg = extracted.weightKg() != null ? extracted.weightKg() : profile.getLastWeightKg();
                Integer chestCm  = extracted.chestCm()  != null ? extracted.chestCm()  : profile.getLastChestCm();
                Integer waistCm  = extracted.waistCm()  != null ? extracted.waistCm()  : profile.getLastWaistCm();
                Integer hipCm    = extracted.hipCm()    != null ? extracted.hipCm()    : profile.getLastHipCm();

                SizeAdvisorService.Measurements merged = new SizeAdvisorService.Measurements(
                        heightCm, weightKg, chestCm, waistCm, hipCm);

                if (!merged.hasMinimumData()) {
                    collector.addMissingFields(merged.missingFields());
                    yield "Để tư vấn size chính xác, bạn cung cấp thêm: " + String.join(", ", merged.missingFields());
                }

                // Lưu số đo vào session profile để dùng cho câu tiếp theo
                profile.setLastHeightCm(heightCm);
                profile.setLastWeightKg(weightKg);
                profile.setLastChestCm(chestCm);
                profile.setLastWaistCm(waistCm);
                profile.setLastHipCm(hipCm);

                SizeAdvisorService.GarmentType type = sizeAdvisorService.detectGarmentType(message);
                SizeAdvisorService.SizeResult result = sizeAdvisorService.suggest(merged, type);
                collector.setSizeRecommendation(result.recommendedSize());

                // === CẢI TIẾN: Tư vấn size xong → tự động tìm sản phẩm phù hợp ===
                String garmentKeyword = extractGarmentKeyword(message);
                if (garmentKeyword != null && !garmentKeyword.isBlank()) {
                    try {
                        fashionTools.setCollector(collector);
                        Double maxPrice = parseBudget(profile.getBudget());
                        fashionTools.searchProducts(garmentKeyword, null, maxPrice != null ? maxPrice.longValue() : null);
                    } catch (Exception ex) {
                        log.debug("Auto product search after size advice skipped: {}", ex.getMessage());
                    } finally {
                        fashionTools.clearCollector();
                    }
                }

                StringBuilder sizeReply = new StringBuilder();
                sizeReply.append("Với chiều cao ").append(heightCm).append("cm và cân nặng ").append(weightKg)
                        .append("kg, mình gợi ý bạn chọn **size ").append(result.recommendedSize()).append("**. ")
                        .append(result.note());
                if (!collector.getProducts().isEmpty()) {
                    sizeReply.append("\n\nMình cũng tìm thấy một số sản phẩm phù hợp cho bạn:");
                }
                yield sizeReply.toString();
            }

            case IntentClassifierService.ASK_PROMOTION -> {
                // Gọi Tool thật để lấy khuyến mãi đang hiệu lực
                try {
                    fashionTools.setCollector(collector);
                    String toolResult = fashionTools.getActivePromotions();
                    yield toolResult;
                } catch (Exception ex) {
                    log.warn("Heuristic promotion lookup failed: {}", ex.getMessage());
                    yield "Mình chưa thể kiểm tra khuyến mãi lúc này. Bạn thử lại sau nhé!";
                } finally {
                    fashionTools.clearCollector();
                }
            }

            case IntentClassifierService.SEARCH_PRODUCT -> {
                // Gọi Tool thật để tìm sản phẩm — áp dụng budget từ Personalization
                try {
                    String searchKeyword = extractProductSearchKeyword(message);
                    Double maxPrice = parseBudget(profile.getBudget());
                    fashionTools.setCollector(collector);
                    String toolResult = fashionTools.searchProducts(searchKeyword, null, maxPrice != null ? maxPrice.longValue() : null);
                    // Nếu không tìm thấy, gợi ý user thử keyword khác
                    if (collector.getProducts().isEmpty()) {
                        yield "Mình chưa tìm thấy sản phẩm nào khớp với \"" + searchKeyword
                                + "\". Bạn thử tìm với từ khóa ngắn hơn (VD: 'áo thun', 'quần jean', 'váy') "
                                + "hoặc cho mình biết thêm chi tiết nhé!";
                    }
                    yield toolResult;
                } catch (Exception ex) {
                    log.warn("Heuristic product search failed: {}", ex.getMessage());
                    yield "Mình chưa thể tìm sản phẩm lúc này. Bạn thử lại sau nhé!";
                } finally {
                    fashionTools.clearCollector();
                }
            }

            case IntentClassifierService.CONSULT_SEASON -> {
                // Gợi ý outfit + tự động tìm sản phẩm theo mùa
                try {
                    fashionTools.setCollector(collector);
                    String outfitResult = fashionTools.suggestOutfit(
                            extractOccasion(message), extractStyle(message));
                    yield outfitResult;
                } catch (Exception ex) {
                    yield "Mình sẵn sàng gợi ý outfit! Bạn cho mình biết dịp cụ thể (đi làm, đi tiệc, du lịch...) nhé!";
                } finally {
                    fashionTools.clearCollector();
                }
            }

            default -> {
                // Fallback: thử search product nếu message có keyword thời trang
                String keyword = extractProductSearchKeyword(message);
                if (keyword.length() >= 2) {
                    try {
                        fashionTools.setCollector(collector);
                        String searchResult = fashionTools.searchProducts(keyword, null, null);
                        if (!collector.getProducts().isEmpty()) {
                            yield searchResult;
                        }
                    } catch (Exception ignored) {
                    } finally {
                        fashionTools.clearCollector();
                    }
                }
                yield "Mình là trợ lý thời trang AI, có thể giúp bạn:\n"
                        + "• Tư vấn size (VD: 'Mình cao 1m70, nặng 65kg mặc size gì?')\n"
                        + "• Tìm sản phẩm (VD: 'Tìm áo sơ mi nam')\n"
                        + "• Kiểm tra khuyến mãi (VD: 'Có voucher nào không?')\n"
                        + "• Gợi ý outfit (VD: 'Gợi ý đồ đi tiệc')";
            }
        };

        return ChatResponse.builder()
                .sessionId(sessionId)
                .intent(intentScore.intent())
                .confidence(intentScore.confidence())
                .reply(reply)
                .missingFields(collector.getMissingFields())
                .suggestions(collector.getProducts())
                .promotions(collector.getPromotions())
                .profile(session.getPreferenceProfile())
                .createdAt(Instant.now())
                .build();
    }

    /**
     * Extract product search keyword from user message.
     * Strips Vietnamese stop words (both accented and non-accented) for better Product Service matching.
     */
    private String extractProductSearchKeyword(String message) {
        if (message == null) return "";
        // Normalize diacritics first so we only need non-accent stop words
        String normalized = java.text.Normalizer.normalize(message.toLowerCase(), java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replace('\u0111', 'd');
        // Strip non-accent stop words
        String cleaned = normalized
                .replaceAll("\\b(tim|cho|minh|mua|xem|cua|hang|co|ban|nao|giup|voi|toi|ban|thoi|nhe|nha|vay|thi|con|nua|duoc|khong|la|cai|mot|xin|gi)\\b", "")
                .replaceAll("[^a-z0-9\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim();
        return cleaned.isEmpty() ? message : cleaned;
    }

    /**
     * Trích xuất keyword loại đồ từ tin nhắn để search sản phẩm sau khi tư vấn size.
     * VD: "mình mặc áo thun size gì" → "ao thun"
     */
    private String extractGarmentKeyword(String message) {
        if (message == null) return "";
        String normalized = java.text.Normalizer.normalize(message.toLowerCase(), java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").replace('\u0111', 'd');
        // Tìm cụm từ chỉ loại đồ
        String[] garmentPatterns = {
                "ao so mi", "ao thun", "ao khoac", "ao polo", "ao hoodie", "ao len",
                "quan jean", "quan tay", "quan short", "quan dai",
                "vay", "dam", "chan vay", "ao dai"
        };
        for (String pattern : garmentPatterns) {
            if (normalized.contains(pattern)) return pattern;
        }
        // Fallback: tìm keyword đơn
        if (normalized.contains("ao")) return "ao";
        if (normalized.contains("quan")) return "quan";
        if (normalized.contains("vay") && !normalized.contains("vay neu")) return "vay";
        if (normalized.contains("dam")) return "dam";
        return "";
    }

    private String extractOccasion(String message) {
        if (message == null) return "he";
        String n = message.toLowerCase();
        if (n.contains("di lam") || n.contains("cong so")) return "di_lam";
        if (n.contains("di tiec") || n.contains("tiec")) return "di_tiec";
        if (n.contains("du lich")) return "du_lich";
        if (n.contains("mua dong") || n.contains("dong")) return "dong";
        if (n.contains("mua thu") || n.contains("thu")) return "thu";
        return "he";
    }

    private String extractStyle(String message) {
        if (message == null) return null;
        String n = message.toLowerCase();
        if (n.contains("thanh lich") || n.contains("sang trong")) return "thanh_lich";
        if (n.contains("casual") || n.contains("thoai mai")) return "casual";
        if (n.contains("sporty") || n.contains("the thao")) return "sporty";
        return null;
    }

    /**
     * Parse budget string from Personalization panel into maxPrice (Double).
     * Supports: "duoi 500.000d", "500k", "tu 200-500k", "tren 1.000.000d", etc.
     * Returns null if no budget is set (no filter applied).
     */
    private Double parseBudget(String budget) {
        if (budget == null || budget.isBlank()) return null;
        String cleaned = budget.toLowerCase()
                .replaceAll("[^0-9kmtrd.]", " ")
                .trim();
        // Extract numbers  
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("([0-9][0-9.,]*)\\s*(k|tr|trieu|d|dong)?")
                .matcher(cleaned);
        Double maxPrice = null;
        while (m.find()) {
            try {
                double value = Double.parseDouble(m.group(1).replace(".", "").replace(",", ""));
                String unit = m.group(2);
                if ("k".equals(unit)) value *= 1_000;
                else if ("tr".equals(unit) || "trieu".equals(unit)) value *= 1_000_000;
                // If multiple numbers found, take the larger one (upper bound)
                if (maxPrice == null || value > maxPrice) maxPrice = value;
            } catch (NumberFormatException ignored) {}
        }
        return maxPrice;
    }

    private void persistMessages(ChatSession session, String userMessage, ChatResponse response) {
        ChatSession.ChatMessage userMsg = ChatSession.ChatMessage.builder()
                .messageId(UUID.randomUUID().toString())
                .sender(ChatSession.Sender.USER)
                .content(userMessage)
                .intent(ChatSession.IntentMeta.builder()
                        .intentName(response.getIntent())
                        .confidence(response.getConfidence())
                        .build())
                .createdAt(Instant.now())
                .build();

        ChatSession.ChatMessage botMsg = ChatSession.ChatMessage.builder()
                .messageId(UUID.randomUUID().toString())
                .sender(ChatSession.Sender.BOT)
                .content(response.getReply())
                .createdAt(Instant.now())
                .build();

        session.getMessages().add(userMsg);
        session.getMessages().add(botMsg);
        session.setEndedAt(Instant.now());

        try {
            chatSessionRepository.save(session);
        } catch (Exception ex) {
            log.warn("Skip persisting chat session: {}", ex.getMessage());
        }
    }

    private ChatSession findOrCreateSession(String sessionId, String userId) {
        try {
            Optional<ChatSession> existing = chatSessionRepository.findBySessionId(sessionId);
            if (existing.isPresent()) {
                ChatSession session = existing.get();
                if (session.getUserId() == null || session.getUserId().isBlank()) {
                    session.setUserId(userId);
                }
                if (session.getPreferenceProfile() == null) {
                    session.setPreferenceProfile(ChatSession.PreferenceProfile.empty());
                }
                if (session.getMessages() == null) {
                    session.setMessages(new ArrayList<>());
                }
                return session;
            }
        } catch (Exception ex) {
            log.warn("Use in-memory chat session: {}", ex.getMessage());
        }

        return ChatSession.builder()
                .sessionId(sessionId)
                .userId(userId)
                .startedAt(Instant.now())
                .endedAt(Instant.now())
                .messages(new ArrayList<>())
                .preferenceProfile(ChatSession.PreferenceProfile.empty())
                .build();
    }

    private void mergePreferences(ChatSession session, ChatRequest.UserPreferences preferences) {
        if (preferences == null) return;
        ChatSession.PreferenceProfile profile = session.getPreferenceProfile();
        if (profile == null) {
            profile = ChatSession.PreferenceProfile.empty();
            session.setPreferenceProfile(profile);
        }
        if (preferences.getTone() != null && !preferences.getTone().isBlank()) {
            profile.setPreferredTone(preferences.getTone());
        }
        if (preferences.getStyle() != null && !preferences.getStyle().isBlank()) {
            profile.setStyle(preferences.getStyle());
        }
        if (preferences.getBudget() != null && !preferences.getBudget().isBlank()) {
            profile.setBudget(preferences.getBudget());
        }
        if (preferences.getFocus() != null) {
            profile.getFocusTags().addAll(preferences.getFocus());
        }
    }

    private String resolveUserId(String userIdHeader, String sessionId) {
        if (userIdHeader != null && !userIdHeader.isBlank()) {
            return userIdHeader.trim();
        }
        return "guest-" + sessionId.substring(0, Math.min(12, sessionId.length()));
    }
}
