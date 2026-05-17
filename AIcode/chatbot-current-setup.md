# Chatbot AI Current Setup

## 1. Muc dich

Tai lieu nay tom tat nhanh chatbot AI tu van thoi trang hien tai:

- no manh o dau
- no yeu o dau
- runtime dang cau hinh the nao
- kien truc gom nhung file/module gi
- muon xay dung va nang cap no can nhung gi

Tai lieu nay uu tien de dev moi doc nhanh va vao viec ngay.

---

## 2. Danh gia nhanh hien trang

### Diem manh

- Da vuot muc "catalog biet noi", co flow tu van, compare va closing co ban.
- Co memory + session + selected product context, nen hoi dap nhieu turn on hon truoc.
- Co direct flow cho cac intent commerce quan trong, giam phu thuoc hoan toan vao agent.
- Co knowledge ingestion + GraphRAG nhe de tra loi policy, FAQ, playbook ban hang.
- Co scoring san pham theo:
  - search context
  - budget
  - size / color
  - metadata enrich
  - occasion
  - body-shape
- Co guardrail hau kiem de giam hallucination ve gia, promo, ton kho, policy.
- Co resilience cho downstream service.

### Diem yeu

- `ChatbotServiceImpl.java` va `FashionTools.java` van con nang, du da tach bot.
- Agent van co rui ro `NPE / timeout / 524`, nen mot so turn van roi ve fallback.
- Metadata san pham hien la enrich tam tu `title + category`, chua phai metadata that trong DB.
- Phan stylist reasoning da tot hon, nhung chua sau o:
  - compare 3+ san pham
  - outfit-level recommendation
  - objection handling sau
- Monitoring quality signals chua lam.

### Muc do san sang

- Tot cho demo, iteration va UAT co huong.
- Chua nen xem la AI sales advisor "hoan thien" neu chua co:
  - quality analytics
  - user feedback loop
  - metadata san pham that
  - giam loi agent runtime

---

## 3. Runtime configuration hien tai

### LLM runtime

File:

- `backend/services/chatbot-service/src/main/java/com/fashion/chatbotservice/config/AgentConfig.java`
- `backend/services/chatbot-service/src/main/resources/application.yml`

Runtime dang dung:

- `LangChain4j`
- `OpenAiChatModel`
- OpenAI-compatible endpoint

Gia tri mac dinh:

- `AI_BASE_URL=https://llm.chiasegpu.vn/v1`
- `AI_MODEL=deepseek-3.2`
- `AI_TIMEOUT_SECONDS=180`
- `AI_MAX_TOKENS=4096`

Y nghia:

- Runtime chinh hien tai khong chay bang `OllamaChatModel`.
- `ollama.*` con xuat hien o mot so config cu nhung khong phai path runtime chinh.

### Backend service config

File:

- `backend/services/chatbot-service/src/main/resources/application.yml`
- `backend/config-repo/chatbot-service.yml`

Cac nhom config quan trong:

- `chatbot.product-service-url`
- `chatbot.promotion-service-url`
- `chatbot.order-service-url`
- `chatbot.cart-service-url`
- `chatbot.review-service-url`
- `chatbot.use-agent`
- `chatbot.memory.max-messages`
- `chatbot.knowledge.*`
- `chatbot.resilience.*`
- `ai.*`

### Data store

- MongoDB dung cho:
  - session
  - long-term preference
  - knowledge chunks
  - graph nodes / edges
  - analytics

---

## 4. Kien truc tong quan

Chatbot hien tai nen duoc hieu la:

`Frontend chat UI + Spring orchestration + LangChain4j agent + tools + Mongo memory + rule layers`

Khong nen hieu no nhu:

- mot model AI train rieng trong repo
- hay mot prompt text don gian

---

## 5. Cac module chinh va vi sao nen co

### 5.1 Entry / orchestration

#### `controller/ChatbotController.java`

Chuc nang:

- nhan request chat
- lay session id / user context
- goi service chinh

Vi sao nen co:

- giu controller mong
- tach HTTP layer khoi business flow

#### `service/impl/ChatbotServiceImpl.java`

Chuc nang:

- orchestration trung tam
- dieu phoi session, state, agent, direct flow, fallback, persistence

Vi sao nen co:

- day la noi gom toan bo "workflow chatbot"
- cac module khac nen duoc goi tu day, khong nen de logic rach o controller

### 5.2 Agent + tool layer

#### `agent/FashionAgent.java`

Chuc nang:

- contract cho LangChain4j AI service

Vi sao nen co:

- giu prompt/agent boundary ro rang

#### `agent/FashionTools.java`

Chuc nang:

- tool-calling sang:
  - product-service
  - promotion-service
  - order-service
  - cart-service
  - review-service

Vi sao nen co:

- chatbot ban hang can du lieu that
- khong the chi dua vao model text

#### `agent/ResponseGuardrail.java`

Chuc nang:

- hau kiem reply cuoi
- sua/chan hallucination

Vi sao nen co:

- LLM va fallback deu co the noi qua tu tin
- day la lop bao hiem truoc khi tra response cho user

### 5.3 Conversation flow

#### `conversation/SalesStage.java`

Chuc nang:

- dinh nghia stage:
  - `DISCOVERY`
  - `FILTERING`
  - `RECOMMENDING`
  - `COMPARING`
  - `CLOSING`

Vi sao nen co:

- chatbot can "biet dang o buoc nao"
- tranh hoi lan man va recommend qua som

#### `conversation/StylingSlots.java`

Chuc nang:

- luu slot tu van:
  - gender
  - occasion
  - style vibe
  - product type
  - budget
  - size
  - fit
  - color
  - height / weight

