# Tổng Hợp Điểm Mạnh Nổi Bật Của Repo Và AI Chatbot

## Mục đích

File này tổng hợp các điểm mạnh nổi bật nhất của repo để phục vụ báo cáo/thuyết trình, dựa trên 3 nguồn chính:

- Code của repo `CNLTHD26K1_Nhom9`
- Báo cáo Word `BaocaoNhom9_(3).docx`
- Slide `Website Thương mại điện tử theo Kiến trúc Microservices & Agentic AI.pptx`

Trọng tâm của file là:

- 5 điểm tốt, nổi bật nhất của toàn repo
- Giải thích rõ vì sao các điểm đó tốt
- Mô tả luồng chạy cụ thể của Kong, Kafka/Saga, Jaeger và AI chatbot
- Đào sâu riêng phần AI chatbot: cách trả lời, luồng nghiệp vụ, ngoại lệ, session memory, guardrail, feedback loop

---

## 1. Điểm mạnh số 1: Kiến trúc microservices được tách theo domain rất rõ, đủ giống một hệ thống thực tế

### Vì sao đây là điểm mạnh

Repo này không dừng ở mức “chia service cho có”, mà tách theo đúng nghiệp vụ:

- `user-service`: xác thực, hồ sơ, email verification
- `product-service`: sản phẩm, category, wishlist, tồn kho
- `cart-service`: giỏ hàng Redis
- `order-service`: đơn hàng, thanh toán VNPay
- `promotion-service`: coupon, loyalty
- `review-service`: đánh giá MongoDB
- `chatbot-service`: trợ lý mua sắm AI

Điểm tốt là mỗi service sở hữu trách nhiệm riêng và database riêng. Điều này bám sát tinh thần trong báo cáo Word ở phần Service Decomposition và giúp nhóm chứng minh được tư duy kiến trúc chứ không chỉ code tính năng.

### Cái hay khi đi báo cáo

- Khi một nghiệp vụ thay đổi, không cần sửa cả khối lớn như monolith.
- Có thể scale riêng service có tải cao, ví dụ `product-service` hoặc `chatbot-service`.
- Từng service có thể test, deploy và quan sát độc lập.

### Luồng tổng thể

`Frontend -> Kong Gateway -> Microservice đúng domain -> Database đúng domain`

Ví dụ:

- đăng nhập đi vào `user-service`
- xem sản phẩm đi vào `product-service`
- đặt đơn đi vào `order-service`
- chatbot đi vào `chatbot-service` nhưng vẫn gọi dữ liệu thật từ các service còn lại

### Minh chứng trong repo

- `README.md`
- `docker/docker-compose.yml`
- `backend/pom.xml`
- `backend/services/*`
- Báo cáo Word, Chương 4 và Chương 5

### Câu chốt ngắn để nói

“Điểm mạnh đầu tiên của hệ thống là kiến trúc được tách theo domain rất rõ, nên vừa đúng tinh thần microservices, vừa đủ gần với cách doanh nghiệp triển khai thực tế.”

---

## 2. Điểm mạnh số 2: Kong Gateway được dùng đúng vai trò cửa vào duy nhất, gom routing, auth và bảo vệ hệ thống về một chỗ

### Vì sao đây là điểm mạnh

Kong ở repo này không chỉ là reverse proxy. Nó thực sự là API Gateway trung tâm:

- gom toàn bộ traffic vào một cổng `:8080`
- tách public route và protected route rõ ràng
- xử lý `CORS`
- có `rate-limiting`
- dùng custom plugin `jwt-auth` bằng Lua để xác thực JWT và bơm `X-User-Id`, `X-User-Email`, `X-User-Role` xuống service phía sau

Điểm mạnh ở đây là backend không phải service nào cũng tự decode JWT. Kong đã làm việc đó tập trung ở gateway. Như vậy service bên dưới nhẹ hơn, stateless hơn, và đúng vai trò hơn.

### Luồng Kong cụ thể

1. Frontend gọi API về `Kong Gateway`.
2. Kong đọc route trong `docker/kong.yml`.
3. Nếu là API public như xem sản phẩm hoặc callback VNPay public thì cho đi thẳng.
4. Nếu là API protected như wishlist, cart, orders, loyalty thì plugin `jwt-auth` chạy.
5. Plugin Lua:
   - lấy Bearer token
   - tách 3 phần JWT
   - kiểm tra chữ ký HS256/HS384 bằng shared secret
   - kiểm tra `exp`
   - trích `sub`, `userId`, `role`
   - inject thành header xuống service phía sau
6. Service phía sau chỉ cần đọc header người dùng thay vì tự parse JWT.

### Cái hay để nhấn mạnh

- Repo dùng Kong DB-less, nghĩa là config nằm trong file `kong.yml`, dễ review và dễ deploy.
- Có `docker-entrypoint-wrapper.sh` để inject `JWT_SECRET` lúc startup, giải quyết đúng nhược điểm DB-less là không interpolate env trực tiếp trong config.
- Public/private route chia rất rõ, giúp demo dễ và bảo mật dễ giải thích.

### Minh chứng trong repo

- `docker/kong.yml`
- `docker/kong/plugins/jwt-auth/handler.lua`
- `docker/kong/plugins/jwt-auth/schema.lua`
- `docker/kong/docker-entrypoint-wrapper.sh`
- `docker/docker-compose.yml`
- Báo cáo Word, phần API Gateway - Kong Gateway

### Câu chốt ngắn để nói

“Kong trong dự án này không chỉ định tuyến mà còn gánh luôn xác thực tập trung, giúp toàn bộ hệ thống sạch hơn, nhẹ hơn và đúng kiểu microservices hơn.”

---

## 3. Điểm mạnh số 3: Kafka + Saga xử lý luồng đặt hàng, thanh toán, giữ kho, hoàn kho và loyalty theo kiểu phân tán nhưng vẫn nhất quán

### Vì sao đây là điểm mạnh

