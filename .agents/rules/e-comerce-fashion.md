---
trigger: always_on
---

# SYSTEM ROLE: Senior Fullstack Engineer (Microservices & AI)

# 1. Ngữ cảnh dự án (Project Context)

* Tên dự án: Website Thương mại Điện tử Thời trang tích hợp Agentic AI
* Mục tiêu: Xây dựng hệ thống Microservices hoàn chỉnh, có khả năng mở rộng, sử dụng Spring Boot và React
* Đối tượng:
  * Customer (Khách hàng)
  * Guest (khách mời chưa login truy cập Web)
  * Admin (Quản trị viên)

# 2. Tech Stack Core

## Backend
* Java 17
* Spring Boot 3.x (Spring Web, Data JPA, Security)
* Maven

## Frontend
* React 18
* Vite
* TailwindCSS
* Axios
* React Router v6

## API Gateway
* Kong Gateway
  * JWT Authentication
  * Rate Limiting
  * CORS

## Communication

### Đồng bộ (Sync)
* REST API
* Feign Client / RestTemplate

### Bất đồng bộ (Async)
* Apache Kafka (Event-driven)

# 3. Database per Service

* User Service: MySQL
* Product Service: MySQL
* Order Service: MySQL
* Promotion Service: MySQL
* Review Service: MongoDB
* Chatbot Service: MongoDB
* Payment Service: MongoDB
* Cart Service: Redis (TTL)

# 4. AI & DevOps

* Google Gemini API Pro 1.5
* Docker
* Kubernetes (K8s)
* GitHub Actions

# 5. Quy tắc kiến trúc & Nghiệp vụ

## Tách biệt Service
* User
* Product
* Cart
* Order
* Promotion
* Review
* Chatbot

## Xác thực (Auth)
* Kong Gateway là entry point duy nhất
* Inject headers:
  * X-User-Id
  * X-User-Role

## Nhất quán dữ liệu

### Sync (REST)
* Kiểm tra tồn kho → Product Service
* Kiểm tra điểm tích lũy → Promotion Service

### Async (Kafka)
* order.delivered
  * Mở quyền đánh giá
  * Cộng điểm thưởng

# 6. Tiêu chuẩn phát triển

## Backend
* Kiến trúc Layered:
  * Controller
  * Service
  * Repository
* Sử dụng DTO cho Request/Response
* Xử lý Exception tập trung

## Frontend
* Functional Components
* React Hooks
* Mobile-first design

## Database
* Single Source of Truth
* Không truy cập DB của service khác