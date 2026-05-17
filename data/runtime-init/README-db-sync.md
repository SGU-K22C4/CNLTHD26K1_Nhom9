# DB Sync Scripts

## Muc dich

Bo script nay dung de day du lieu tu local/dev len server theo 2 nhom:

- MySQL:
  - `fashion_user_db`
  - `fashion_product_db`
  - `fashion_order_db`
  - `fashion_promotion_db`
- Mongo:
  - `fashion_review_db`
  - `fashion_chatbot_db`

## Chuan bi

1. Copy `db-sync.env.example` thanh `db-sync.env`
2. Dien dung:
   - MySQL local / server
   - Mongo local / server
3. Dam bao may da co tool:
   - `docker`
   - `mysqldump`
   - `mysql`
   - `mongodump`
   - `mongorestore`

## Chay MySQL

Preview truoc:

```powershell
powershell -ExecutionPolicy Bypass -File .\data\runtime-init\push-local-mysql-to-server.ps1 -EnvFile .\data\runtime-init\db-sync.env -WhatIf
```

Day 1 DB:

```powershell
powershell -ExecutionPolicy Bypass -File .\data\runtime-init\push-local-mysql-to-server.ps1 -EnvFile .\data\runtime-init\db-sync.env -Databases fashion_product_db
```

Day tat ca 4 DB:

```powershell
powershell -ExecutionPolicy Bypass -File .\data\runtime-init\push-local-mysql-to-server.ps1 -EnvFile .\data\runtime-init\db-sync.env
```

## Chay Mongo

Preview truoc:

```powershell
powershell -ExecutionPolicy Bypass -File .\data\runtime-init\push-local-mongo-to-server.ps1 -EnvFile .\data\runtime-init\db-sync.env -WhatIf
```

Day chi review DB:

```powershell
powershell -ExecutionPolicy Bypass -File .\data\runtime-init\push-local-mongo-to-server.ps1 -EnvFile .\data\runtime-init\db-sync.env -Targets review
```

Day ca review + chatbot:

```powershell
powershell -ExecutionPolicy Bypass -File .\data\runtime-init\push-local-mongo-to-server.ps1 -EnvFile .\data\runtime-init\db-sync.env
```

## Luu y

- Script MySQL se import de len server DB cung ten.
- Script Mongo dung `--drop` khi restore de tranh document cu va moi tron nhau.
- Chi chay len server khi ban chac chan server do la moi truong dung.
- Neu server la production, nen backup truoc khi restore.
