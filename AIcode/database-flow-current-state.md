# Database Flow - Current State

## 1. Muc dich tai lieu

Tai lieu nay dung de giup AI va developer hieu nhanh database flow hien tai cua du an truoc khi cleanup SQL files va sap xep lai repo structure.

Muc tieu cua file nay:

- Xac dinh service nao dang dung loai database nao.
- Xac dinh file nao moi la source of truth cua schema.
- Tach biet du lieu migration chinh thuc va cac file SQL dump/mock dang de roi.
- Ghi chu cac diem loan trong structure hien tai de phuc vu cleanup o buoc sau.

## 2. Tong quan storage hien tai

Du an dang dung mo hinh polyglot persistence, khong phai tat ca service dung chung mot loai database.

### 2.1. MySQL

MySQL dang duoc dung cho 4 service:

- `user-service` -> `fashion_user_db`
- `product-service` -> `fashion_product_db`
- `order-service` -> `fashion_order_db`
- `promotion-service` -> `fashion_promotion_db`

Tham chieu:

- `backend/config-repo/user-service.yml`
- `backend/config-repo/product-service.yml`
- `backend/config-repo/order-service.yml`
- `backend/config-repo/promotion-service.yml`
- `docker/docker-compose.yml`
- `k8s/configmaps.yaml`
- `k8s/microservices.yaml`
- `k8s/databases.yaml`

### 2.2. MongoDB

MongoDB dang duoc dung cho 2 service:

- `review-service` -> `fashion_review_db`
- `chatbot-service` -> `fashion_chatbot_db`

Tham chieu:

- `backend/config-repo/review-service.yml`
- `backend/config-repo/chatbot-service.yml`
- `backend/config-repo/review-service-dev.yml`
- `backend/config-repo/review-service-prod.yml`
- `backend/config-repo/chatbot-service-dev.yml`
- `backend/config-repo/chatbot-service-prod.yml`
- `k8s/configmaps.yaml`

Luu y quan trong:

- Moi truong `dev` dang duoc dinh huong dung MongoDB Atlas shared de cac developer dung chung data.
- `docker/docker-compose.yml` khong con la runtime chinh cho MongoDB o luong dev.
- `k8s` cung da duoc dinh huong dung MongoDB Atlas thong qua Kubernetes Secret.
- `mongo` local chi con y nghia neu sau nay team chu dong bat lai che do offline/fallback.

### 2.3. Redis

Redis dang duoc dung cho:

- `cart-service`

Tham chieu:

- `backend/config-repo/cart-service.yml`
- `backend/config-repo/cart-service-dev.yml`
- `backend/config-repo/cart-service-prod.yml`
- `docker/docker-compose.yml`
- `k8s/configmaps.yaml`

## 3. Database mapping theo service

| Service | Storage | Database / Logical Store | Ghi chu |
| --- | --- | --- | --- |
| `user-service` | MySQL | `fashion_user_db` | Auth, user, address, token |
| `product-service` | MySQL | `fashion_product_db` | Product, category, variant, wishlist, inventory |
| `order-service` | MySQL | `fashion_order_db` | Order, order_item, payment-related data |
| `promotion-service` | MySQL | `fashion_promotion_db` | Coupon, loyalty, membership tier, point transaction |
| `review-service` | MongoDB | `fashion_review_db` | Review documents |
| `chatbot-service` | MongoDB | `fashion_chatbot_db` | Chat session, knowledge docs, analytics, preference |
| `cart-service` | Redis | key-value | Cart cache / cart state |

## 4. Runtime flow cau hinh database

### 4.1. Local / Docker

Khi chay local theo huong Docker, `docker/docker-compose.yml` dung:

- 1 MySQL container
- 1 Redis container

Trong local Docker:

- MySQL expose `3307 -> 3306`
- Redis expose `6379`

Moi service se nhan connection qua environment variables trong `docker/docker-compose.yml`.

Luu y:

- MySQL la mot server, nhung ben trong tach thanh nhieu database logic (`fashion_user_db`, `fashion_product_db`, `fashion_order_db`, `fashion_promotion_db`).
- `review-service` va `chatbot-service` chay local app nhung mac dinh dung cloud MongoDB trong moi truong `dev` thong qua config repo.
- Dieu nay giup cac developer dung chung data test va tranh sinh ra mot local MongoDB rieng khong can thiet.

