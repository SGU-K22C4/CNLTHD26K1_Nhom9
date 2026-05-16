# Chatbot Current Setup

## 1. Muc dich tai lieu

Tai lieu nay mo ta setup hien tai cua chatbot tu van ban hang trong du an theo runtime thuc te.

Muc tieu:

- giup AI va developer hieu nhanh chatbot FE + BE dang hoat dong the nao
- chi ro model/provider dang dung that
- mo ta luong tool-calling, memory, knowledge base, GraphRAG
- danh dau cac diem lech giua config cu va runtime hien tai

## 2. Tong quan vai tro cua chatbot

`chatbot-service` hien tai khong phai mot model AI train rieng trong repo, ma la mot lop orchestration gom:

- frontend widget + full chatbot page
- backend Spring Boot API
- LangChain4j agent
- model provider theo OpenAI-compatible API
- MongoDB luu session, knowledge, analytics, preference
- tool-calling sang cac microservice khac de lay du lieu that

No dong vai tro mot "AI Sales Advisor" cho website fashion, tap trung vao:

- tu van san pham
- tu van size
- goi y outfit
- kiem tra khuyen mai
- wishlist / loyalty / review
- hoi dap policy va FAQ

## 3. Frontend setup hien tai

Frontend chatbot nam trong:

- `ecommerce-frontend/src/modules/chatbot/components/ChatWidget.jsx`
- `ecommerce-frontend/src/modules/chatbot/components/ChatMessage.jsx`
- `ecommerce-frontend/src/modules/chatbot/hooks/useChatbot.js`
- `ecommerce-frontend/src/modules/chatbot/services/chatbotService.js`
- `ecommerce-frontend/src/modules/chatbot/pages/ChatbotPage.jsx`

Flow FE hien tai:

1. `ChatWidget` duoc mount global trong `ecommerce-frontend/src/App.jsx`, nen chatbot mini-widget xuat hien toan site.
2. Route `/chatbot` duoc khai bao trong `ecommerce-frontend/src/routes/AppRoutes.jsx` va dang bi bao ve boi `PrivateRoute`.
3. Widget mini van cho phep chat nhanh ngay ca khi chua login, nhung full page chatbot thi yeu cau dang nhap.
4. `chatbotService.js` luu `sessionId` vao `localStorage` theo key `chatbot_session_<userId>` hoac `chatbot_session_guest`.
5. `useChatbot.js` tu hydrate lich su chat tu backend qua API `GET /api/v1/chatbot/sessions/{sessionId}` neu user da co session.
6. Khi login/logout, hook chu dong reset session de tranh cross-contamination giua guest va user that.
7. FE gui request chat qua `POST /api/v1/chatbot/chat`, kem `message`, `sessionId`, `preferences`.
8. FE render text message, product suggestions, promotions va `missingFields` trong cung mot conversation stream.

Y nghia:

- FE da duoc thiet ke theo huong chat UI + product cards, khong chi la text bot thong thuong.
- Session continuity phu thuoc vao `localStorage` + Mongo session store o backend.

## 4. Backend API va orchestration

Backend chatbot nam o:

- `backend/services/chatbot-service`

API chinh:

- `POST /api/v1/chatbot/chat`
- `GET /api/v1/chatbot/sessions/{sessionId}`

`ChatbotController` chi la entrypoint mong, con orchestration chinh nam trong `ChatbotServiceImpl`.

Flow backend thuc te:

1. Nhan `message`, `sessionId`, `X-User-Id`.
2. Tim session trong Mongo hoac tao session moi.
3. Merge user preferences tu request vao `PreferenceProfile`.
4. Detect cold start de hoi lam ro neu user hoi qua chung.
5. Enrich profile tu:
   - noi dung message
   - purchase history
   - wishlist
   - user profile
6. Classify out-of-domain som de chan query ngoai pham vi shop truoc khi goi LLM.
7. Inject them context gio hang hien tai tu `cart-service`.
8. Goi agent de model tu quyet dinh tool nao can dung.
9. Validate reply qua guardrail.
10. Persist user message + bot reply vao Mongo.
11. Ghi analytics va cap nhat long-term preference profile.

Can luu y:

- Code hien tai uu tien duong `agentic AI`.
- Heuristic fallback van ton tai, nhung chu yeu la safety net khi LLM/agent loi hoac tra ve rong.
- Session user guest se duoc map thanh `guest-<prefix cua sessionId>` neu khong co `X-User-Id`.

