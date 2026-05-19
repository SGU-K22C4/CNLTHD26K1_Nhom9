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

- giảm số call LLM không cần thiết
- tăng tính ổn định
- bớt chi phí
- giữ được các business flow quan trọng ở backend, không phó mặc cho model

---

## 6.2. Chatbot có “bộ não bán hàng”, không phải chỉ search sản phẩm

Chatbot có `SalesStage`:

- `DISCOVERY`
- `FILTERING`
- `RECOMMENDING`
- `COMPARING`
- `CLOSING`

`ConversationStateServiceImpl` dùng slot readiness để quyết định:

- còn thiếu thông tin gì
- nên hỏi tiếp hay đã nên recommend
- đang ở bước so sánh hay chốt đơn

Điểm này rất tốt khi báo cáo vì nó chứng minh bot hiểu cuộc hội thoại như một hành trình bán hàng, không phải chuỗi câu hỏi rời rạc.

---

## 6.3. Slot filling của chatbot khá đúng bài toán tư vấn thời trang

`SlotFillingServiceImpl` đang thu thập các slot quan trọng:

- gender
- occasion
- style vibe
- product type
- budget
- size
- fit
- color
- height / weight

Đây là đúng bài toán fashion commerce, vì khách mua quần áo không chỉ hỏi “có gì đẹp”, mà cần:

- mặc dịp gì
- dáng người ra sao
- thích vibe nào
- ngân sách bao nhiêu

Nghĩa là bot đã dịch được câu nói tự nhiên của user thành dữ liệu tư vấn có cấu trúc.

---

## 6.4. Chatbot biết hỏi làm rõ khi user nói quá chung chung

Luồng `handleColdStart()` xử lý các câu như:

- “mua áo”
- “mua quần”
- “tư vấn đồ”

Thay vì vội vàng recommend bừa, bot hỏi lại cụ thể:

- áo gì
- quần gì
- váy gì

Điểm tốt:

- giảm sai ngay từ turn đầu
- tạo cảm giác bot đang hiểu nhu cầu thật
- rất hợp với nghiệp vụ tư vấn bán hàng

Đây là một điểm dễ ghi điểm với giảng viên vì nó cho thấy nhóm không để AI trả lời lan man.

---

## 6.5. Chatbot có memory theo session và còn có long-term memory cho user quay lại

Phần memory của chatbot mạnh ở 2 tầng:

### Tầng 1: session memory

- chat history được lưu trong MongoDB `chat_sessions`
- mỗi turn lưu cả:
  - user message
  - bot reply
  - product suggestions snapshot
  - promotion snapshot
- `AgentConfig` hydrate lại `MessageWindowChatMemory` từ lịch sử cũ khi session được mở lại

### Tầng 2: long-term preference

- profile được persist async sau mỗi turn
- khi user quay lại, session mới có thể bootstrap từ profile đã lưu

Kết quả là bot không bị “mất trí nhớ hoàn toàn” sau mỗi câu hay mỗi lần mở lại widget.

---

## 6.6. Chatbot biết tận dụng dữ liệu người dùng thật để cá nhân hóa

`ProfileEnrichmentServiceImpl` không chỉ đọc tin nhắn hiện tại, mà còn enrich từ:

- purchase history
- wishlist
- user profile

Ví dụ bot có thể biết:

- size user hay mua
- màu user hay chọn
- category user quan tâm
- target gender

Điểm hay ở đây là cá nhân hóa không chỉ đến từ prompt, mà đến từ dữ liệu thật của hệ thống.

---

## 6.7. Chatbot có direct business flow cho các intent quan trọng

Đây là một trong những điểm tốt nhất của phần AI.

Thay vì đẩy toàn bộ intent vào agent, repo tách một số intent mạnh ra flow riêng:

- wishlist recommendation
- loyalty benefit
- size consulting
- direct product search

Lợi ích:

- nhanh hơn
- ít lỗi hơn
- deterministic hơn
- đúng nghiệp vụ hơn

Ví dụ:

- hỏi wishlist mà chưa login thì bot trả lời yêu cầu đăng nhập
- hỏi loyalty cũng vậy
- hỏi size với số đo rõ thì vào thẳng `buildSizeConsultationResponse()`
- hỏi sản phẩm cụ thể thì đi strict search trước

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

- hỏi chi tiết
- hỏi review
- hỏi khuyến mãi

trên cùng một sản phẩm đã chọn.

Nghĩa là user không cần tách 3 câu rời rạc kiểu:

- “cho mình chi tiết”
- “review sao”
- “có giảm giá không”

Bot có thể tổng hợp lại thành một reply gộp cho đúng mẫu đó.

Điểm này rất hợp demo vì cho thấy bot làm “người bán hàng” tốt hơn, không chỉ là router intent cơ bản.

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