Đây là phần rất đáng giá để báo cáo vì nó cho thấy nhóm hiểu bài toán giao dịch phân tán thật sự. Khi đơn hàng liên quan đồng thời đến:

- order
- inventory
- payment
- loyalty

thì không thể dùng 1 transaction DB duy nhất. Repo giải quyết bằng choreography-based saga qua Kafka.

### Luồng Saga đặt hàng - thanh toán - tồn kho

1. User tạo đơn ở `order-service`.
2. `order-service` lưu đơn với trạng thái `PENDING`.
3. `order-service` publish event `order.created.v1`.
4. `product-service` consume event này để kiểm tra và giữ kho.
5. Nếu đủ kho:
   - trừ số lượng
   - publish `inventory.reservation.result.v1` với `success=true`
6. Nếu thiếu kho:
   - publish `inventory.reservation.result.v1` với `success=false`
7. `order-service` consume kết quả giữ kho:
   - nếu thành công, đánh dấu `inventoryReserved=true`
   - nếu thất bại, hủy đơn
8. Khi VNPay callback về:
   - `order-service` verify chữ ký
   - publish `payment.result.v1`
9. `order-service` consume `payment.result.v1`:
   - nếu đã giữ kho rồi thì xác nhận đơn, payment `PAID`
   - nếu kho fail trước đó thì đánh dấu hủy và log `REFUND NEEDED`
10. Nếu thanh toán fail sau khi đã giữ kho:
   - `order-service` publish `order.cancelled.v1`
   - `product-service` consume để cộng kho lại
11. Khi đơn giao thành công:
   - `order-service` publish `order.delivered.v1`
   - `promotion-service` consume để cộng điểm loyalty
12. Nếu đơn bị hủy và đã dùng điểm:
   - `promotion-service` consume `order.cancelled.v1` để refund điểm

### Điểm hay nổi bật

- Có đủ cả forward flow lẫn compensating flow.
- Luồng lỗi được nghĩ tới rất thực tế, ví dụ “payment success nhưng inventory fail” thì không giả vờ thành công, mà giữ trạng thái để xử lý refund.
- Loyalty được nối vào saga chứ không hard-code chặt trong order-service.

### Các topic quan trọng

- `order.created.v1`
- `inventory.reservation.result.v1`
- `payment.result.v1`
- `order.cancelled.v1`
- `order.delivered.v1`

### Minh chứng trong repo

- `backend/common/src/main/java/com/fashion/common/event/SagaTopics.java`
- `backend/services/order-service/src/main/java/com/fashion/orderservice/service/impl/OrderServiceImpl.java`
- `backend/services/order-service/src/main/java/com/fashion/orderservice/service/impl/PaymentServiceImpl.java`
- `backend/services/order-service/src/main/java/com/fashion/orderservice/saga/OrderSagaConsumer.java`
- `backend/services/product-service/src/main/java/com/fashion/productservice/saga/InventorySagaConsumer.java`
- `backend/services/promotion-service/src/main/java/com/fashion/promotionservice/saga/LoyaltySagaConsumer.java`
- `saga_flow_explanation.md`
- Báo cáo Word, phần Asynchronous Communication và Saga Pattern

### Câu chốt ngắn để nói

“Điểm mạnh của repo là nhóm không né bài toán giao dịch phân tán, mà giải bằng Saga thật, có đủ luồng giữ kho, hủy đơn, hoàn kho và hoàn/ cộng điểm loyalty.”

---

## 4. Điểm mạnh số 4: Hệ thống observability và reliability đủ sâu để không chỉ chạy được, mà còn quan sát và debug được

### Vì sao đây là điểm mạnh

Nhiều đồ án có Kafka hoặc microservices nhưng thiếu phần quan sát. Repo này làm khá tròn:

- `Jaeger` để xem distributed trace
- `Kafka UI` để xem topic/message/consumer group
- `Prometheus` để scrape metrics
- `Grafana` để hiển thị dashboard
- `RetryableTopic` + `.retry` + `.dlt` cho các consumer saga

Nghĩa là khi demo, nhóm không chỉ nói “luồng có chạy”, mà còn chỉ ra nó chạy qua đâu, chậm ở đâu, và fail thì rơi vào đâu.

### Luồng Jaeger cụ thể nên kể

Với luồng đặt hàng:

1. Gọi HTTP vào `order-service`.
2. Trace ghi lại span HTTP ở `order-service`.
3. Khi `order-service` publish Kafka event, trace có span producer.
4. `product-service` consume event, trace có span consumer.
5. `product-service` publish kết quả giữ kho, trace tiếp tục nối.
6. `order-service` consume kết quả đó và cập nhật trạng thái.

Vì `order-service` và `product-service` đều bật Kafka observation và trỏ Zipkin endpoint về Jaeger nên có thể soi được đúng chuỗi HTTP -> Kafka producer -> Kafka consumer -> update state.

### Luồng Kafka UI cụ thể nên kể

Kafka UI giúp kiểm tra:

- topic nào đang có message
- payload event là gì
- consumer group nào đã đọc tới đâu
- có xuất hiện topic `.retry` hoặc `.dlt` hay không

Nói ngắn gọn:

- Jaeger trả lời câu hỏi “luồng chạy qua đâu”
- Kafka UI trả lời câu hỏi “event nào đã được bắn và consumer đã xử lý chưa”

### Điểm reliability đáng khen

- `OrderSagaConsumer`, `InventorySagaConsumer`, `LoyaltySagaConsumer` đều có `@RetryableTopic`
- có backoff, multiplier
- có DLT handler log rõ payload lỗi

Điều này rất có giá trị khi đi báo cáo vì nó cho thấy nhóm không làm event-driven theo kiểu happy path בלבד.

### Minh chứng trong repo

