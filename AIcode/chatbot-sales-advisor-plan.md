# Chatbot Sales Advisor Plan

## 1. Muc dich cua file nay

File nay dung de luu lai session planning cho chatbot tu van ban hang, de co the quay lai review theo tung phase.

Muc tieu:

- xac dinh ro chatbot hien tai dang thieu gi
- dinh nghia dich den mong muon
- chia roadmap thanh nhieu phase co the review va trien khai dan
- ghi lai thu tu uu tien de khong sua lan man

## 2. Muc tieu dich den

Chatbot mong muon khong chi tra loi duoc, ma phai hoat dong gan voi mot nhan vien sales/CSKH thuc thu:

- tu van san pham dung voi nhu cau that cua khach
- hoi lam ro nhu cau dung luc
- uu tien san pham dang kinh doanh va con kha nang chot don
- ca nhan hoa theo style, budget, size, lich su mua sam
- goi y outfit / combo / upsell / cross-sell hop ly
- tra loi policy dung va nhat quan
- khong bịa du lieu
- co the danh gia va cai thien theo ket qua thuc te

## 3. Danh gia hien tai

Setup hien tai da co nen tang kha tot:

- FE chatbot widget + page rieng
- BE orchestration Spring Boot + LangChain4j
- model provider qua OpenAI-compatible API
- tool-calling sang `product`, `promotion`, `order`, `cart`, `review`
- MongoDB cho session, preference, knowledge, analytics
- knowledge retrieval tren `faq.md` va `policy.md`
- GraphRAG nhe tren Mongo

Nhung setup nay moi dat muc:

- chatbot co kha nang hoi dap va tim du lieu
- chatbot co the goi y o muc co ban
- chatbot chua dat muc seller / CSKH giau ban nang ban hang

## 4. Khoang cach giua hien tai va muc tieu

### 4.1. Thieu sales knowledge chuyen biet

Hien tai knowledge moi nghieng ve:

- FAQ
- policy

Con thieu:

- sales playbook
- style guide cua brand
- product positioning
- rules upsell / cross-sell
- mapping san pham theo dip mac / mood / climate / budget
- pattern xu ly objection cua khach

### 4.2. Qua nhieu quyet dinh dang day vao model

Model hien tai dang phai tu quyet dinh qua nhieu:

- hoi gi tiep theo
- luc nao nen suggest san pham
- nen suggest 1 mon hay 1 combo
- luc nao nen upsell
- luc nao nen chot mem

Neu de model tu xoay so qua nhieu, chat se:

- khong on dinh
- luc hay luc do
- kho kiem soat hanh vi business

### 4.3. Chua co ranking business ro rang

Tim duoc san pham khong dong nghia voi tu van tot.

Can co ranking theo:

- dung nhu cau
- con hang
- dung size
- dung budget
- hop style
- uu tien san pham can day
- uu tien item co review / khuyen mai / margin tot

Hien tai lop nay chua du ro va chua du trung tam.

### 4.4. Memory va profile da co, nhung chua sau

Hien tai chatbot nho duoc mot so thong tin co ban:

- style
- budget
- focus
- mot phan preference

Nhung chua du de hanh xu nhu mot sales advisor that:

- price comfort zone thuc te
- category affinity theo tan suat
- preferred fit theo loai san pham
- color habit theo hanh vi mua, khong chi theo loi noi
- recommendation da goi y truoc do co hieu qua hay khong

### 4.5. Chua co vong feedback de cai thien chatbot

Neu chatbot recommend ma khong do:

- user co click hay khong
- co them vao cart khong
- co mua khong
- co quay lai hoi tiep khong

thi chatbot rat kho tien bo theo huong business.

### 4.6. Chua co bo eval / scenario review chuan

Muon review chatbot nghiem tuc, can bo tinh huong test on dinh:

- khach tim ao di lam
- khach budget 500k
- khach hoi size
- khach hoi mau phu hop
- khach can outfit theo dip
- khach hoi khuyen mai
- khach compare 2 mon

Hien tai chua thay bo eval theo kieu nay.

## 5. Nguyen tac trien khai de tranh vo tran

1. Khong co gang giai quyet tat ca bang prompt.
2. Dua cac quy tac business on dinh ra khoi prompt neu co the.
3. Uu tien su dung du lieu that tu service va ranking co kiem soat.
4. Tinh nang nao phuc vu sales phai do duoc hieu qua.
5. Moi phase phai co deliverable ro de review.

