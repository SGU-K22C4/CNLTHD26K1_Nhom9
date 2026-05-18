# Code Review: Fashion AI Chatbot Service (DeepSeek + LangChain4j)

> **Stack**: Spring Boot 3 · LangChain4j 0.36.2 · MongoDB · Resilience4j · DeepSeek via OpenAI-compatible endpoint  
> **Mục đích**: AI Sales Advisor tư vấn bán thời trang (dữ liệu Zara VN)

---

## 1. ĐIỂM MẠNH — Những gì đã làm tốt

### Architecture
- **Layered Agent Design** rõ ràng: `FashionAgent` (LLM interface) → `FashionTools` (tool calling) → `ToolResultCollector` → `ResponseGuardrail` → `ResponseAssembler`. Tách biệt trách nhiệm tốt.
- **Heuristic fallback** (`use-agent: false`) cho phép service vẫn hoạt động khi LLM down — thiết kế phòng thủ tốt.
- **FallbackChatLanguageModel** chain nhiều model (primary → fallbacks) xử lý lỗi credit/timeout — production-ready.
- **Conversation flow** phân tầng: `ProductDiscoveryFlow`, `SizeConsultationFlow`, `LoyaltyReviewFlow`, `WishlistReviewFlow` — design pattern Strategy dùng đúng chỗ.

### Reliability
- **Resilience4j** đầy đủ: Circuit Breaker + Retry + Bulkhead + TimeLimiter + RateLimiter cho cả global lẫn per-user.
- **GraphRAG** kết hợp vector search + knowledge graph — approach tiên tiến cho knowledge base.
- **Caffeine cache** 2 tầng (productSearch 5 phút, knowledgeBase 15 phút) — đúng hướng giảm LLM calls.

### AI/Agent Quality
- **System prompt** viết rất kỹ: intent routing, tool priority order, sales technique, personalization, response format — một trong những system prompt tốt nhất cho Vietnamese e-commerce.
- **ResponseGuardrail** là highlight của codebase: chủ động block hallucinated prices, promotion codes, product names, stock status, policy claims. Đây là lớp bảo vệ rất quan trọng với chatbot bán hàng.
- **SlotFillingService** + **ConversationStateService** quản lý trạng thái hội thoại đa lượt tốt.
- **VietnameseNormalizer** xử lý tiếng Việt không dấu — cần thiết cho thị trường VN.
- **A/B experiment framework** (`PromptExperimentService`) để test system prompt variants — tư duy data-driven.

### Knowledge Base
- 16 file knowledge phân loại tốt: sales playbook, objection handling, size rules, style guide, seasonal rules, persona, business priority — phủ đủ use case tư vấn.

---

## 2. VẤN ĐỀ NGHIÊM TRỌNG — Cần fix ngay

### 🔴 Security: CORS hoàn toàn tắt
```java
// SecurityConfig.java
.cors(AbstractHttpConfigurer::disable)
```
**Vấn đề**: Bất kỳ domain nào cũng có thể gọi API từ browser. Nếu chatbot deploy public, đây là lỗ hổng nghiêm trọng.  
**Fix**: Cấu hình `CorsConfigurationSource` chỉ cho phép domain frontend của bạn.

### 🔴 Security: `allowDirectUserHeader = true` theo mặc định
```java
// ChatbotController.java
@Value("${chatbot.security.allow-direct-user-header:true}")
private boolean allowDirectUserHeader;
```
Khi `true`, bất kỳ client nào cũng có thể tự set `X-User-Id` để giả mạo identity — bypass toàn bộ user context. Đây là thiết kế nguy hiểm cho production.  
**Fix**: Default phải là `false`. Chỉ `true` trong môi trường local dev.

### 🔴 Memory leak: `chatMemoryStore` không có eviction
```java
// AgentConfig.java
private final Map<String, MessageWindowChatMemory> chatMemoryStore = new ConcurrentHashMap<>();
```
Map này grow vô hạn theo số sessions. Sau vài ngày traffic, service sẽ OOM.  
**Fix**: Dùng Caffeine cache với TTL + max size thay vì `ConcurrentHashMap` thuần.

### 🔴 `AI_API_KEY` không có default
```yaml
# application.yml
ai:
  api-key: ${AI_API_KEY}
```
Thiếu default value → service crash khi start nếu env var chưa set, không có error message rõ ràng.  
**Fix**: Thêm `@Value("${ai.api-key}")` với `@PostConstruct` validation rõ ràng, hoặc dùng Spring Boot validation `@NotBlank`.

### 🔴 `maxTokens` bị override từ config nhưng không nhất quán
```java
// AgentConfig.java: đọc từ config
@Value("${ai.max-tokens:1000}")
private int maxTokens;
```
```yaml
# application.yml: config là 4096
ai:
  max-tokens: ${AI_MAX_TOKENS:4096}
```
Code Java default là `1000` nhưng YAML default là `4096` — conflict. Response có thể bị cắt với complex queries.

---

## 3. VẤN ĐỀ TRUNG BÌNH — Nên cải thiện

### 🟡 Per-user rate limiter không có eviction
```java
// RateLimitConfig.java
private final ConcurrentMap<String, RateLimiter> userRateLimiters = new ConcurrentHashMap<>();
```
Tương tự chatMemoryStore — accumulate vô hạn. Với guest users (null userId), rate limiting bị bỏ qua hoàn toàn.  
**Fix**: Evict limiter sau 1 giờ không dùng; áp rate limit cho IP thay vì userId với guest.