- `docker/docker-compose.yml`
- `backend/services/order-service/src/main/resources/application.yml`
- `backend/services/product-service/src/main/resources/application.yml`
- `docker/prometheus/prometheus.yml`
- `docker/grafana/provisioning/datasources/datasource.yml`
- `backend/services/order-service/src/main/java/com/fashion/orderservice/saga/OrderSagaConsumer.java`
- `backend/services/product-service/src/main/java/com/fashion/productservice/saga/InventorySagaConsumer.java`
- `backend/services/promotion-service/src/main/java/com/fashion/promotionservice/saga/LoyaltySagaConsumer.java`
- `README.md`

### Câu chốt ngắn để nói

“Dự án này mạnh ở chỗ không chỉ có luồng nghiệp vụ, mà còn có đủ công cụ để nhìn xuyên qua luồng đó bằng trace, metrics và event inspection.”

---

## 5. Điểm mạnh số 5: AI chatbot của repo này là một sales advisor có điều phối nghiệp vụ, có dữ liệu thật, có trí nhớ và có chống bịa

### Vì sao đây là điểm mạnh nhất về mặt khác biệt

Đây không phải chatbot kiểu “prompt hỏi gì trả lời nấy”. Phần AI trong repo được làm theo hướng agentic commerce:

- có orchestration layer
- có session memory
- có long-term preference
- có state machine theo giai đoạn bán hàng
- có tool calling qua dữ liệu thật
- có GraphRAG cho policy/FAQ/sales guidance
- có hậu kiểm anti-hallucination
- có fallback nhiều tầng khi agent lỗi
- có analytics/feedback loop để nối chatbot với hành vi click, checkout, order

Nói đơn giản: chatbot này không chỉ “biết nói”, mà “biết tư vấn theo flow mua hàng”.

### Luồng chatbot ở mức cao

1. Nhận message + `sessionId`.
2. Resolve user là guest hay logged-in.
3. Tạo hoặc load session từ MongoDB.
4. Merge preferences từ FE và selected product context từ card người dùng vừa click.
5. Enrich hồ sơ từ:
   - message hiện tại
   - lịch sử mua hàng
   - wishlist
   - user profile
6. Refresh conversation state và slot filling.
7. Chặn out-of-domain trước khi gọi LLM nếu câu hỏi lệch phạm vi.
8. Nếu là direct intent mạnh như wishlist, loyalty, size, product search thì đi thẳng vào flow nghiệp vụ tương ứng.
9. Nếu người dùng nói “mẫu này”, “áo này” thì bind vào selected product hoặc các suggestion gần nhất.
10. Nếu không đủ, agent mới chạy với context đã enrich.
11. Agent gọi tool thật để lấy sản phẩm, promotion, order, knowledge, review, loyalty.
12. Response cuối cùng đi qua guardrail để chặn bịa giá, bịa tên sản phẩm, bịa mã giảm, bịa policy.
13. Persist lại session, suggestions, promotions và profile để dùng cho turn sau.
14. Ghi analytics cho từng chat turn.

### Minh chứng trong repo

- `backend/services/chatbot-service/src/main/java/com/fashion/chatbotservice/service/impl/ChatbotServiceImpl.java`
- `backend/services/chatbot-service/src/main/java/com/fashion/chatbotservice/agent/FashionAgent.java`
- `backend/services/chatbot-service/src/main/java/com/fashion/chatbotservice/agent/FashionTools.java`
- `backend/services/chatbot-service/src/main/java/com/fashion/chatbotservice/agent/ResponseGuardrail.java`
- `backend/services/chatbot-service/src/main/java/com/fashion/chatbotservice/conversation/impl/ConversationStateServiceImpl.java`
- `backend/services/chatbot-service/src/main/java/com/fashion/chatbotservice/conversation/impl/SlotFillingServiceImpl.java`
- `backend/services/chatbot-service/src/main/java/com/fashion/chatbotservice/service/impl/ProfileEnrichmentServiceImpl.java`
- `backend/services/chatbot-service/src/main/java/com/fashion/chatbotservice/service/impl/KnowledgeBaseServiceImpl.java`
- `backend/services/chatbot-service/src/main/java/com/fashion/chatbotservice/service/impl/GraphRagServiceImpl.java`
- `backend/services/chatbot-service/src/main/java/com/fashion/chatbotservice/service/impl/ChatAnalyticsServiceImpl.java`
- `backend/services/chatbot-service/src/main/java/com/fashion/chatbotservice/service/impl/ChatFeedbackServiceImpl.java`
- `AIcode/chatbot-current-setup.md`
- `AIcode/nghiep-vu-tu-van-ban-hang-fashion-chatbot.md`
- Báo cáo Word, Chương 5

### Câu chốt ngắn để nói

“Điểm khác biệt lớn nhất của repo là chatbot không chỉ dùng LLM để trả lời, mà được đặt trong một workflow tư vấn bán hàng thật, có memory, có tool, có guardrail và có dữ liệu để tối ưu tiếp.”

---

## 6. Đào sâu riêng phần AI chatbot: các điểm nổi bật nhất để báo cáo

## 6.1. Chatbot trả lời theo pipeline nhiều lớp, không đẩy mọi thứ vào LLM

Đây là điểm rất đáng nói vì thể hiện tư duy kỹ thuật tốt.

Pipeline trong `ChatbotServiceImpl` đi theo thứ tự:

1. validate input
2. resolve/load session
3. enrich profile
4. refresh conversation state
5. chặn out-of-domain
6. xử lý discovery / clarification
7. xử lý multi-intent
8. xử lý direct intent
9. xử lý deictic reference
10. explicit lookup
11. agent path
12. final guardrail
13. persist + analytics

Điểm mạnh của cách làm này là:

- **Giảm số call LLM không cần thiết**
  - *Là gì:* Các bước validate, out-of-domain filter, direct intent được xử lý bằng Java logic thuần.
  - *Ở đâu:* `ChatbotServiceImpl.java` — các method `handleOutOfDomain()`, `handleDirectIntent()`, `handleDeicticReference()` chạy trước khi gọi agent.
  - *Làm gì:* Nếu câu hỏi lệch domain hoặc là intent mạnh đã biết (wishlist, loyalty, size), trả lời ngay mà không tốn token LLM.