### 4.2. Config Server / config-repo

`backend/config-repo` dang la noi giu cau hinh trung tam cho da so service.

Day la lop cau hinh quan trong nhat can doc khi AI can hieu database flow hien tai.

Pattern hien tai:

- `*-service.yml`: base config
- `*-service-dev.yml`: config dev
- `*-service-prod.yml`: config prod

Dieu nay dac biet quan trong voi:

- `review-service`
- `chatbot-service`

vi 2 service nay dung MongoDB Atlas o dev/prod thong qua `SPRING_DATA_MONGODB_URI`.

### 4.3. Kubernetes

`k8s/configmaps.yaml` dang map lai cac URL quan trong:

- `USER_DB_URL`
- `PRODUCT_DB_URL`
- `ORDER_DB_URL`
- `PROMOTION_DB_URL`
- `SPRING_DATA_REDIS_HOST`
- `SPRING_DATA_REDIS_PORT`

Ngoai ra file nay con chua `mysql-init-scripts` de tao 4 database MySQL rong khi khoi tao cum.

Luu y:

- `REVIEW_MONGODB_URI` va `CHATBOT_MONGODB_URI` cho `k8s` khong nen dat trong `ConfigMap`.
- 2 bien nay nen dat trong Kubernetes `Secret` vi co chua credential Atlas.
- `k8s/databases.yaml` khong con deploy MongoDB noi bo sau khi chuyen qua Atlas.

## 5. Source of truth cua schema

## Ket luan ngan

Neu can hieu schema that su cua du an, uu tien doc migration files trong tung service, khong uu tien doc `data/legacy-dumps/*`.

### 5.1. MySQL schema source of truth

Schema MySQL chinh thuc hien tai nam trong:

- `backend/services/user-service/src/main/resources/db/migration`
- `backend/services/product-service/src/main/resources/db/migration`
- `backend/services/order-service/src/main/resources/db/migration`
- `backend/services/promotion-service/src/main/resources/db/migration`

Danh sach migration da tim thay:

- `user-service`
  - `V1__init_user_auth.sql`
  - `V2__add_email_verification.sql`
  - `V3__align_users_legacy_columns.sql`
  - `V4__drop_district_from_addresses.sql`
- `product-service`
  - `V1__init_product.sql`
  - `V2__add_wishlist.sql`
  - `V3__seed_fashion_products.sql`
- `order-service`
  - `V1__init_order.sql`
  - `V2__alter_product_id_varchar.sql`
  - `V3__add_loyalty_fields.sql`
  - `V4__add_inventory_reserved.sql`
- `promotion-service`
  - `V1__init_promotion.sql`
  - `V2__init_loyalty.sql`

Y nghia:

- Day moi la bo file phan anh schema va schema evolution chinh thuc.
- Khi reset database MySQL dung cach, can de Flyway chay cac migration nay.
- Cleanup sau nay khong duoc lam mat nhom file nay.

### 5.2. MongoDB schema source of truth

MongoDB khong co migration trung tam ro rang nhu MySQL.

Schema hien tai dang the hien qua:

- Document classes / model classes trong source code
- Mongo repository
- Index annotations trong entity/model

Vi du:

- `backend/services/review-service/src/main/java/.../entity/Review.java`
- `backend/services/chatbot-service/src/main/java/.../model/*.java`

Dieu nay co nghia:

- Neu AI can hieu schema MongoDB, phai doc class Java, khong phai tim schema SQL.

### 5.3. Redis source of truth

Redis la key-value store, nen "schema" chu yeu nam trong code cua `cart-service` va config Redis.

## 6. Vai tro cua cac file SQL ngoai migration

### 6.1. `data/`

Thu muc `data/` sau cleanup se duoc tach thanh cac nhom ro nghia:

- `data/legacy-dumps/`
- `data/mock-seeds/`
- `data/runtime-init/`

Danh gia hien tai:

- Day khong nen duoc xem la source of truth chinh.
- Nhung file nay la dump, seed, mock data, hoac tai lieu ho tro import.
- `generate_mock_data.js` sinh mock SQL phuc vu du lieu mau.

Rui ro neu khong tach ro:

- AI co the hieu nham cac file dump/seed la schema chinh thuc.
- Schema trong migration va schema trong file dump co the lech nhau theo thoi gian.

### 6.2. `data/legacy-dumps/backup_all_local.sql`