Vi sao nen co:

- chatbot stylist phai dua tren slot ro rang, khong chi keyword search

#### `conversation/impl/ConversationStateServiceImpl.java`

Chuc nang:

- quyet dinh chatbot nen:
  - hoi gi tiep
  - recommend chua
  - chuyen stage nao

Vi sao nen co:

- day la "state machine" cua chat ban hang
- quan trong hon viec prompt noi hay

#### `conversation/impl/SlotFillingServiceImpl.java`

Chuc nang:

- rut slot tu message va profile

Vi sao nen co:

- neu khong co slot filling, chatbot se tiep tuc giong catalog search

### 5.4 Product intelligence

#### `product/ProductMetadataProfile.java`

Chuc nang:

- model metadata enrich tam cho san pham

Vi sao nen co:

- chatbot can ly do stylist:
  - easy to match
  - safe choice
  - office
  - date
  - premium look

#### `product/impl/ProductMetadataEnrichmentServiceImpl.java`

Chuc nang:

- enrich metadata tu `title + category`

Vi sao nen co:

- truoc khi co metadata that trong DB, day la cach tam de bot "co gu hon"

#### `product/impl/ProductScoringServiceImpl.java`

Chuc nang:

- cham diem san pham theo:
  - occasion
  - style
  - body shape
  - budget
  - size / color
  - versatility

Vi sao nen co:

- recommendation phai dua tren score co ly do, khong chi search hit

### 5.5 Stylist rule layer

#### `styling/impl/BodyShapeAdvisorServiceImpl.java`

Chuc nang:

- rule theo dang nguoi:
  - petite / thap
  - gay
  - dam nguoi
  - fit preference

Vi sao nen co:

- body-shape reasoning khong nen de agent tu do tu nghi

#### `styling/impl/OccasionAdvisorServiceImpl.java`

Chuc nang:

- rule theo dip mac:
  - work
  - date
  - daily
  - travel
  - party light

Vi sao nen co:

- day la cot song cua stylist logic

### 5.6 Sales layer

#### `sales/impl/CompareEngineImpl.java`

Chuc nang:

- so sanh list gan nhat theo:
  - safer choice
  - stylish choice
  - better value
  - easier to style

Vi sao nen co:

- user mua hang thuong hoi:
  - "nen chon cai nao"
  - "mau nao an toan hon"
  - "dang tien hon"

#### `sales/impl/ClosingEngineImpl.java`

Chuc nang:

- tao soft close / decision close

Vi sao nen co:

- chatbot ban hang phai biet day user toi quyet dinh, khong chi tu van

### 5.7 Response layer

#### `response/FashionResponseComposer.java`

Chuc nang:

- compose reply theo style stylist:
  - option 1/2/3
  - ly do
  - styling hint
  - closing line

Vi sao nen co:

- giu format response on dinh
- tranh de `ChatbotServiceImpl` noi text linh tinh

---

## 6. FE chatbot can nhung gi

Frontend dang nam o:

- `ecommerce-frontend/src/modules/chatbot/components/ChatWidget.jsx`
- `.../ChatMessage.jsx`
- `.../hooks/useChatbot.js`
- `.../services/chatbotService.js`
- `.../pages/ChatbotPage.jsx`

Can co:

- `sessionId`
- render message + product cards
- selected product context khi click card
- hydrate session tu backend

Vi sao nen co:

- chatbot commerce khong the chi la textbox
- card context la dieu kien quan trong de follow-up "mau nay", "san pham nay", "trong list nay"

---

## 7. Xay dung chatbot nay can nhung gi

### Bat buoc

- 1 LLM endpoint on dinh
- MongoDB cho session / knowledge / analytics
- microservice du lieu that:
  - product
  - promotion
  - order
  - cart
  - review
- knowledge markdown
- state machine conversation
- guardrail

### Rat nen co

- product metadata that trong DB
- signal quality / analytics
- eval set regression
- feedback loop tu UAT / user behavior

### Neu muon no thong minh hon nua

- metadata san pham chuan schema
- outfit builder theo set
- objection handling sau
- analytics quality signals
- ranking theo click / conversion / order data that

---

## 8. Diem manh va diem yeu theo nghiep vu

### Hien tai chatbot lam tot

- loyalty / wishlist / promotion lookup
- search product theo context
- compare tren list gan nhat
- selected product follow-up
- consultative flow co ban
- recommendation co ly do tot hon truoc

### Hien tai chatbot chua that tot

- van phu thuoc agent o mot so case phuc tap
- title grounding chua the xem la perfect
- metadata enrich van la heuristic tam
- outfit-level reasoning chua sau
- objection / upsell / cross-sell chua manh
- chua co signal do "quality" sau moi dot sua

---

## 9. Cau hinh van hanh can nho

- `ai.log-requests`, `ai.log-responses`
  - dev co the bat
  - production nen than trong

- `chatbot.resilience.*`
  - dang bao ve downstream call

- `chatbot.use-agent`
  - bat/tat direct agent path

- `chatbot.memory.max-messages`
  - gioi han chat memory window

- `chatbot.knowledge.*`
  - bat/tat knowledge + GraphRAG

---

## 10. Ket luan ngan

Chatbot hien tai da co 4 lop quan trong:

1. `Conversation flow`
2. `Product intelligence`
3. `Sales compare + close`
4. `Body-shape + occasion rule layer`

No da du de xem la mot nen chatbot sales advisor co huong ro.

Nhung de len muc tot hon nua, uu tien tiep theo nen la:

1. analytics quality signals
2. metadata san pham that
3. giam loi agent runtime
4. objection / upsell / outfit intelligence