### 🟡 `FallbackChatLanguageModel` được định nghĩa nhưng không được dùng
`AgentConfig.chatLanguageModel()` return `OpenAiChatModel` trực tiếp, không wrap vào `FallbackChatLanguageModel`. Class fallback tồn tại nhưng dead code.

### 🟡 Log requests/responses LLM trong production
```yaml
ai:
  log-requests: ${AI_LOG_REQUESTS:true}
  log-responses: ${AI_LOG_RESPONSES:true}
```
Default `true` trong production có thể log toàn bộ conversation của user — vi phạm privacy. Nên default `false`, chỉ bật khi debug.

### 🟡 `ResponseGuardrail.validateProductReferences()` dễ false positive
Regex `QUOTED_PRODUCT_PATTERN` chỉ match text trong dấu ngoặc kép. LLM thường mention tên sản phẩm không có dấu ngoặc → guard không phát hiện được hallucinated product names trong nhiều trường hợp.

### 🟡 `SemanticIntentRouter` dùng hardcoded weights thay vì ML
Threshold `0.82` và weights (prime keyword `2.0`, others `1.0`) hardcode — không dễ tune. Cần benchmark để chứng minh con số này đúng trước khi dùng production.

### 🟡 `ChatbotServiceImpl.java` quá lớn (143KB)
File này vi phạm Single Responsibility nặng. Cần refactor extract thành các service nhỏ hơn để maintainability tốt hơn.

### 🟡 CacheConfig không apply đúng per-cache TTL
```java
// CacheConfig.java
manager.setCaffeine(
    Caffeine.newBuilder()
        .expireAfterWrite(5, TimeUnit.MINUTES) // ← áp dụng cho TẤT CẢ cache
        ...
);
manager.setCacheNames(List.of("productSearch", "knowledgeBase", "intentEmbeddings"));
```
`knowledgeBase` được khai báo cần 15 phút nhưng thực ra TTL chỉ 5 phút (bị override). Cần dùng `CaffeineCacheManager` per-name hoặc custom config per cache.

---

## 4. VẤN ĐỀ NHỎ — Nice to fix

### 🟢 `PromptExperimentService` chưa tích hợp vào `AgentConfig`
`getPromptKey()` trả về key nhưng không có `PromptTemplateLoader` để load file tương ứng. A/B test framework hiện chỉ log, không thực sự test variant prompts.

### 🟢 Knowledge base thiếu versioning
Files `.md` trong `classpath:knowledge` không có versioning. Khi update content, không có cách roll back hoặc audit trail.

### 🟢 `ChatbotController.getSession()` không có authorization
Bất kỳ ai cũng có thể `GET /api/v1/chatbot/sessions/{sessionId}` nếu đoán được sessionId — data leak potential.

### 🟢 `VietnameseNormalizer` chưa xử lý một số edge cases
Không normalize `đ/Đ` → `d`, một số ký tự đặc biệt có thể gây lỗi so sánh trong `SemanticIntentRouter`.

---

## 5. NHẬN XÉT VỀ CHỌN DEEPSEEK

**Ưu điểm** của setup hiện tại (DeepSeek via `chiasegpu.vn`):
- Chi phí thấp hơn GPT-4 đáng kể
- DeepSeek-3 có tool calling tương đối tốt
- Endpoint OpenAI-compatible → dễ swap sang model khác

**Rủi ro cần lưu ý**:
- `chiasegpu.vn` là third-party provider, không phải DeepSeek official → latency có thể không ổn định, SLA không rõ ràng
- Timeout 180s (`AI_TIMEOUT_SECONDS:180`) rất cao → user sẽ chờ lâu nếu model slow; nên set 30–60s và handle gracefully
- DeepSeek's tool calling kém hơn GPT-4o trong complex multi-tool scenarios — cần test kỹ với concurrent tool calls

---

## 6. TÓM TẮT ĐÁNH GIÁ

| Hạng mục | Điểm | Nhận xét |
|---|---|---|
| Architecture | 8/10 | Layered design tốt, có fallback path |
| Security | 4/10 | CORS off, allowDirectUserHeader insecure |
| AI/Prompt Quality | 9/10 | System prompt và guardrail rất kỹ |
| Reliability | 7/10 | Resilience4j tốt nhưng memory leak |
| Code Quality | 6/10 | ChatbotServiceImpl quá lớn, dead code |
| Observability | 7/10 | Prometheus + MDC trace, thiếu structured error |

**Kết luận**: Đây là codebase chất lượng tốt hơn mức trung bình, thể hiện hiểu biết sâu về AI agent design (guardrail, slot filling, conversation state). Tuy nhiên cần fix 4 vấn đề nghiêm trọng (CORS, user identity spoofing, memory leak, API key handling) trước khi deploy production.

---

## 7. ƯU TIÊN FIX

1. `allowDirectUserHeader` → default `false`
2. CORS config → whitelist domain cụ thể  
3. `chatMemoryStore` → Caffeine cache với TTL
4. `per-user RateLimiter` → thêm eviction
5. `FallbackChatLanguageModel` → wire vào AgentConfig
6. `CacheConfig` → fix per-cache TTL
7. Log requests/responses → default `false` in prod
8. `ChatbotServiceImpl` → refactor chia nhỏ