File nay co kich thuoc lon va dang giong mot full local dump.

Danh gia hien tai:

- Nen xem day la du lieu backup / tham khao.
- Khong nen dung file nay lam schema authority.
- Khong nen de AI mac dinh sua theo file nay.

### 6.3. `data/mock-seeds/*`

Day la cac file mock / seed / support, khong phai migration chinh thuc.

## 7. Flow van hanh database hien tai

### 7.1. Khoi tao MySQL

Flow hien tai:

1. MySQL container / instance duoc khoi tao.
2. Cac database logic duoc tao san:
   - `fashion_user_db`
   - `fashion_product_db`
   - `fashion_order_db`
   - `fashion_promotion_db`
3. Moi service MySQL tu chay Flyway migration trong chinh service do.
4. Service khoi dong va truy cap dung database cua no.

Y nghia kien truc:

- Moi service tu so huu schema rieng.
- Khong co mot file SQL tong duy nhat lam trung tam cho tat ca MySQL services.

### 7.2. Khoi tao MongoDB

Flow hien tai:

1. Ở luong `dev`, `review-service` va `chatbot-service` ket noi truc tiep toi MongoDB Atlas qua `SPRING_DATA_MONGODB_URI`.
2. Collection va document duoc hinh thanh theo code va hanh vi runtime.
3. Index duoc quan ly qua annotation / Spring Data Mongo.
4. O luong `k8s`, review/chatbot cung duoc dinh huong ket noi Atlas qua Secret thay vi MongoDB noi bo.

### 7.3. Khoi tao Redis

Flow hien tai:

1. Redis duoc khoi tao.
2. `cart-service` ket noi qua host/port.
3. Cart state duoc luu dang key-value.

## 8. Diem loan trong repo structure lien quan den database

### 8.1. Nhieu nguon SQL nam o root-level va `data/`

Hien tai du an co 3 nhom file du lieu ho tro:

- `data/legacy-dumps/*`
- `data/mock-seeds/*`
- `docker/mysql/init/*`

Nhung migration that lai nam sau trong `backend/services/*/src/main/resources/db/migration`.

Van de:

- SQL business chinh thuc va SQL support tung bi tron.
- Nguoi moi vao repo rat de doc nham.

### 8.2. MongoDB config bi tach giua local va cloud

Mongo dang co:

- fallback local URI trong service `application.yml`
- Atlas URI trong `config-repo` dev/prod
- K8s Secret URI cho `review-service` va `chatbot-service`

Neu khong co note ro, AI de ket luan sai rang moi truong nao cung dung cung mot nguon MongoDB.

### 8.3. Config database bi phan tan o nhieu tang

Hien tai cau hinh DB nam o:

- `backend/services/*/src/main/resources/application.yml`
- `backend/config-repo/*.yml`
- `docker/docker-compose.yml`
- `k8s/configmaps.yaml`
- `k8s/databases.yaml`

Day la do dac diem deployment, nhung cung la nguon gay nhieu nham lan neu khong co tai lieu tong hop.

## 9. Nguyen tac cleanup de tranh pha flow hien tai

### 9.1. Thu tu uu tien khi doc database structure

AI nen uu tien doc theo thu tu:

1. `backend/config-repo/*`
2. `backend/services/*/src/main/resources/db/migration/*`
3. model/entity/repository cua Mongo services
4. `docker/docker-compose.yml`
5. `k8s/configmaps.yaml`
6. cuoi cung moi toi `data/legacy-dumps/*` va `data/mock-seeds/*`

Ly do:

- Day la thu tu phan anh "cau hinh dang chay" truoc, "du lieu ho tro" sau.

### 9.2. Nhung thu khong nen xoa khi cleanup

Khong duoc xoa hoac lam roi y nghia cua:

- `backend/services/*/src/main/resources/db/migration/*`
- `backend/config-repo/*`
- `docker/docker-compose.yml`
- `k8s/configmaps.yaml`
- `k8s/databases.yaml`

### 9.3. Nhung thu can danh dau de xem xet cleanup

Can xem xet gom nhom hoac doi cho:

- `data/legacy-dumps/*`
- `data/mock-seeds/*`

Khuyen nghi:

- Giu `docker/mysql/init/*` rieng vi day la runtime init files cho local Docker.
- Truoc khi xoa, can xac dinh file nao con dang duoc dung that trong quy trinh demo, mock, hoac backup.

