# Chatbot Agentic Workflow Plan (LangChain4j)

## 1. Muc tieu
Nang cap `chatbot-service` tu mo hinh goi OpenAI thu cong sang Agentic Workflow voi LangChain4j, de chatbot co kha nang "think + act" thong qua viec goi cac API noi bo cua he thong microservices.

## 2. Pham vi thay doi
- `backend/services/chatbot-service/pom.xml`
- Tao moi:
  - `backend/services/chatbot-service/src/main/java/com/fashion/chatbotservice/agent/FashionAgent.java`
  - `backend/services/chatbot-service/src/main/java/com/fashion/chatbotservice/agent/FashionTools.java`
  - `backend/services/chatbot-service/src/main/java/com/fashion/chatbotservice/config/AgentConfig.java`
  - `backend/services/chatbot-service/src/main/java/com/fashion/chatbotservice/agent/KnowledgeTools.java`
  - `backend/services/chatbot-service/src/main/java/com/fashion/chatbotservice/service/KnowledgeBaseService.java`
  - `backend/services/chatbot-service/src/main/java/com/fashion/chatbotservice/service/KnowledgeIngestionService.java`
  - `backend/services/chatbot-service/src/main/java/com/fashion/chatbotservice/model/KnowledgeDocument.java`
- Refactor:
  - `backend/services/chatbot-service/src/main/java/com/fashion/chatbotservice/service/ChatbotService.java`
  - `backend/services/chatbot-service/src/main/java/com/fashion/chatbotservice/controller/ChatbotController.java`
- Cau hinh:
  - `backend/services/chatbot-service/src/main/resources/application.yml`
  - `backend/services/chatbot-service/src/main/resources/knowledge/faq.md`
  - `backend/services/chatbot-service/src/main/resources/knowledge/policy.md`

## 3. Yeu cau ky thuat can dat
### 3.1 Dependency
Them cac thu vien LangChain4j can thiet:
- `langchain4j`
- `langchain4j-open-ai`
- `langchain4j-spring-boot-starter`
- `langchain4j-embedding-store-pgvector` (neu dung PostgreSQL + pgvector)

Them dependency phuc vu parse/ingestion tai lieu:
- Jackson (JSON/YAML)
- Apache Commons IO (neu can doc file lon)

### 3.2 AI Service Interface
Tao `FashionAgent`:
- Dung `@AiService`
- Co `@SystemMessage` bang tieng Viet voi cac rule:
  - Bat buoc dung tool tim san pham truoc khi tra loi cau hoi lien quan den san pham/gia/tinh san co
  - Chi tra loi dua tren du lieu tool tra ve
  - Neu tool khong co du lieu, phai noi ro gioi han
  - Phong cach than thien, chuyen nghiep, ngan gon, tieng Viet

### 3.3 Tool Calling
Tao `FashionTools` voi cac `@Tool`:
1. Tim san pham:
   - Goi `product-service` (`/api/v1/products`) theo `categoryId`, `search`, `minPrice`, `maxPrice`
2. Kiem tra don hang:
   - Goi `order-service` theo ma don (`/api/v1/orders/by-number/{orderNumber}`)
   - Ho tro xem lich su theo user (`/api/v1/orders`, header `X-User-Id`)
3. Validate coupon:
   - Goi `promotion-service` (`POST /api/v1/promotions/validate?code=...&orderAmount=...`)

Yeu cau chung cho tools:
- Dung `WebClient` (uu tien theo hien trang chatbot-service)
- Co timeout + xu ly loi + message fallback ro rang
- Tra ket qua duoi dang text/JSON ngan gon de LLM de su dung

### 3.4 Knowledge Base (bat buoc de tang do chinh xac)
Muc tieu: chatbot tra loi chinh sach/huong dan/FAQ dua tren nguon du lieu co kiem soat, khong doan.

Nguon du lieu KB toi thieu:
- FAQ CSKH (giao hang, doi tra, bao hanh, thanh toan)
- Chinh sach khuyen mai va dieu kien ap ma
- Huong dan size, chat lieu, bao quan san pham