## 6. Roadmap de xuat theo phase

## Phase 1 - On dinh hanh vi tu van co ban

### Muc tieu

Lam cho chatbot tra loi on dinh hon va biet tu van theo flow co kiem soat, truoc khi mo rong thong minh hon.

### Cong viec chinh

1. Xac dinh lai cac flow sales can co:
   - product search
   - cold start clarification
   - size consulting
   - outfit suggestion
   - promotion-aware recommendation
   - policy support

2. Tinh gon va chuan hoa behavior prompt:
   - giam phan dai dong trung lap
   - tach quy tac bat buoc va style response
   - uu tien hanh vi hoi lam ro truoc khi suggest khi input qua mo ho

3. Review va chot lai intent routing:
   - khi nao vao product search
   - khi nao vao general browsing
   - khi nao vao size
   - khi nao vao policy

4. Chuan hoa response template cho sales:
   - toi da 3 san pham
   - moi san pham co ly do phu hop
   - neu co upsell/cross-sell thi chi dua 1-2 goi y phu

### Deliverable

- mot bo flow chatbot co the review bang tay
- prompt va response style on dinh hon
- danh sach tinh huong co expected behavior cho phase 1

### Tieu chi hoan thanh

- chatbot khong tra loi lan man o cac case pho bien
- chatbot hoi lam ro tot hon o case mo ho
- chatbot suggest san pham co ly do ro hon

## Phase 2 - Them sales knowledge va business context

### Muc tieu

Cho chatbot co "ngon ngu va logic cua nguoi ban hang", khong chi biet FAQ/policy.

### Cong viec chinh

1. Tao sales knowledge base rieng, tach khoi `faq.md` va `policy.md`.

2. Bo sung knowledge cho:
   - playbook hoi nhu cau
   - guide phan loai style
   - guide phoi outfit
   - budget bands
   - objection handling
   - closing soft phrases

3. Dinh nghia ro cac business signal can dua vao context:
   - preferred category
   - purchase pattern
   - wishlist affinity
   - budget habit
   - loyalty sensitivity

4. Xem xet tach:
   - policy knowledge
   - sales knowledge
   - product positioning knowledge

### Deliverable

- bo markdown/knowledge moi cho sales advisor
- tai lieu hoa business context can dua vao chat

### Asset da them

- `backend/services/chatbot-service/src/main/resources/knowledge/sales-playbook.md`
- `backend/services/chatbot-service/src/main/resources/knowledge/style-guide.md`
- `backend/services/chatbot-service/src/main/resources/knowledge/sales-objections.md`

Muc dich:

- bo sung playbook hoi nhu cau va chot mem
- bo sung logic style / dip mac / tone tu van
- bo sung xu ly objection co dinh huong sales

### Tieu chi hoan thanh

- chatbot biet dat cau hoi va goi y tu nhien hon
- chatbot co giong sales/CSKH hon trong hoi thoai

## Phase 3 - Dua recommendation logic ve business layer

### Muc tieu

Khong de model tu quyet dinh hoan toan mon nao nen dua len dau.

### Cong viec chinh

1. Tao ranking strategy cho suggestions:
   - match nhu cau
   - stock readiness
   - size availability
   - budget fit
   - style fit
   - promotion fit
   - review quality

2. Tinh diem san pham truoc khi tra ve cho model.

### Tien do da lam toi hien tai

- `Phase 1`: da on dinh flow tra loi co ban, giam text dai va dedupe du lieu tra ve
- `Phase 1.1`: da rut gon tool reply, uu tien product cards thay vi liet ke dai dong
- `Phase 2`: da them sales knowledge base
- `Phase 2.1`: da noi knowledge sales vao prompt va fallback behavior
- `Phase 3`: da dua scoring business vao ranking suggestions
- `Phase 3.1`: da them conversation state nhe cho session dai
- `Phase 3.2`: da them taxonomy/use-case map cho office/casual/party/safe/statement
- `Phase 3.3`: da tach recommendation va taxonomy ra service rieng

### Asset va refactor cua Phase 3.3

- `backend/services/chatbot-service/src/main/java/com/fashion/chatbotservice/service/ProductRecommendationService.java`
- `backend/services/chatbot-service/src/main/java/com/fashion/chatbotservice/service/impl/ProductRecommendationServiceImpl.java`
- `backend/services/chatbot-service/src/main/java/com/fashion/chatbotservice/service/ProductTaxonomyService.java`
- `backend/services/chatbot-service/src/main/java/com/fashion/chatbotservice/service/impl/ProductTaxonomyServiceImpl.java`