## 5. Model / provider dang dung that

Day la diem de gay hieu nham neu chi doc file config repo cu.

Runtime code hien tai dang dung:

- `LangChain4j`
- `OpenAiChatModel`
- endpoint OpenAI-compatible

File quyet dinh provider thuc te:

- `backend/services/chatbot-service/src/main/java/com/fashion/chatbotservice/config/AgentConfig.java`
- `backend/services/chatbot-service/src/main/resources/application.yml`

Provider dang duoc code de chay:

- `ai.base-url`
- `ai.api-key`
- `ai.model`

Gia tri mac dinh hien tai:

- `AI_BASE_URL=https://llm.chiasegpu.vn/v1`
- `AI_MODEL=deepseek-3.2`

Dieu nay co nghia:

- Chatbot hien tai khong con phu thuoc truc tiep vao `OllamaChatModel` trong runtime chinh.
- No dang goi mot model ben ngoai thong qua OpenAI-compatible API.
- Ve ban chat, day la inference orchestration + prompt + tools, khong phai custom-trained model duoc train trong chinh repo.

## 6. Trang thai cua Ollama trong du an

`Ollama` chua bien mat hoan toan khoi codebase, nhung hien tai dang o trang thai legacy / transition:

- `pom.xml` van con dependency `langchain4j-ollama`
- `backend/config-repo/chatbot-service.yml` van con block `ollama.*`
- `application.yml` van giu comment config Ollama cu

Nhung:

- `AgentConfig` dang build `OpenAiChatModel`, khong build `OllamaChatModel`
- runtime path chinh hien tai theo `ai.*`, khong theo `ollama.*`

Ket luan:

- neu AI hoac dev moi chi doc `config-repo/chatbot-service.yml`, rat de hieu sai rang chatbot dang chay bang Ollama
- thuc te runtime da chuyen sang DeepSeek/OpenAI-compatible endpoint

## 7. Tool-calling va du lieu realtime dang dung

`FashionAgent` + `FashionTools` la trung tam cua kha nang tu van ban hang co du lieu that.

Agent prompt buoc model phai:

- goi tool truoc khi tra loi cac cau hoi ve san pham, gia, ton kho, order, promotion, review, loyalty
- goi `searchKnowledge` truoc khi tra loi policy / FAQ
- khong duoc bịa du lieu

`FashionTools` hien tai goi toi cac service khac de lay du lieu runtime:

- `product-service`
- `promotion-service`
- `order-service`
- `cart-service`
- `review-service`

Nhom tool chinh hien co:

- search product
- strict product lookup
- browse product
- list product types
- consult size
- suggest outfit
- check promotions
- get loyalty benefits
- get product reviews
- compare products
- save user preference
- search knowledge

Y nghia:

- chatbot nay la AI + rule + data integration, khong phai mot chat model doc lap
- chat tra loi dung hay sai phu thuoc rat nhieu vao tinh trang cac service phia sau

## 8. MongoDB schema va vai tro cua tung collection

`chatbot-service` dung MongoDB cho nhieu loai document hon review-service.

Nhom document chinh:

- `ChatSession`
  - luu lich su hoi dap
  - luu suggestion snapshots
  - luu promotion snapshots
  - luu `PreferenceProfile`
- `UserPreferenceDocument`
  - luu profile lau dai tach rieng khoi lifecycle cua session
- `KnowledgeDocument`
  - luu cac chunk FAQ / policy da ingest
- `KnowledgeGraphNode`
- `KnowledgeGraphEdge`
  - phuc vu GraphRAG retrieval
- `ChatAnalyticsDocument`
  - luu telemetry ve trace, latency, tool usage, outcome

Dieu nay co nghia:

- MongoDB cua chatbot khong chi luu chat history
- no con dong vai tro memory store, knowledge store, analytics store va preference store

## 9. Knowledge base va GraphRAG hien tai

Knowledge base local cua chatbot hien tai nam trong:

- `backend/services/chatbot-service/src/main/resources/knowledge/faq.md`
- `backend/services/chatbot-service/src/main/resources/knowledge/policy.md`

Flow hien tai:

1. Khi service startup, `KnowledgeIngestionServiceImpl` doc cac file `.md`.
2. File duoc chunk theo heading `##` / `###`.
3. Chunk duoc luu vao Mongo `KnowledgeDocument`.
4. `GraphRagServiceImpl` rebuild do thi node/edge dua tren:
   - topic
   - keyword
   - quan he adjacent
   - quan he related