Yeu cau ky thuat KB:
- Ingestion job: nap tai lieu tu `resources/knowledge` (pha 1), co the mo rong tu DB/CMS o pha sau
- Chunking: tach doan 300-800 tokens, overlap 50-100 tokens
- Metadata bat buoc: `source`, `title`, `lastUpdatedAt`, `version`, `topic`
- Embedding + Retrieval: semantic search topK=3..5
- Citation: cau tra loi phai kem nguon (`title` + `source`)
- Guardrail: neu retrieval khong du tin cay, agent phai noi ro "khong tim thay thong tin trong knowledge base"

Kien truc de xuat:
- Pha 1 (nhanh): In-memory embedding store de validate luong
- Pha 2 (production): PostgreSQL + pgvector cho do ben va kha nang scale

### 3.5 Tool cho Knowledge Retrieval
Tao `KnowledgeTools` voi `@Tool`:
1. `searchKnowledge(query)`
  - Goi `KnowledgeBaseService` de tim context lien quan
  - Tra ve trich doan ngan + metadata nguon
2. `answerFromKnowledge(query)`
  - Tong hop top context + danh sach citation
  - Chi dung cho cau hoi phi realtime (policy/faq/guide)

### 3.6 Memory Management
Cau hinh `ChatMemory` theo tung `sessionId`:
- Dung `ChatMemoryProvider`
- Dung `MessageWindowChatMemory` voi gioi han so message (VD: 20)
- Session key uu tien: `sessionId`; fallback theo `userId` neu can

### 3.7 Refactor Service + Controller
- `ChatbotService`:
  - Bo logic goi OpenAI bang WebClient thu cong
  - Chuyen sang goi `fashionAgent.chat(sessionId, userMessage)`
  - Bo sung router y dinh:
    - Cau hoi realtime (san pham, don hang, coupon) -> uu tien `FashionTools`
    - Cau hoi tri thuc/chinh sach -> uu tien `KnowledgeTools`
- `ChatbotController`:
  - Nhan payload co `message`, `sessionId` (va co the `userId`)
  - Tra ve `reply` + `sessionId`

### 3.8 Prompt Engineering
System prompt phai the hien ro:
- Vai tro: tu van thoi trang chuyen nghiep
- Hanh vi goi tool truoc khi ket luan
- Khong hallucinate, khong tu suy dien ngoai du lieu he thong
- Ngan gon, lich su, tieng Viet

Rule bo sung cho do chinh xac:
- Cau hoi policy/faq/huong dan: phai goi `searchKnowledge` truoc khi tra loi
- Cau hoi realtime: phai goi tool microservice truoc khi tra loi
- Neu khong co du lieu tu tool/KB: tra loi trung thuc ve gioi han, khong tu sang tao thong tin
- Uu tien tra loi co citation cho cau hoi tu KB

## 4. Trinh tu implementation de code (sau khi duoc phe duyet)
1. Cap nhat `pom.xml`.
2. Tao `FashionAgent.java`.
3. Tao `FashionTools.java`.
4. Tao model + service cho KB (`KnowledgeDocument`, `KnowledgeBaseService`, `KnowledgeIngestionService`).
5. Tao `KnowledgeTools.java`.
6. Tao `AgentConfig.java` de wiring model + tools + memory + embedding/retriever.
7. Refactor `ChatbotService.java` (intent router realtime vs KB).
8. Refactor `ChatbotController.java`.
9. Cap nhat `application.yml` (OpenAI + service base URLs + memory + KB config).
10. Tao tai lieu mau trong `resources/knowledge`.
11. Compile module `chatbot-service` de validate.

## 5. Acceptance Criteria
- Build compile thanh cong cho module `chatbot-service`.
- Chatbot co the:
  - Goi tool tim san pham khi nguoi dung hoi san pham
  - Goi tool tra cuu don hang khi nguoi dung hoi trang thai don
  - Goi tool coupon khi nguoi dung hoi ma giam gia
  - Goi tool knowledge retrieval khi nguoi dung hoi chinh sach/huong dan/FAQ
- Cung mot `sessionId` giu duoc context hoi thoai.
- Cau tra loi bang tieng Viet, ngan gon, va dua tren du lieu tool/KB.
- Cau tra loi tu KB co kem citation nguon.
- Neu khong tim thay du lieu: chatbot noi ro gioi han, khong hallucinate.
- Khi co `userId`, agent co kha nang ca nhan hoa goi y san pham theo profile/hanh vi gan day.