Muc dich:

- tach scorer/ranker khoi `FashionTools`
- tach taxonomy/use-case parsing khoi tool layer
- giup recommendation business de test, mo rong va review doc lap hon

## Phase 4 - Deep memory va size-fit advisory

### Muc tieu

- nho khach hang sau hon qua nhieu turn va nhieu session
- tu van size/fit theo rule thoi trang cua shop, khong tra loi qua generic
- bo sung business context ve persona, dip mac, vung gia thoai mai

### Asset da them

- `backend/services/chatbot-service/src/main/resources/knowledge/customer-personas.md`
- `backend/services/chatbot-service/src/main/resources/knowledge/size-fit-rules.md`
- `backend/services/chatbot-service/src/main/resources/knowledge/business-priority-rules.md`
- `backend/services/chatbot-service/src/main/java/com/fashion/chatbotservice/service/SizeFitAdvisoryService.java`
- `backend/services/chatbot-service/src/main/java/com/fashion/chatbotservice/service/impl/SizeFitAdvisoryServiceImpl.java`

### Tien do da lam trong Phase 4

- them memory fields cho persona, fit preference, occasion, price comfort zone, target gender
- enrich profile tu message theo office/casual/party/travel va relaxed/fitted
- bat dau route tu van size qua `SizeFitAdvisoryService` o orchestrator
- dua business input moi vao knowledge de chatbot ingest cung cac markdown hien co

## Phase 4.1 - Dong nhat size-fit va follow-up

### Tien do da lam

- dong nhat luong size-fit o heuristic orchestration theo `SizeFitAdvisoryService`
- bo sung follow-up context cho nhung cau kieu `phan van S hay M`, `mac rong hay om`, `vai rong`, `dui to`
- uu tien dung `lastProductCategoryQueried` khi tu van size de advice gan voi mon do dang duoc theo duoi

3. Tinh diem combo/outfit thay vi de model ghep tu do.

4. Xac dinh ro:
   - flagship products
   - products to push
   - safe alternatives khi het hang

### Deliverable

- logic ranking co review duoc
- output suggestions co thu tu hop ly hon

### Tieu chi hoan thanh

- 2 lan chat cung kieu nhu cau cho ket qua gan on dinh
- suggestions phan anh uu tien business ro rang hon

## Phase 4 - Lam sau customer memory va personalization

### Muc tieu

Cho chatbot nho khach hang theo cach huu ich cho sales.

### Cong viec chinh

1. Mo rong `PreferenceProfile` va `UserPreferenceDocument` neu can.

2. Bo sung cac signal:
   - gia thuong mua
   - category mua nhieu
   - preferred fit
   - preferred colors theo hanh vi
   - history recommendation effectiveness

3. Tach ro:
   - stated preference
   - inferred preference
   - observed purchase behavior

4. Xac dinh expiration / confidence cho tung loai preference.

### Deliverable

- customer profile chat phuc vu sales ro hon
- luong bootstrap session moi tot hon

### Tieu chi hoan thanh

- chatbot co the dua lai context hop ly o nhung lan chat sau
- giam so cau hoi lap lai khong can thiet

## Phase 5 - Analytics va feedback loop

### Muc tieu

Bien chatbot thanh he thong co the toi uu duoc, khong chi la chat vui.

### Cong viec chinh

1. Theo doi:
   - suggestion shown
   - product clicked
   - add-to-cart after chat
   - order after chat
   - promotion used after chat

2. Gan trace giua:
   - chatbot session
   - recommendation
   - user action

3. Tao metrics review:
   - response helpfulness
   - conversion assist rate
   - size advice acceptance
   - policy answer correctness

### Deliverable

- bo event / analytics phuc vu review business
- dashboard hoac file tong hop metric co the doc duoc

### Tieu chi hoan thanh

- biet recommendation nao co tac dung
- biet chatbot dang fail o dau theo data that

## Phase 6 - Eval set va quy trinh review dinh ky

### Muc tieu

Co cach review chatbot bang tay va bang testcase co lap, khong danh gia cam tinh.

### Cong viec chinh