- **Tăng tính ổn định**
  - *Là gì:* Pipeline có thứ tự cố định, mỗi bước có try-catch riêng và fallback rõ ràng.
  - *Ở đâu:* `ChatbotServiceImpl.java` — toàn bộ method `processMessage()` bọc trong cấu trúc try-catch nhiều tầng.
  - *Làm gì:* Khi một bước lỗi (ví dụ enrich profile fail), pipeline vẫn tiếp tục thay vì throw exception ra ngoài, giúp bot không bị sập khi dịch vụ phụ trợ không ổn định.

- **Bớt chi phí**
  - *Là gì:* Chỉ gọi LLM ở bước agent path, các bước còn lại dùng rule-based hoặc cache.
  - *Ở đâu:* `KnowledgeBaseServiceImpl.java` — có `@Cacheable(value = "knowledgeBase")` để cache kết quả search knowledge.
  - *Làm gì:* Câu hỏi policy/FAQ đã được hỏi trước đó sẽ lấy từ cache thay vì search lại MongoDB và tốn thêm token.

- **Giữ business flow quan trọng ở backend, không phó mặc cho model**
  - *Là gì:* Các nghiệp vụ quan trọng (wishlist chưa login → yêu cầu đăng nhập; size consulting → tính toán size theo số đo) được code cứng trong Java.
  - *Ở đâu:* `ChatbotServiceImpl.java` — `handleDirectIntent()`; `SizeAdvisorServiceImpl.java`; `SizeFitAdvisoryServiceImpl.java`.
  - *Làm gì:* Đảm bảo các flow quan trọng luôn chạy đúng, không phụ thuộc vào việc LLM có "hiểu" đúng prompt hay không.

---

## 6.2. Chatbot có “bộ não bán hàng”, không phải chỉ search sản phẩm

Chatbot có `SalesStage` — enum định nghĩa 7 giai đoạn hành trình bán hàng:

- **`DISCOVERY`** — *Ở đâu:* `SalesStage.java`. *Làm gì:* Giai đoạn bot đang tìm hiểu nhu cầu ban đầu, chưa có đủ thông tin để recommend. Bot ưu tiên hỏi dịp mặc, phong cách, ngân sách.
- **`STYLE_DISCOVERY`** — *Làm gì:* Bot đang đào sâu về phong cách (vibe, màu sắc, form dáng) sau khi đã biết loại sản phẩm cần mua.
- **`FILTERING`** — *Làm gì:* Đã có đủ thông tin cơ bản, bot đang thu hẹp danh sách sản phẩm theo filter (size, màu, giá).
- **`RECOMMENDING`** — *Làm gì:* Bot đang gợi ý sản phẩm cụ thể, kèm lý do phù hợp với profile user.
- **`COMPARING`** — *Làm gì:* User đang so sánh 2+ sản phẩm, bot hỗ trợ phân tích ưu nhược điểm từng mẫu.
- **`CLOSING`** — *Làm gì:* Bot nhận ra user sắp chốt đơn, chủ động nhắc promotion, loyalty hoặc CTA.
- **`AFTER_SALES`** — *Làm gì:* Sau khi đặt hàng, hỗ trợ tra cứu đơn, chính sách đổi trả.

`ConversationStateServiceImpl` dùng slot readiness để quyết định:

- **Còn thiếu thông tin gì**
  - *Là gì:* `RecommendationReadiness` — object đánh giá xem slot nào còn trống trong `StylingSlots`.
  - *Ở đâu:* `ConversationStateServiceImpl.java` — `checkReadiness()` kiểm tra các slot: gender, occasion, budget, size, style vibe.
  - *Làm gì:* Nếu slot quan trọng còn thiếu → trả về `ClarifyingQuestion` để bot hỏi thêm thay vì recommend ngay.

- **Nên hỏi tiếp hay đã nên recommend**
  - *Là gì:* `StageDecision` — object quyết định hành động tiếp theo dựa trên stage hiện tại và readiness score.
  - *Ở đâu:* `ConversationStateServiceImpl.java` — `decideNextStage()`.
  - *Làm gì:* Nếu readiness đạt ngưỡng → chuyển stage sang `RECOMMENDING` và trigger tool search. Nếu chưa → ở lại `FILTERING` và sinh clarifying question.

- **Đang ở bước so sánh hay chốt đơn**
  - *Là gì:* Logic detect intent so sánh ("mẫu nào tốt hơn", "khác nhau thế nào") và intent chốt ("mình lấy mẫu này", "thêm vào giỏ").
  - *Ở đâu:* `ChatbotServiceImpl.java` — phần xử lý stage `COMPARING` và `CLOSING`.
  - *Làm gì:* Ở COMPARING bot gọi tool so sánh; ở CLOSING bot nhắc promotion và loyalty tier còn áp dụng.

Điểm này rất tốt khi báo cáo vì nó chứng minh bot hiểu cuộc hội thoại như một hành trình bán hàng, không phải chuỗi câu hỏi rời rạc.

---

## 6.3. Slot filling của chatbot khá đúng bài toán tư vấn thời trang

`SlotFillingServiceImpl` đang thu thập các slot quan trọng:

- **`gender`** — Trích từ câu nói tự nhiên ("đồ nam", "cho con gái") bằng regex context-aware, tránh bắt sai khi user dùng "nam" không phải để chỉ giới tính. *Ở đâu:* `ProfileEnrichmentServiceImpl.java` — `extractTargetGender()`.
- **`occasion`** — Nhận diện dịp mặc: "đi làm" → `office`, "đi chơi" → `casual`, "dự tiệc" → `party`, "du lịch" → `travel`. *Ở đâu:* `ProfileEnrichmentServiceImpl.java` — `extractOccasionPreference()`.
- **`style vibe`** — Lưu vào `StylingSlots`. *Ở đâu:* `StylingSlots.java` — `SlotFillingServiceImpl.java` ghi nhận khi user nhắc đến "basic", "minimal", "streetwear", "elegant".
- **`product type`** — Phân loại áo, quần, váy, đầm từ tin nhắn. *Ở đâu:* `ProfileEnrichmentServiceImpl.java` — `extractCategoryPreference()` dùng keyword matching sau khi normalize tiếng Việt.
- **`budget`** — Parse ngân sách từ chuỗi tự nhiên ("200k", "dưới 1 triệu", "khoảng 500"). *Ở đâu:* `ProfileEnrichmentServiceImpl.java` — `parseBudget()` + `refreshPriceComfortZone()` phân loại thành `soft/mid/premium`.
- **`size`** — Trích size bằng regex (`size\s*[:=]?\s*(xs|s|m|l|xl|xxl|\d{2})`). *Ở đâu:* `ProfileEnrichmentServiceImpl.java` — `extractSizePreference()`.
- **`fit`** — Nhận diện oversize/slim/regular từ từ khóa "rộng", "thoải mái", "ôm", "slim fit". *Ở đâu:* `ProfileEnrichmentServiceImpl.java` — `extractFitPreference()`.
- **`color`** — Ghi nhận màu yêu thích (xanh, đen, trắng, đỏ, hồng). *Ở đâu:* `ProfileEnrichmentServiceImpl.java` — `extractColorPreference()`.
- **`height / weight`** — Thu thập từ câu nói khi user tự khai số đo để tư vấn size. *Ở đâu:* `SizeAdvisorServiceImpl.java` — `consultSize()` tính size phù hợp dựa trên BMI và bảng size chuẩn.

Đây là đúng bài toán fashion commerce, vì khách mua quần áo không chỉ hỏi "có gì đẹp", mà cần:

- **Mặc dịp gì** — slot `occasion` quyết định filter "office" hay "casual" khi search sản phẩm.
- **Dáng người ra sao** — slot `height/weight` → `SizeAdvisorServiceImpl` tính toán size phù hợp theo số đo thật.
- **Thích vibe nào** — slot `style vibe` → inject vào system context của agent để gợi ý đúng phong cách.
- **Ngân sách bao nhiêu** — slot `budget` → `priceComfortZone` (soft/mid/premium) giúp filter sản phẩm theo khoảng giá phù hợp.

Nghĩa là bot đã dịch được câu nói tự nhiên của user thành dữ liệu tư vấn có cấu trúc thông qua `ProfileEnrichmentServiceImpl` + `SlotFillingServiceImpl`, không phải chỉ dựa vào prompt.

---

## 6.4. Chatbot biết hỏi làm rõ khi user nói quá chung chung

Luồng `handleColdStart()` xử lý các câu như:

- **"mua áo"** — *Là gì:* Câu quá chung, không có đủ thông tin để search. *Làm gì:* Bot nhận diện đây là `DISCOVERY` stage, gọi `handleColdStart()` thay vì chạy agent ngay.
- **"mua quần"** — *Làm gì:* Tương tự, bot hỏi lại "quần gì — jeans, kaki, hay jogger?" để thu slot `product type` trước.
- **"tư vấn đồ"** — *Làm gì:* Trigger `handleDiscovery()` — bot hỏi tuần tự: dịp mặc → phong cách → ngân sách, trước khi gọi tool search.

Thay vì vội vàng recommend bừa, bot hỏi lại cụ thể:

- **Áo gì** — *Ở đâu:* `FashionAgent.java` — phần `COLD START / CÂU HỎI QUÁ CHUNG` trong system prompt hướng dẫn agent hỏi: "áo thun, sơ mi, polo hay áo khoác?".
- **Quần gì** — *Làm gì:* Giúp điền slot `product type` trước khi call tool `searchProducts` hoặc `listProductTypes`.
- **Váy gì** — *Làm gì:* Tương tự, thu hẹp product type để kết quả recommend chính xác hơn.

Điểm tốt:

- **Giảm sai ngay từ turn đầu** — *Là gì:* Không gọi tool search với query quá mơ hồ → tránh trả về sản phẩm không liên quan ngay lần đầu. *Ở đâu:* `ChatbotServiceImpl.java` — kiểm tra `SalesStage == DISCOVERY` trước khi execute agent.
- **Tạo cảm giác bot đang hiểu nhu cầu thật** — *Là gì:* Câu hỏi làm rõ được sinh từ `ClarifyingQuestion` object dựa trên slot nào còn thiếu, không phải hỏi đại. *Ở đâu:* `ConversationStateServiceImpl.java` — `generateClarifyingQuestion()`.
- **Rất hợp với nghiệp vụ tư vấn bán hàng** — *Là gì:* Đây là kỹ thuật consultative selling — hiểu nhu cầu trước khi giới thiệu sản phẩm, đúng quy trình của nhân viên bán hàng chuyên nghiệp.

Đây là một điểm dễ ghi điểm với giảng viên vì nó cho thấy nhóm không để AI trả lời lan man — bot bị ràng buộc bởi pipeline và system prompt không được tự suy diễn khi thiếu context.

---

## 6.5. Chatbot có memory theo session và còn có long-term memory cho user quay lại

Phần memory của chatbot mạnh ở 2 tầng:

### Tầng 1: session memory

- **Chat history lưu trong MongoDB `chat_sessions`**
  - *Là gì:* Collection `chat_sessions` trong MongoDB, mỗi document là một `ChatSession` gắn với `sessionId` duy nhất.
  - *Ở đâu:* `ChatSession.java` (model) + `ChatSessionRepository.java` (Spring Data MongoDB repo).
  - *Làm gì:* Lưu toàn bộ lịch sử câu hỏi/trả lời dưới dạng `List<ChatMessage>` để khôi phục memory khi session được mở lại.