## 6. Rui ro va giam thieu
- Sai contract API giua cac service:
  - Giam thieu: doi chieu endpoint + test bang Postman/curl truoc khi finalize
- Loi timeout khi goi lien service:
  - Giam thieu: dat timeout + fallback message
- Model bo qua tool:
  - Giam thieu: siet chat SystemMessage + mo ta `@Tool` ro rang
- KB stale/outdated:
  - Giam thieu: metadata version + lastUpdatedAt, co quy trinh cap nhat dinh ky
- Retrieval sai ngu canh:
  - Giam thieu: toi uu chunk size, topK, them rule confidence threshold + fallback trung thuc

## 7. Ke hoach test sau khi code
- Case 1: "Ao so mi den duoi 500k" -> bat buoc goi tool search san pham
- Case 2: "Kiem tra don ORD-xxxx" -> goi tool order by number
- Case 3: "Ma SALE20 ap dung duoc cho don 1 trieu khong?" -> goi tool validate coupon
- Case 4: Chat nhieu luot cung `sessionId` -> agent nho duoc ngu canh
- Case 5: 1 service down -> chatbot tra loi loi than thien, khong hallucinate
- Case 6: "Chinh sach doi tra trong bao nhieu ngay?" -> bat buoc goi knowledge retrieval + tra loi co citation
- Case 7: Cau hoi ngoai KB -> chatbot noi ro khong co thong tin trong knowledge base
- Case 8: Cap nhat file policy -> ingestion lai -> cau tra loi phan anh noi dung moi

## 8. Deliverables sau giai doan code
- `pom.xml` da cap nhat dependency
- `FashionAgent.java`
- `FashionTools.java`
- `KnowledgeTools.java`
- `KnowledgeBaseService.java`
- `KnowledgeIngestionService.java`
- `KnowledgeDocument.java`
- `AgentConfig.java`
- `ChatbotService.java` refactor
- `ChatbotController.java` refactor
- `application.yml` bo sung cau hinh
- Thu muc `resources/knowledge` voi FAQ/Policy mau
- Mo ta ngan co che "agent decide tool" va "realtime tool vs knowledge tool"

## 9. Cau hinh goi y cho Knowledge Base (pha 1)
```yaml
chatbot:
  knowledge:
    enabled: true
    data-path: classpath:knowledge
    top-k: 4
    min-score: 0.65
    chunk-size: 500
    chunk-overlap: 80
    citation-enabled: true
```

## 10. Dinh huong roadmap
- Sprint 1: Hoan thanh KB co ban + citation + test regression
- Sprint 2: Chuyen sang pgvector, them ingestion tu nguon dong (DB/CMS)
- Sprint 3: Ket hop hanh vi nguoi dung de ca nhan hoa goi y (van giu rule khong hallucinate)

## 11. Checklist trien khai 2 tuan (uu tien do chinh xac)
### Tuan 1 - Xay nen tang do tin cay
Ngay 1:
- Chot taxonomy intent: `realtime_product`, `realtime_order`, `realtime_coupon`, `knowledge_policy`, `knowledge_faq`, `unknown`
- Chot bo response contract JSON: `answer`, `sources`, `confidence`, `nextAction`

Ngay 2:
- Tao bo du lieu KB toi thieu (FAQ + policy + huong dan)
- Chuan hoa metadata: `source`, `title`, `topic`, `version`, `lastUpdatedAt`

Ngay 3:
- Implement ingestion + chunking + embedding (pha 1 in-memory)
- Cau hinh retrieval topK, minScore, citation bat buoc

Ngay 4:
- Implement intent router truoc agent
- Rule: realtime -> `FashionTools`, tri thuc -> `KnowledgeTools`

Ngay 5:
- Them confidence gate + safe fallback
- Them guardrail: khong citation thi khong dua ket luan chac chan

Output ket thuc tuan 1:
- Agent tra loi co citation cho cau hoi KB
- Agent fallback trung thuc khi score thap
- Dashboard log toi thieu: intent, tool da goi, score, sources

### Tuan 2 - Nang chat luong va kiem chung
Ngay 6:
- Tao bo golden set 200-300 cau hoi that (co dap an chuan + nguon chuan)
- Phan nhom theo intent va do kho

