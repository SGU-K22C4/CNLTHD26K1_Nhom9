# 🛍️ Fashion E-Commerce Platform — Nhóm 9

A full-stack microservices e-commerce application with a React frontend and Spring Boot backend services managed by Docker Compose.

---

## 🏗️ System Architecture

```
React Frontend (Vite)
        │
        ▼
  API Gateway (:8080)
        │
        ├── user-service     (:8081) → MySQL: fashion_user_db
        ├── product-service  (:8082) → MySQL: fashion_product_db
        ├── cart-service     (:8083) → Redis
        ├── order-service    (:8084) → MySQL: fashion_order_db
        ├── promotion-service(:8085) → MySQL: fashion_promotion_db
        ├── review-service   (:8086) → MySQL: fashion_review_db
        └── chatbot-service  (:8087)
        
Infrastructure: MySQL 8.0 (:3307) | Redis 7.0 (:6379)
```

---

## ⚙️ Prerequisites

Make sure you have the following installed:

| Tool | Version | Download |
|---|---|---|
| **Docker Desktop** | Latest | [docker.com](https://www.docker.com/products/docker-desktop/) |
| **Node.js** | 18+ | [nodejs.org](https://nodejs.org/) |

> 💡 **Java & Maven are NOT required** on your machine — Docker builds everything inside containers.

---

## 🚀 Quick Start (First Time Setup)

### Step 1: Clone the repository
```bash
git clone https://github.com/phucmanh1310/CNLTHD26K1_Nhom9.git
cd CNLTHD26K1_Nhom9
```

### Step 2: Configure Environment Variables

**Linux / macOS:**
```bash
cp docker/.env.example docker/.env
```

**Windows (PowerShell):**
```powershell
Copy-Item docker\.env.example docker\.env
```

Then open `docker/.env` and fill in **required** fields:

| Variable | Required | Description |
|---|---|---|
| `MYSQL_ROOT_PASSWORD` | ✅ | Default: `huytran123` |
| `JWT_SECRET` | ✅ | Generate: `openssl rand -base64 32` |
| `MAIL_USERNAME` | ⚠️ | Gmail address (for email verification) |
| `MAIL_PASSWORD` | ⚠️ | Gmail App Password ([guide](https://myaccount.google.com/apppasswords)) |
| `VNPAY_TMN_CODE` | ⚠️ | VNPay Sandbox terminal code |
| `VNPAY_HASH_SECRET` | ⚠️ | VNPay Sandbox hash secret |
| `OPENAI_API_KEY` | ⚠️ | Only needed for chatbot feature |

> ⚠️ fields are optional (the app still runs without them, specific features just won't work)

### Step 3: Launch all backend services
```bash
docker compose -f docker/docker-compose.yml up -d --build
```

This will:
- Start **MySQL** and **Redis** containers
- Build and start all **8 Spring Boot microservices**
- Automatically create all databases and **import seed data** (products, users, etc.)
- ⏱️ First build takes ~5–10 minutes

### Step 4: Launch the Frontend
```bash
cd ecommerce-frontend
npm install
npm run dev
```

Open your browser: **[http://localhost:5173](http://localhost:5173)**

---

## 🌐 Service Endpoints

| Service | URL |
|---|---|
| **Frontend** | http://localhost:5173 |
| **API Gateway** | http://localhost:8080 |
| **User Service** | http://localhost:8081 |
| **Product Service** | http://localhost:8082 |
| **Cart Service** | http://localhost:8083 |
| **Order Service** | http://localhost:8084 |
| **Promotion Service** | http://localhost:8085 |
| **Review Service** | http://localhost:8086 |

---

## 💳 Test Accounts

### Admin / Demo User
| Field | Value |
|---|---|
| Email | `admin@fashion.com` |
| Password | `admin123` |

### VNPay Sandbox Test Card (NCB Bank)
| Field | Value |
|---|---|
| Card Number | `9704198526191432198` |
| Card Holder | `NGUYEN VAN A` |
| Expiry Date | `07/15` |
| OTP | `123456` |

---

## 🔧 Common Commands

```bash
# View logs of a specific service
docker compose -f docker/docker-compose.yml logs -f order-service

# Restart a specific service (e.g., after code change)
docker compose -f docker/docker-compose.yml restart order-service

# Stop all services
docker compose -f docker/docker-compose.yml down

# ⚠️ FULL RESET — deletes all data and rebuilds from scratch
docker compose -f docker/docker-compose.yml down -v
docker compose -f docker/docker-compose.yml up -d --build
```

---

## 📂 Project Structure

```
CNLTHD26K1_Nhom9/
├── backend/
│   ├── pom.xml               # Parent POM (multi-module Maven)
│   ├── common/               # Shared module
│   ├── api-gateway/          # Spring Cloud Gateway
│   └── services/
│       ├── user-service/
│       ├── product-service/
│       ├── cart-service/     # Redis-based cart
│       ├── order-service/    # Orders + VNPay integration
│       ├── promotion-service/
│       ├── review-service/
│       └── chatbot-service/
├── docker/
│   ├── docker-compose.yml
│   ├── .env.example          # ← Copy to .env and fill in your values
│   └── mysql/init/
│       ├── 01-create-databases.sql
│       └── 02-seed-data.sql  # ← Auto-imported on first startup
├── ecommerce-frontend/       # React + Vite
└── README.md
```

---

## 🐛 Troubleshooting

**MySQL seed data not loading?**
> The init scripts only run when the Docker volume is first created. If you already have an old volume:
```bash
docker compose -f docker/docker-compose.yml down -v
docker compose -f docker/docker-compose.yml up -d
```

**A service failed to start?**
> Check its logs:
```bash
docker compose -f docker/docker-compose.yml logs <service-name>
```

**Port already in use?**
> Stop any local MySQL/Redis running on the same ports, or edit `docker-compose.yml` to change the port mappings.

**Docker build failed?**
> Make sure Docker Desktop is running and has at least 4GB of memory allocated.