5. `KnowledgeBaseServiceImpl` search theo hybrid scoring:
   - lexical overlap
   - GraphRAG score

Ket luan:

- chatbot hien tai co knowledge retrieval, nhung dang la lightweight GraphRAG tren Mongo
- khong thay vector database hoac embedding retrieval chuyen biet trong setup hien tai
- chat policy/FAQ phu thuoc vao chat luong cua 2 file markdown nay

## 10. Ca nhan hoa va memory

Chatbot hien tai da co nhieu lop nho ngu canh:

- `MessageWindowChatMemory` trong LangChain4j
- hydrate memory lai tu `ChatSession` trong Mongo
- `PreferenceProfile` o trong session
- `UserPreferenceDocument` de luu memory lau dai
- enrich them context tu purchase history, wishlist, user profile, cart

Tac dung thuc te:

- user quay lai co the duoc nho style, budget, size, focus
- session moi van co the bootstrap tu profile da persist
- model co them context gio hang khi tu van cross-sell / outfit

## 11. Nhung diem dang lech hoac can luu y

1. `backend/config-repo/chatbot-service.yml` dang lech runtime
   - file nay van noi theo `ollama.*`
   - trong khi code runtime dang doc `ai.*`

2. URI MongoDB dang bi hardcode trong `chatbot-service-dev.yml` va `chatbot-service-prod.yml`

## 12. Trang thai nang cap hien tai

Chatbot hien tai khong con o muc "chat demo" nua. No da co:

- FE card-based chat UI de dung cho e-commerce
- Mongo session + profile + knowledge + analytics
- agent tools cho product / order / promotion / wishlist / loyalty / review
- sales knowledge ingestion
- business ranking cho suggestions
- mot so co che giam drift multi-turn

Nhung danh gia thuc te hien tai van nen de o muc:

- `55% -> 65%`

No da:

- hieu duoc kha nhieu cau hoi
- tra loi duoc
- co the tu van co logic hon truoc

Nhung chua:

- giu mach hoi thoai dai that on dinh
- tu van outfit sau nhu stylist that
- quyet dinh thu tu de xuat theo business signals manh
- hoc duoc tu ket qua sau chat

## 13. Nhan dinh kien truc

Kien truc hien tai van nen duoc giu:

- `LangChain4j` orchestration
- `FashionTools` cho data realtime
- `MongoDB` cho session / knowledge / analytics
- `knowledge markdown` de iteration nhanh

Nhung neu muon chatbot hay hon ro ret, can them 3 lop nua:

1. Conversation state layer
2. Recommendation service / scorer rieng
3. Eval + feedback loop

Khong co 3 lop nay, chatbot se thuong bi:

- drift sau vai turn
- de xuat khong that su "co chu dich ban hang"
- kho do tien bo sau moi dot sua
   - ve van hanh va bao mat, nen dua dan vao env var / secret thay vi de credential ro trong repo

3. `/chatbot` full page dang bat dang nhap, nhung `ChatWidget` mini van cho phep guest chat
   - day la chu y quan trong khi mo rong logic guest/user va training data

4. Knowledge base hien tai moi co `faq.md` va `policy.md`
   - neu muon chatbot tu van ban hang sau hon, can bo sung them nguon knowledge co cau truc ro hon

5. Chatbot phu thuoc vao availability cua nhieu service phia sau
   - product
   - promotion
   - order
   - cart
   - review
   => chi can mot service loi la chat experience co the giam manh

6. K8s / CI-CD chi thuc su an config Atlas moi sau khi manifest va workflow da duoc push va deploy
   - neu chua push, pod co the van noi `mongo:27017` tu config cu

## 12. Ket luan nhanh cho AI va developer

Neu can hieu chatbot hien tai, dung doc no nhu mot model AI don le.

Can doc theo cach sau:

1. FE chatbot la sessioned UI co product-card rendering.
2. BE chatbot la orchestration layer Spring Boot + LangChain4j.
3. Model runtime hien tai la DeepSeek qua OpenAI-compatible API, khong phai Ollama runtime chinh.
4. MongoDB cua chatbot la storage da muc dich: session + memory + knowledge + analytics + preference.
5. Gia tri that cua chatbot den tu tool-calling sang microservices va knowledge markdown, khong den tu training model trong repo.