1. Tao bo scenario test:
   - cold start
   - product search
   - size consultation
   - outfit suggestion
   - budget constraint
   - policy FAQ
   - promotion/loyalty
   - compare product
   - fallback / service unavailable

2. Moi scenario can co:
   - input
   - expected behavior
   - expected follow-up question
   - expected product style
   - expected "khong duoc lam"

3. Dung bo scenario nay de review moi lan sua lon.

### Deliverable

- checklist review chatbot theo phase
- eval scenarios co the tai su dung

### Tieu chi hoan thanh

- moi lan sua chatbot deu co cach test lai co he thong
- review theo phase khong bi tranh luan cam tinh

## 7. Thu tu uu tien thuc te de lam trong repo nay

Neu phai chon thu tu toi uu cho codebase hien tai, nen lam:

1. Phase 1
2. Phase 2
3. Phase 3
4. Phase 6
5. Phase 4
6. Phase 5

Ly do:

- can on dinh behavior truoc
- sau do moi bo sung knowledge
- roi moi dua ranking business vao
- tiep theo moi co bo review scenarios
- memory sau hon vi can dung pham vi
- analytics business sau khi flow da bot loang

## 8. De xuat ky thuat cu the cho repo hien tai

### 8.1. Nhung gi nen giu

- `LangChain4j` agent architecture
- `FashionTools` la diem goi du lieu realtime
- `ChatSession` va `UserPreferenceDocument`
- knowledge ingestion tu markdown

### 8.2. Nhung gi nen chuan hoa tiep

- bo config cu nghiêng `ollama.*`
- dua Mongo credential ve env/secret thay vi hardcode trong config repo
- tach sales knowledge khoi policy knowledge
- giam business logic nam hoan toan trong prompt neu co the dua xuong code

### 8.3. Nhung gi co the se can them sau

- recommendation scorer/service rieng
- scenario eval files
- event tracking cho post-chat conversion
- knowledge sources cho stylist / sales playbook

## 9. Cach dung file nay o cac session sau

Moi lan review tiep theo, nen lam theo thu tu:

1. Chon 1 phase dang lam
2. Liet ke file code lien quan cua phase do
3. Chot deliverable nho trong phase
4. Implement
5. Review lai theo tieu chi hoan thanh cua phase

## 10. Ket luan ngan

Van de cua chatbot hien tai khong phai chi la doi model.

DeepSeek co the lam "bo nao hoi thoai", nhung de chatbot giong sales/CSKH that thi can them:

- sales knowledge
- ranking business
- memory huu ich
- feedback loop
- eval process

File nay se la roadmap nen de quay lai review tung phase thay vi sua ngau nhien.

## 11. Tien do hien tai sau cac vong nang cap

### 11.1. Nhung gi da lam xong

- Phase 1:
  - rut gon text tra loi
  - gioi han so luong suggestion
  - giam lap du lieu giua text va product cards
  - on dinh hon cho cold start va flow tu van co ban

- Phase 1.1:
  - FE `/chatbot` va mini widget de doc hon
  - text bot khong con doc lai qua nhieu thuoc tinh san pham
  - compare van duoc giu chi tiet

- Phase 2:
  - da them sales knowledge assets:
    - `sales-playbook.md`
    - `style-guide.md`
    - `sales-objections.md`

- Phase 2.1:
  - da noi sales knowledge vao prompt, tool va fallback
  - chatbot co the tra loi theo goc nhin sales/stylist tot hon o nhung cau hoi phan van, de mac, an toan, gia hoi cao

- Phase 3:
  - da dua ranking suggestions ve business layer
  - scoring hien tai da tinh den:
    - query match
    - explicit size/color
    - budget fit
    - stock readiness
    - style fit
    - profile affinity

- Multi-turn stabilization:
  - da fix them nhung case:
    - wishlist / loyalty khong di qua agent de tranh NPE
    - size selection tren mot mon cu the khong bi day sai sang intent size consulting
    - follow-up chi doi budget khong bi parse nham category
    - session co co che tai su dung `lastProductCategoryQueried`

### 11.2. Danh gia chat luong hien tai

Danh gia thuc te hien tai:

- muc dat duoc tam:
  - `55% -> 65%`

Ly do:

- chatbot da hieu duoc kha nhieu intent
- da biet goi tool that, co memory co ban, co ranking, co knowledge sales
- nhung van chua dat muc sales advisor that vi:
  - multi-turn context chua that su ben
  - outfit recommendation van con tho
  - ranking chua co business signals manh tu conversion / review / push-product
  - chua co bo eval scenario de do tien bo mot cach khach quan
  - chua co event feedback loop de biet goi y nao thuc su co tac dung

