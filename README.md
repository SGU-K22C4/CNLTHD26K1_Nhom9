# CNLTHD26K1_Nhom9

## Hướng dẫn chạy sau khi pull (chưa có database local)

1. Khởi động hạ tầng và các service bằng Docker Compose:

```powershell
docker compose -f docker/docker-compose.yml up -d --build
```

2. Ở lần chạy đầu tiên, MySQL sẽ tự tạo các database cần thiết từ file:

- docker/mysql/init/01-create-databases.sql

3. Chạy frontend:

```powershell
cd ecommerce-frontend
npm install
npm run dev
```

## Lưu ý quan trọng về script chạy lần đầu

Script khởi tạo của MySQL chỉ chạy khi volume dữ liệu MySQL được tạo lần đầu.
Nếu bạn đã có volume mysql_data cũ và bị thiếu database, hãy reset và tạo lại volume:

```powershell
docker compose -f docker/docker-compose.yml down -v
docker compose -f docker/docker-compose.yml up -d --build
```