- **Mỗi turn lưu đủ 4 thập**
  - *`user message`* — Nội dung câu hỏi gốc của user.
  - *`bot reply`* — Nội dung trả lời của bot đã qua guardrail.
  - *`product suggestions snapshot`* — Danh sách sản phẩm được gợi ý trong turn đó, dùng để bind khi user nói "mẫu này" ở turn sau.
  - *`promotion snapshot`* — Promotion đang áp dụng trong turn đó, giúp bot không phải fetch lại nếu user hỏi tiếp.
  - *Ở đâu:* `ChatbotServiceImpl.java` — method `persistSession()` ghi sau mỗi turn.

- **`AgentConfig` hydrate lại `MessageWindowChatMemory` từ lịch sử cũ**
  - *Là gì:* `MessageWindowChatMemory` là class của LangChain4j, giữ toàn bộ context chat theo cử a sổ trượt (tối đa 1000 messages theo config).
  - *Ở đâu:* `AgentConfig.java` — method `hydrateMemory()` được gọi khi `chatMemoryProvider` tạo memory mới cho một session.
  - *Làm gì:* Đọc `ChatSession.getMessages()` từ MongoDB, nạp vào memory theo thứ tự user/AI mỗi cặp — đảm bảo khi user mở lại widget, bot "nhớ" cuộc hội thoại trước mà không cần user nhắc lại.

### Tầng 2: long-term preference

- **Profile được persist async sau mỗi turn**
  - *Là gì:* `UserPreferenceDocument` — MongoDB document lưu profile sở thích lâu dài theo `userId`, tách riêng khỏi session.
  - *Ở đâu:* `ProfileEnrichmentServiceImpl.java` — `persistProfileAsync()` dùng `CompletableFuture.runAsync()` để không block main thread.
  - *Làm gì:* Sau mỗi turn, ghi `PreferenceProfile` (size, màu, category, persona, budget zone…) vào MongoDB. Không chặn luồng trả lời để không tăng latency.

- **Khi user quay lại, session mới bootstrap từ profile đã lưu**
  - *Là gì:* `loadPersistedProfile()` — load profile cũ từ `UserPreferenceRepository` và merge vào session mới.
  - *Ở đâu:* `ProfileEnrichmentServiceImpl.java` — được gọi ngay ở đầu `processMessage()` khi tạo session mới.
  - *Làm gì:* User lần trước chọn size M, thích đen, mặc casual — lần này mở chat mới, bot đã biết điều đó ngay từ turn đầu và ưu tiên gợi ý phù hợp.

Kết quả là bot không bị “mất trí nhớ hoàn toàn” sau mỗi câu hay mỗi lần mở lại widget.

---

## 6.6. Chatbot biết tận dụng dữ liệu người dùng thật để cá nhân hóa

`ProfileEnrichmentServiceImpl` không chỉ đọc tin nhắn hiện tại, mà còn enrich từ 3 nguồn thật:

- **Purchase history (lịch sử mua hàng)**
  - *Là gì:* `enrichFromPurchaseHistory()` — gọi REST API `GET /api/v1/orders?page=0&size=20` với header `X-User-Id`.
  - *Ở đâu:* `ProfileEnrichmentServiceImpl.java` — `enrichFromPurchaseHistory()` dùng `WebClient` để gọi `order-service`.
  - *Làm gì:* Phân tích 20 đơn gần nhất, trích xuất size và màu của từng item đã mua, bổ sung vào `preferredSizes` và `preferredColors` của profile. Chỉ fetch nếu profile còn trống, tránh gọi HTTP mỗi message.

- **Wishlist**
  - *Là gì:* `enrichFromWishlist()` — gọi REST API `GET /api/v1/wishlists?page=0&size=20` với header `X-User-Id`.
  - *Ở đâu:* `ProfileEnrichmentServiceImpl.java` — `enrichFromWishlist()` dùng `WebClient` để gọi `product-service`.
  - *Làm gì:* Với từng sản phẩm trong wishlist, trích xuất `categoryName`, `colorName`, `sizeName` từ variants để enrich `preferredCategories`, `preferredColors`, `preferredSizes`.

- **User profile**
  - *Là gì:* `enrichFromUserProfile()` — gọi REST API `GET /api/v1/users/me` để lấy hồ sơ cơ bản.
  - *Ở đâu:* `ProfileEnrichmentServiceImpl.java` — `enrichFromUserProfile()` dùng `WebClient` gọi `user-service`.
  - *Làm gì:* Lấy `gender` từ field trong profile để điền `targetGender` nếu user chưa đề cập trong chat. Lấy tên để set preferred tone.

Ví dụ bot có thể biết:

- **Size user hay mua** — *Ở đâu:* Từ `enrichFromPurchaseHistory()` — lấy size từng item trong order, ưu tiên gợi ý sản phẩm còn size đó.
- **Màu user hay chọn** — *Ở đâu:* Từ cả purchase history và wishlist — bot gợi ý sản phẩm màu quen trước.
- **Category user quan tâm** — *Ở đâu:* Từ wishlist `categoryName` — filter sản phẩm theo category ưu tiên.
- **Target gender** — *Ở đâu:* Từ user profile `gender` field — không cần user khai lại giới tính mỗi session.

Điểm hay ở đây là cá nhân hóa không chỉ đến từ prompt, mà đến từ dữ liệu thật của hệ thống.

---

## 6.7. Chatbot có direct business flow cho các intent quan trọng

Đây là một trong những điểm tốt nhất của phần AI.

Thay vì đẩy toàn bộ intent vào agent, repo tách một số intent mạnh ra flow riêng:

- **Wishlist recommendation**
  - *Là gì:* Flow xử lý khi user hỏi về wishlist của mình ("đồ đã lưu", "wishlist của tôi").
  - *Ở đâu:* `ChatbotServiceImpl.java` — `handleDirectIntent()` — nếu intent là `wishlist_recommendation` và user chưa đăng nhập → trả về thông báo yêu cầu login ngay không gọi agent.
  - *Làm gì:* Nếu đã login, gọi `ProductQueryHandlerImpl` để lấy wishlist thật và render card sản phẩm trực tiếp, không phải đưa qua LLM.