Ngay 7:
- Them rerank sau retrieval (neu co)
- Tuning chunk-size/chunk-overlap/topK/minScore theo ket qua golden set

Ngay 8:
- Them response verification (self-check): cau tra loi co duoc support boi source hay khong
- Neu khong support du -> ha confidence + fallback/hoi lai user

Ngay 9:
- Human-in-the-loop cho case confidence thap hoac case nhay cam (doi tra/hoan tien)
- Them flow chuyen CSKH voi transcript tom tat

Ngay 10:
- Chay regression full bo test + bao cao KPI
- Chot baseline cho production rollout

Output ket thuc tuan 2:
- Bao cao chat luong theo intent
- Rule set on dinh cho fallback/escalation
- Plan nang cap pha 2 (pgvector + ingestion DB/CMS)

## 12. KPI muc tieu de do "tu van dung hon"
- Intent routing accuracy >= 92%
- Retrieval hit@3 >= 85%
- Citation coverage (cau hoi KB co nguon) >= 95%
- Hallucination rate <= 3%
- Fallback dung cach (khi thieu du lieu) >= 98%
- Escalation case nhay cam duoc xu ly dung luong >= 99%

## 13. Cong thuc ra quyet dinh tra loi
- Neu `intent` la realtime -> goi tool microservice
- Neu `intent` la knowledge -> goi KB retrieval
- Neu `confidence < minScore` hoac `sources = rong` -> fallback trung thuc + hoi lam ro
- Neu case nhay cam va confidence thap -> chuyen human-in-the-loop

## 14. Kien truc ca nhan hoa theo tung user
Muc tieu: tang do phu hop cua goi y san pham ma van dam bao thong tin dung tu tool/KB.

Thanh phan du lieu can co:
- User profile co cau truc: `userId`, `sizePreference`, `priceRange`, `favoriteCategories`, `favoriteBrands`, `colorPreference`
- User behavior events: `view_product`, `add_to_cart`, `purchase`, `remove_from_cart`, `search_query`
- Profile tong hop theo cua so thoi gian (7/30/90 ngay)

Thanh phan dich vu:
- `UserProfileService`: doc/ghi profile va feature theo `userId`
- `RecommendationService`: rerank danh sach san pham tra ve tu `product-service`
- `PersonalizationTools` (tuy chon):
  - `getUserPreference(userId)`
  - `personalizeProducts(userId, products)`

Nguyen tac ranking:
- Candidate retrieval: lay danh sach san pham phu hop theo query tu `product-service`
- Re-ranking score (pha 1 rule-based):
  - +diem neu trung danh muc ua thich
  - +diem neu trong tam gia user thuong mua
  - +diem neu co hanh vi xem/mua gan day
  - -diem neu vuot xa budget lich su
- Top N sau rerank duoc dua vao cau tra loi

Guardrail cho do chinh xac:
- Facts (gia, ton kho, khuyen mai) phai lay tu tool realtime, khong lay tu profile cu
- Neu khong co `userId` hoac profile rong -> fallback ve ranking mac dinh
- Khong suy doan thong tin nhay cam cua user

## 15. Task chi tiet cho Sprint 3 (Personalization)
1. Data tracking
- Bo sung event tracking tai frontend va backend cho cac hanh vi mua sam chinh
- Dinh nghia schema event thong nhat va luu timestamp

2. Profile aggregation
- Tao job tong hop profile theo `userId` (batch moi 1h hoac near-real-time)
- Luu profile vao bang rieng de truy van nhanh

3. Recommendation pipeline
- Implement `RecommendationService` de rerank danh sach san pham
- Tich hop vao flow tra loi khi intent la tu van san pham

4. Prompt va tool policy
- Prompt bat buoc neu co `userId` thi uu tien de xuat theo profile
- Van phai trich xuat facts tu `FashionTools` truoc khi ket luan

5. Experiment va rollout
- Bat dau voi 10-20% traffic (feature flag)
- A/B test giua non-personalized va personalized

## 16. KPI rieng cho ca nhan hoa
- Recommendation CTR uplift >= 8%
- Add-to-cart rate uplift >= 5%
- Conversion uplift >= 3%
- Ty le user co profile hop le >= 85%
- Ty le fallback ve non-personalized < 20%
- Khong tang hallucination rate so voi baseline Sprint 2