## 12. Review ky thuat ngan sau vong nang cap nay

### Diem manh hien tai

- architecture agent + tools + Mongo memory van dung huong
- da tach duong `policy knowledge` va `sales guidance` ro hon ve vai tro
- FE card-based rendering hop ly hon cho bai toan e-commerce
- da co business scoring thay vi de model tu xoay toan bo

### Diem yeu hien tai

1. Chatbot van de bi "troi intent" trong session dai.
2. Product understanding van phu thuoc nhieu vao ten san pham/category text, chua co taxonomy san pham chat.
3. Outfit / styling chua co bo luat phoi do that su manh.
4. Chua co notion "safe fallback recommendation set" theo category.
5. Chua co conversation state machine cho cac flow:
   - dang clarifying
   - dang compare
   - dang chot theo budget
   - dang chot theo size
6. Chua co bo danh gia theo scenario va metrics.

## 13. De xuat nang cap tiep theo

Neu muon vuot qua moc 70% roi tien toi 80%+, nen uu tien theo thu tu nay:

1. Phase 3.1:
   - them conversation state ro cho cac flow multi-turn
   - khong de moi turn deu classify lai nhu mot request moi

2. Phase 3.2:
   - tao taxonomy san pham / category alias / use-case map ro hon
   - mapping:
     - di lam
     - di choi
     - di tiec
     - mua he / mua dong
     - basic / minimal / smart casual

3. Phase 3.3:
   - tach recommendation service rieng khoi `FashionTools`
   - cho scorer input ro hon thay vi dua tren string matching

4. Phase 4:
   - nang memory len muc "stated vs inferred vs observed"
   - uu tien signal nao co do tin cay cao hon

5. Phase 5:
   - gan chatbot session voi click / add-to-cart / order
   - neu khong co feedback loop thi rat kho dat muc sales advisor that

6. Phase 6:
   - tao eval scenario set
   - moi lan sua chatbot phai test lai cung mot bo case

## 14. De xuat nghiep vu de chatbot tu van quan ao tot hon

Tu van thoi trang khong giong FAQ bot. Muon bot hay hon, can bo sung:

1. Product taxonomy that su tot:
   - ten item
   - loai item
   - dip mac
   - form
   - tone mau
   - muc do an toan / de phoi
   - do formal
   - mua / thoi tiet

2. Outfit rules:
   - ao nao hop voi quan nao
   - item nao la mon chinh
   - item nao la mon bo sung
   - item nao la "safe office"
   - item nao la "statement piece"

3. Sales playbook theo category:
   - ao so mi
   - ao thun
   - dam
   - chan vay
   - ao khoac

4. Better profile memory:
   - user nay uu tien de mac hay noi bat
   - thuong mua tam gia nao
   - hay chot category nao
   - thuong bo gio hang o step nao

5. Session state:
   - dang tim category
   - dang chot budget
   - dang chot size
   - dang compare
   - dang xin option thay the

Neu khong co 5 lop nay, chatbot rat de "hieu tung cau" nhung khong "tu van duoc ca cuoc hoi thoai".

## 15. Cap nhat sau Phase 3.1 va 3.2

### Phase 3.1

Da them lightweight conversation state:

- `conversationFlow`
- `pendingQuestionType`
- `conversationStateUpdatedAt`

Tac dung:

- giam drift trong session dai
- budget follow-up / size follow-up / measurement follow-up bam sat flow hon

### Phase 3.2

Da them taxonomy/use-case map o business layer:

- scoring theo `office / casual / party / summer / winter`
- scoring theo `safe / statement`
- outfit query da bo sung richer aliases nhu:
  - `blazer`
  - `chan vay midi`
  - `ao kieu`
  - `linen`

Tac dung:

- bot bat dau hieu ngon ngu nghiep vu kieu:
  - di lam
  - de mac
  - de phoi
  - an toan
  - co diem nhan

Gioi han hien tai:

- taxonomy van la rule-based va dua nhieu tren string matching
- chua co catalog metadata thuc su chuan hoa o product-service

## 16. Cap nhat sau Phase 3.3

### Phase 3.3

Da tach business recommendation layer khoi tool layer:

- `ProductRecommendationService`
- `ProductTaxonomyService`