- **Loyalty benefit**
  - *Là gì:* Flow kiểm tra điểm tích lũy, loyalty tier và ưu đãi thành viên.
  - *Ở đâu:* `ChatbotServiceImpl.java` — `handleDirectIntent()` — nếu intent là `loyalty_benefit` và guest → trả về yêu cầu đăng nhập.
  - *Làm gì:* Nếu đã login, gọi `FashionTools.getLoyaltyBenefits()` → lấy điểm thật từ `promotion-service` rồi format kết quả mà không cần LLM tự tạo số.

- **Size consulting**
  - *Là gì:* Flow tư vấn size dựa trên số đo chiều cao/cân nặng/vòng ngực.
  - *Ở đâu:* `SizeAdvisorServiceImpl.java` — `consultSize()` có bảng size chuẩn cho áo, quần, váy; `SizeFitAdvisoryServiceImpl.java` tư vấn chọn fit phù hợp dáng người.
  - *Làm gì:* Nếu user cung cấp số đo rõ → gọi thẳng `buildSizeConsultationResponse()`, trả kết quả deterministic, không cần LLM suy đoán.

- **Direct product search**
  - *Là gì:* Flow search sản phẩm theo tên cụ thể bằng strict match trước khi dùng fuzzy search.
  - *Ở đâu:* `ProductQueryHandlerImpl.java` — `searchProductsStrict()` tìm exact/partial match theo tên sản phẩm.
  - *Làm gì:* Tránh LLM “pần mình” tìm sản phẩm không tồn tại khi user đã nói rõ tên; nếu strict search ra kết quả → trả về ngay.

Lợi ích:

- **Nhanh hơn** — Không cần qua LLM và tool calling round-trip. Response có thể về trong < 200ms thay vì chờ model.
- **Ít lỗi hơn** — LLM có thể mắc lỗi intent routing; code Java thì deterministic.
- **Deterministic hơn** — Cùng một câu hỏi wishlist/loyalty luôn ra cùng kết quả thật từ database, không thay đổi theo temperature.
- **Đúng nghiệp vụ hơn** — "Guest hỏi wishlist → yêu cầu đăng nhập" là nghiệp vụ cứng, phải luôn đúng, không nên để LLM tự quyết định.

Đây là tư duy rất tốt: dùng AI ở nơi cần linh hoạt, nhưng giữ logic nghiệp vụ quan trọng ở code.

---

## 6.8. Chatbot xử lý được câu tham chiếu kiểu “mẫu này”, “áo này”

`selectedProductContext` là một điểm rất sáng.

Luồng:

1. Frontend gửi `selectedProductContext` khi user click vào card sản phẩm chatbot gợi ý.
2. Backend lưu snapshot sản phẩm đó vào profile session.
3. Nếu user nói:
   - “mẫu này có size M không?”
   - “áo này có khuyến mãi không?”
   - “mẫu này review sao?”
4. Bot không phải đoán từ text, mà bind trực tiếp vào đúng sản phẩm user vừa chọn.

Điểm này làm trải nghiệm bot giống trợ lý thật hơn rất nhiều, vì bot hiểu “this product” theo context UI chứ không phải suy luận mơ hồ.

---

## 6.9. Chatbot xử lý được multi-intent follow-up trên cùng một sản phẩm

`MultiIntentResolverImpl` cho phép gom nhiều nhu cầu follow-up như:

- **Hỏi chi tiết** — *Là gì:* Intent `product_detail`. *Ở đâu:* `MultiIntentResolverImpl.java` — detect keyword "chi tiết", "thông tin", "mô tả". *Làm gì:* Gọi `FashionTools.getProductDetails()` lấy mô tả, chất liệu, thông số từ `product-service`.
- **Hỏi review** — *Là gì:* Intent `product_review`. *Ở đâu:* `MultiIntentResolverImpl.java` — detect "review", "đánh giá". *Làm gì:* Gọi `FashionTools.getProductReviews()` lấy rating và comment thật từ `review-service` (MongoDB).
- **Hỏi khuyến mãi** — *Là gì:* Intent `promotion_check`. *Ở đâu:* `MultiIntentResolverImpl.java` — detect "khuyến mãi", "giảm giá", "coupon". *Làm gì:* Gọi `FashionTools.getApplicablePromotions()` kiểm tra promotion còn hiệu lực.

Kết quả được tổng hợp bởi `ResponseAssembler.java` thành 1 reply gộp — user không cần gửi 3 câu riêng lẻ mà vẫn nhận đủ thông tin.

Điểm này rất hợp demo vì cho thấy bot làm "người bán hàng" tốt hơn, không chỉ là router intent cơ bản.

---

## 6.10. Chatbot dùng dữ liệu thật thông qua tool calling, không trả lời tưởng tượng

`FashionAgent` ép agent phải gọi tool trước khi trả lời về:

- sản phẩm
- giá
- tồn kho
- size
- đơn hàng
- review
- promotion
- loyalty
- policy / FAQ

`FashionTools` là nơi agent truy xuất sang các service thật.

Điểm mạnh:

- câu trả lời gắn với dữ liệu hệ thống
- frontend nhận luôn structured suggestions để render card
- tránh kiểu bot “trả lời nghe hay nhưng không đúng catalog”

Đây là điểm rất đáng nói khi bảo vệ đề tài Agentic AI.

---

## 6.11. Chatbot có GraphRAG và knowledge ingestion riêng cho policy/FAQ/sales guidance

Repo không chỉ có search text đơn giản.

`KnowledgeIngestionServiceImpl`:

- đọc markdown trong `resources/knowledge`
- chunk theo heading
- lưu MongoDB
- rebuild graph khi startup

`GraphRagServiceImpl`:

- tạo node loại `TOPIC`, `KEYWORD`, `CHUNK`
- nối quan hệ `TOPIC_HAS_CHUNK`, `KEYWORD_MENTIONS_CHUNK`, `ADJACENT`, `RELATED`
- retrieve theo seed keyword/topic rồi expand theo adjacency và related edges

`KnowledgeBaseServiceImpl`:

- trộn lexical score với graph score

Điểm mạnh:

- bot trả lời policy/FAQ có nguồn hơn
- sales guidance cũng không chỉ dựa vào prompt
- retrieval có ngữ cảnh tốt hơn search từ khóa thuần

---

## 6.12. Chatbot có guardrail hậu kiểm chống hallucination rất thực dụng

`ResponseGuardrail` là một highlight lớn của codebase.

Nó kiểm:

- giá trong câu trả lời có khớp data không
- tên sản phẩm có nằm trong tool result không
- mã giảm giá có thật không
- claim còn hàng có grounded không
- policy có grounded không
- nếu tool fail thì ép reply sang hướng mềm, an toàn

Điểm rất tốt là guardrail nằm cuối flow, nên:

- direct intent
- heuristic fallback
- deictic lookup
- agent path

đều bị hậu kiểm giống nhau.

Đây là thiết kế chững chạc hơn kiểu “chỉ viết prompt bảo model đừng bịa”.

---

## 6.13. Chatbot có luồng ngoại lệ và fallback khá đầy đủ

Các nhánh ngoại lệ đáng khen:

- hỏi ngoài domain thì chặn trước khi gọi LLM
- guest hỏi wishlist/loyalty/order thì yêu cầu login
- thiếu số đo thì hỏi lại field còn thiếu
- không rõ đang nói sản phẩm nào thì yêu cầu user nói rõ tên
- strict search không thấy thì hỏi có muốn tìm mẫu tương tự không
- agent lỗi thì retry
- nếu lỗi liên quan corrupt tool-call memory thì clear session memory rồi retry lại
- agent vẫn fail thì rơi về heuristic fallback
- tool fail thì response bị softening để tránh nói chắc sai

Điểm này cho thấy nhóm làm AI theo hướng production-thinking, không chỉ làm happy path demo.

---

## 6.14. Chatbot có thêm cart context và profile context trước khi gọi agent

Trước khi gọi LLM, `executeAgent()` còn inject:

- clarification context
- conversation state
- profile context
- active cart context

Nghĩa là agent không nhìn mỗi câu user vừa nhập, mà nhìn cả trạng thái mua sắm hiện tại.

Ví dụ nếu user đang có một áo trong giỏ, bot có thể tư vấn thêm món phối kèm theo logic hơn.

Đây là một điểm rất đáng kể vì nó kéo AI từ “Q&A bot” sang “shopping assistant”.

---

## 6.15. Chatbot có analytics và feedback loop nối đến hành vi thật của người dùng

Đây là điểm rất đáng báo cáo vì nhiều chatbot demo thiếu phần đo hiệu quả.

Repo đang làm được:

- log từng chat turn vào `ChatAnalyticsDocument`
- lưu:
  - intent
  - confidence
  - sources tri thức
  - số suggestion
  - guardrail violation
  - latency
- frontend gửi thêm feedback event khi:
  - click product từ chatbot
  - submit checkout
  - order success
  - VNPay return

Nghĩa là chatbot bắt đầu được nối với conversion funnel thật, không chỉ dừng ở “trả lời hay”.

### Ý nghĩa khi báo cáo

Bạn có thể nói:

“Nhóm không xem chatbot là một hộp chat tách biệt, mà đã bắt đầu nối nó với hành vi mua hàng thật để sau này đo được bot có góp phần vào checkout và order hay không.”

---

## 6.16. Chatbot còn có lớp quality scoring và rate limiting, cho thấy tư duy vận hành dài hạn

Ngoài trả lời được, repo còn có:

- `ResponseQualityScorerImpl` để chấm chất lượng response
- `RateLimitConfig` để giới hạn global và per-user/per-session
- `ResilienceConfig` để áp circuit breaker, retry, timeout, bulkhead cho downstream calls

Điểm này tốt vì nó cho thấy chatbot không được xem như một module “AI thử nghiệm”, mà đã được đặt trong mindset vận hành thật.

---

## 7. Gợi ý 1 đoạn nói ngắn khi thuyết trình

“Điểm em đánh giá nổi bật nhất của repo là nhóm không chỉ xây một website thương mại điện tử theo microservices, mà còn ghép được 4 lớp rất thực tế với nhau: gateway bằng Kong, giao dịch phân tán bằng Kafka Saga, quan sát hệ thống bằng Jaeger/Prometheus/Grafana, và đặc biệt là một AI chatbot theo hướng agentic có memory, tool calling, GraphRAG và anti-hallucination. Vì vậy hệ thống này không chỉ chạy được tính năng, mà còn thể hiện khá rõ tư duy thiết kế hệ thống hiện đại.”

---

## 8. Kết luận ngắn

Nếu phải chọn 5 điểm mạnh nổi bật nhất để đi báo cáo, mình chốt theo thứ tự này:

1. Kiến trúc microservices tách domain rõ, database riêng, rất sát thực tế.
2. Kong Gateway làm đúng vai trò cửa vào duy nhất với auth tập trung và routing rõ ràng.
3. Kafka + Saga xử lý tốt luồng đặt hàng, thanh toán, tồn kho, loyalty và bù trừ.
4. Observability và reliability tốt: Jaeger, Kafka UI, Prometheus, Grafana, retry và DLT.
5. AI chatbot là điểm khác biệt lớn nhất: có orchestration, session memory, business flow, GraphRAG, tool calling, fallback và anti-hallucination.

Riêng phần chatbot, điểm đáng khen nhất là bot đã được thiết kế như một trợ lý bán hàng có trạng thái và có dữ liệu thật, chứ không phải chỉ là lớp giao diện gọi LLM.