## 10. Ket luan cho AI truoc khi refactor / cleanup

AI can nho 5 diem sau:

1. Du an nay khong dung mot database duy nhat, ma dung MySQL + MongoDB + Redis.
2. MySQL schema chinh thuc nam trong Flyway migration cua tung service.
3. MongoDB schema chinh thuc nam trong model/entity code, khong nam trong file SQL.
4. `data/legacy-dumps/*` va `data/mock-seeds/*` chi nen xem la dump/mock/seed/legacy cho den khi duoc xac nhan nguoc lai.
5. Cleanup structure database chi an toan khi tach ro "runtime config", "schema source of truth", va "legacy SQL assets".

## 11. Huong cleanup de xuat o buoc tiep theo

Buoc tiep theo hop ly sau tai lieu nay:

1. Lap danh sach chinh xac file SQL nao dang con duoc dung.
2. Danh dau file SQL nao la legacy, mock, backup, seed.
3. De xuat structure moi cho nhom file SQL support.
4. Sau khi xac nhan, moi bat dau move/xoa/thay doi structure repo.

## 12. Huong dan migrate du lieu MySQL tu local Docker len server

Phan nay ap dung cho giai doan hien tai khi:

- local dang chay MySQL bang Docker
- server tam thoi van dung MySQL local/on-host thay vi managed cloud
- MongoDB da tach qua Atlas, nhung MySQL van can dong bo bang dump/migration

### 12.1. Nguyen tac migrate

- Flyway migration van la source of truth cua schema.
- Dump SQL chi dung de chuyen data hoac snapshot data.
- Tren server moi, uu tien de service tao schema bang migration truoc.
- Sau do moi import data neu can giu lai du lieu local.

### 12.2. Truong hop migrate schema + data tu local len server

Buoc de xuat:

1. Tren local, dam bao cac service MySQL da chay migration day du.
2. Export tung database rieng tu MySQL local Docker:
   - `fashion_user_db`
   - `fashion_product_db`
   - `fashion_order_db`
   - `fashion_promotion_db`
3. Tren server, khoi tao MySQL va tao san 4 database rong.
4. Khoi dong service hoac chay migration de tao schema dung version hien tai.
5. Import data dump vao tung database tuong ung.
6. Khoi dong lai service va verify data.

### 12.3. Lenh mau export tu local Docker

Vi local MySQL dang chay trong Docker, co the dump bang mau lenh:

```powershell
docker exec fashion_mysql mysqldump -uroot -pYOUR_PASSWORD --databases fashion_user_db > user-db.sql
docker exec fashion_mysql mysqldump -uroot -pYOUR_PASSWORD --databases fashion_product_db > product-db.sql
docker exec fashion_mysql mysqldump -uroot -pYOUR_PASSWORD --databases fashion_order_db > order-db.sql
docker exec fashion_mysql mysqldump -uroot -pYOUR_PASSWORD --databases fashion_promotion_db > promotion-db.sql
```

Neu muon dump data ma bo qua bang version cua Flyway, co the tuy chinh them option theo nhu cau.

### 12.4. Lenh mau import len server

Sau khi copy file dump len server:

```powershell
mysql -h SERVER_HOST -u root -p fashion_user_db < user-db.sql
mysql -h SERVER_HOST -u root -p fashion_product_db < product-db.sql
mysql -h SERVER_HOST -u root -p fashion_order_db < order-db.sql
mysql -h SERVER_HOST -u root -p fashion_promotion_db < promotion-db.sql
```

### 12.5. Cach an toan hon cho moi truong moi

Neu server la moi truong moi hoan toan, cach an toan hon la:

1. Chay service hoac Flyway migration truoc de tao schema dung.
2. Dump chi phan data can thiet tu local.
3. Import data sau khi schema da dung version.

Ly do:

- Giam rui ro schema trong dump cu de len schema moi.
- Tranh lam sai lich su migration cua Flyway.

### 12.6. File nao nen dung cho migration data

Nen uu tien:

- dump moi tu MySQL local hien tai
- hoac cac file trong `data/legacy-dumps/` chi khi xac nhan chung van con dung

Khong nen coi:

- `backend/services/*/src/main/resources/db/migration/*` la file import data

Vi day la migration schema/application-level, khong phai full snapshot data.