Tac dung:

- `FashionTools` giam bot logic scoring phuc tap
- de mo rong recommendation rule theo nghiep vu ma khong lam tool layer ngay cang kho bao tri

## 17. Cap nhat sau Phase 4 va 4.1

### Phase 4

Da them memory/profile nghiep vu sau hon:

- `preferredOccasions`
- `fitPreference`
- `customerPersona`
- `priceComfortZone`
- `targetGender`

Da bo sung knowledge assets:

- `customer-personas.md`
- `size-fit-rules.md`
- `business-priority-rules.md`

Da them `SizeFitAdvisoryService` de dua logic size/fit ve mot service rieng.

### Phase 4.1

Da dong nhat duong tu van size o ca tool layer va heuristic fallback qua `SizeFitAdvisoryService`.

Tac dung:

- giam sai lech giua agent va fallback
- size follow-up giu duoc mach hoi thoai tot hon

## 18. Cap nhat sau Phase 5

### Phase 5

Da bat dau event-level analytics cho chatbot:

- them endpoint `POST /api/v1/chatbot/analytics/events`
- FE track duoc click san pham tu:
  - product cards trong bubble chat
  - khu `Latest Curated Picks`
- Mongo analytics da luu duoc:
  - `eventType`
  - `sourceMessageId`
  - `productId`
  - `productName`
  - `metadata`

Muc dich:

- tao nen tang do `shown -> click`
- biet goi y nao thuc su lam user tuong tac
- chuan bi cho buoc sau noi tiep `add-to-cart -> order attribution`

Gioi han hien tai:

- chua bat su kien add-to-cart that
- chua co dashboard analytics
- `sourceMessageId` hien duoc dung de noi event voi bubble FE, chua phai attribution day du den order

## 19. Cap nhat sau Phase 5.1

### Phase 5.1

Da bo sung event tracking gan hon voi hanh vi mua hang thuc te:

- `compare_intent`
- `view_more_products`
- `add_to_cart_intent`
- `add_to_cart_success`

Huong trien khai:

- detect `compare/view more/add-to-cart intent` tu chinh cau chat cua user
- luu attribution khi user click product card tu chatbot
- mang attribution sang `ProductDetailPage`
- ghi `add_to_cart_success` sau khi thao tac them gio thanh cong

Tac dung:

- khong chi biet user co click hay khong
- bat dau noi duoc recommendation cua chatbot voi hanh vi mua gan that
- tao nen tang cho buoc tiep theo: attribution den order/checkout

Gioi han hien tai:

- attribution hien tai moi theo `product click -> product detail -> add to cart`
- chua theo doi duoc neu user mo san pham tu kenh khac roi moi them gio
- chua noi tiep den `checkout_success` hoac `order_success`

## 20. Cap nhat sau Phase 5.2

### Phase 5.2

Da noi attribution cua chatbot toi checkout/order flow:

- luu checkout attribution sau `add_to_cart_success`
- gui `checkout_submit` khi tao order
- gui `order_success` cho COD thanh cong
- gui `order_success` cho VNPay sau verify thanh cong

Tac dung:

- bat dau do duoc recommendation cua chatbot co di den checkout/order hay khong
- co chuoi gan day du hon: `shown -> click -> add_to_cart_success -> checkout_submit -> order_success`

Gioi han hien tai:

- attribution hien tai dua tren session storage o FE
- neu user doi trinh duyet/thiet bi thi attribution se mat
- chua co backfill attribution tu order-service de xac nhan nguon chatbot o backend

## 21. Cap nhat sau Phase 6

### Phase 6

Da them bo tai lieu danh gia de review chatbot theo cung mot chuan:

- `chatbot-eval-scenarios.md`
- `chatbot-eval-scorecard.md`
- `chatbot-regression-checklist.md`

Muc dich:

- test chatbot theo scenario nghiep vu co dinh
- cham diem theo rubric thay vi danh gia cam giac
- giam nguy co sua phase sau lam hong phase truoc

Gia tri thuc te:

- co the do bot dang o muc nao theo nhom bai toan:
  - product discovery
  - size/fit
  - objection handling
  - compare/closing
  - session dai
- de thu nghiem du lieu gia lap tiep theo ma van giu duoc benchmark cu

## 22. Cap nhat sau Phase 7

### Phase 7

Da sieu lai trust model cho internal calls cua chatbot-service:

- WebClient cua chatbot-service tu dong gui:
  - `X-Internal-Caller`
  - `X-Internal-Auth`
- `FashionTools` giu them `currentUserId` theo session de cac protected endpoint co user context ro hon
- cac service duoc chatbot goi sync da chap nhan:
  - gateway identity
  - hoac trusted internal caller

Pham vi da ap dung:

- `cart-service`
- `product-service`
- `promotion-service`
- `order-service`
- `review-service`
- `user-service`

Tac dung:

- chatbot khong con goi internal service theo kieu "bare internal traffic"
- trust boundary ro hon so voi viec chi dua vao `X-User-Id`
- de override bang secret/env theo moi truong

Gioi han hien tai:

- van la shared-secret model, chua phai mTLS/service mesh
- chua verify signature theo request payload
- ownership business o downstream service van can tiep tuc duoc review rieng

## 23. Cap nhat sau Memory + Slot Hardening

### Memory resilience

- khi agent bi `NullPointerException`, lan retry thu hai se dung `recovery context`
- recovery context gom:
  - conversation state
  - profile context
  - 4 turn gan nhat trong session
- muc tieu la tranh clear xong roi goi lai voi bo nho trang

### Slot hardening

- cac cau hoi size du ro nhu `cao/can nang + ao so mi/quan jean + S hay M` duoc short-circuit vao `SizeFitAdvisoryService`
- `cold start` khong con chen vao nhung cau da ro la size-consulting
- nhom cau hoi size co garment ro se uu tien tra loi theo rule thay vi de agent hoi lai loai do

## 24. Khac phuc loi NullPointerException khi LLM tra ve Null hoac thieu doi so

### Vấn đề (NPE trong LangChain4j)
Khi LLM (như Gemini 2.5 hoặc DeepSeek) thực hiện Tool Calling (ví dụ: `suggestOutfit` hoặc `consultSizeTool`), đối với một số đối số không bắt buộc hoặc bị bỏ sót, model có thể điền giá trị `null` hoặc bỏ trống trường đó trong JSON.
Khi LangChain4j nhận được request từ model và chạy qua `DefaultToolExecutor`, nó gọi phương thức `coerceArgument` để ép kiểu. Phương thức này không thể tự động xử lý giá trị `null` cho kiểu chuỗi dẫn đến lỗi `NullPointerException` (NPE) và làm crash toàn bộ request hội thoại của khách hàng.

### Cách xử lý triệt để
Thay vì sửa vá víu từng tool đơn lẻ, chúng ta đã triển khai một lớp giải pháp **chủ động ngăn chặn (Sanitization Interceptor)** ngay tại `FallbackChatLanguageModel` (lớp bọc duy nhất mà mọi LLM đi qua):
1. **Đánh chặn phản hồi (Intercept Response)**: Kiểm tra nếu phản hồi của LLM chứa danh sách các `ToolExecutionRequest`.
2. **Khử trùng tham số (Sanitize Arguments)**:
   - Sử dụng Jackson `ObjectMapper` để parse chuỗi JSON arguments của từng Tool Call thành một `Map<String, Object>`.
   - Duyệt qua map và thay thế toàn bộ giá trị `null` bằng chuỗi rỗng `""` (chuỗi an toàn).
   - Re-serialize map đã được làm sạch về dạng chuỗi JSON mới.
3. **Tạo mới Tool Call**: Sử dụng `ToolExecutionRequest.builder()` để tái cấu trúc Tool Call an toàn và đóng gói ngược lại vào một `AiMessage` mới trước khi chuyển cho LangChain4j thực thi.
4. **Hỗ trợ ép kiểu an toàn ở Tool Layer**: Các hàm hỗ trợ chuyển đổi dữ liệu dạng chuỗi như `parseIntegerSafe` và `parseLongSafe` trong `FashionTools` đã có sẵn cơ chế bỏ qua chuỗi rỗng và chuỗi `"null"` để trả về `null` an toàn mà không lỗi hệ thống.

### Kết quả thử nghiệm
- **Đã kiểm nghiệm thực tế**: Sử dụng Playwright / browser agent để chạy thử nghiệm trên giao diện React (`http://localhost:5173`) với tài khoản khách hàng `phucmanhtran08@gmail.com`.
- **Câu truy vấn**: *"Tư vấn áo và quần nam size M mặc hằng ngày."*
- **Kết quả**: LLM gọi tool gợi ý thành công, toàn bộ tham số trống được chuẩn hóa mượt mà, phản hồi dạng thẻ sản phẩm (tops và bottoms) hiển thị vô cùng premium, bắt mắt, không gặp bất kỳ lỗi NPE hay đứng luồng nào!
- **Minh chứng hình ảnh**: Lưu trữ và hiển thị ảnh chụp màn hình trực quan tại [chatbot_consultation_success.png](file:///C:/Users/PhucManh/.gemini/antigravity/brain/6a248979-4104-477a-8d06-7d0643389cab/artifacts/chatbot_consultation_success.png).

## 25. Sửa lỗi kiểm thử (Unit Tests Failures) khi build lên Server CI/CD

### Vấn đề 1: Lỗi kiểm thử `ResponseQualityScorerTest.lowQualityNoProducts`
* **Triệu chứng**: Test case kiểm tra trường hợp chất lượng câu trả lời thấp do không trả về danh sách sản phẩm gợi ý (cho intent tìm sản phẩm `SEARCH_PRODUCT`) bị thất bại do `score.getWarnings()` trống rỗng.
* **Nguyên nhân**: Trong `ResponseQualityScorerImpl.java`, khi một hội thoại có intent tìm kiếm sản phẩm nhưng dữ liệu `collector` trống và phản hồi không chứa câu báo lỗi lịch sự chuẩn `"mình chưa tìm thấy"`, hệ thống đã không chủ động thêm cảnh báo `Warning` tương ứng.
* **Giải pháp**: Bổ sung nhánh cảnh báo `else if (isProductIntent(intent))` để tự động kích hoạt `qs.addWarning("No products suggested for a product intent");` khi không có sản phẩm nào được trả về cho các ý định mua sắm/size/mùa. Điều này làm cho cảnh báo được đưa ra đầy đủ và chuẩn xác, giúp test case kiểm thử đạt yêu cầu thành công.

### Vấn đề 2: Lỗi kiểm thử `SemanticIntentRouterTest` (`clearPromotionMessage_routesWithoutLLM`, `clearSizeMessage_routesWithoutLLM`, `wishlistMessage_classified`)
* **Triệu chứng**: Các test case giả lập bỏ qua bộ phân loại LLM (TF-IDF/ML Classifier) khi gặp các tin nhắn cực kỳ tường minh (như hỏi voucher, số đo chiều cao, xem wishlist) đều bị chuyển hướng thất bại sang intent mặc định `GENERAL`.
* **Nguyên nhân**: Bộ lọc độ tin cậy `SemanticIntentRouter.java` sử dụng giải thuật cosine overlap chia tổng trọng số từ khóa khớp cho **tổng trọng số của tất cả từ khóa đã đăng ký** đối với intent đó. Việc này dẫn đến việc điểm số tương đồng thực tế của các câu hội thoại cực kỳ ngắn, rõ nghĩa của con người luôn rất nhỏ (ví dụ: `0.176` hoặc `0.333`), không bao giờ có thể vượt qua ngưỡng tin cậy cứng `HIGH_CONFIDENCE_THRESHOLD = 0.82`.
* **Giải pháp**: Nâng cấp giải thuật so khớp từ khóa thông minh (Prime Keyword Boost):
  * Xác định 3 từ khóa hàng đầu của mỗi intent trong `IntentKeywordRegistry` là **từ khóa cốt lõi (Prime Keywords)** (ví dụ: `"size"`, `"voucher"`, `"wishlist"`).
  * Trong `computeKeywordMatchScore`, nếu tin nhắn của khách hàng khớp với bất kỳ từ khóa cốt lõi nào, hệ thống sẽ ngay lập tức trả về điểm tương đồng tối đa là `0.9` (vượt qua ngưỡng `0.82` một cách tuyệt đối).
  * Phương án này giúp các ý định rõ ràng được định tuyến trực tiếp siêu tốc, giảm tối đa tải cho LLM Classifier, đồng thời đảm bảo các tin nhắn mập mờ vẫn được đẩy về LLM Classifier kiểm duyệt chuẩn xác.

### Kết quả
* **Đã chạy kiểm thử cục bộ**: Chạy thành công suite test của Maven (`mvn test` tại `chatbot-service`).
* **Đạt kết quả**: `Tests run: 51, Failures: 0, Errors: 0, Skipped: 0` - **BUILD SUCCESS** hoàn hảo!


