# 🛍️ Fashion E-Commerce Platform — Nhóm 9

A full-stack microservices e-commerce application with a React frontend and Spring Boot backend services managed by Docker Compose.

---

## 🏗️ System Architecture

```
React Frontend (Vite :5173)
        │
        ▼
  Kong Gateway (:8080)      ← High-performance API Gateway (Nginx/OpenResty)
  ├─ CORS plugin (global)      DB-less mode, declarative config
  ├─ Rate-Limiting (global)
  ├─ jwt-auth (custom Lua)
  │
  ├── /api/v1/auth/**      → user-service     (:8081) → MySQL: fashion_user_db
  ├── /api/v1/users/**     → user-service     (:8081) → [JWT protected]
  ├── /api/v1/products/**  → product-service  (:8082) → MySQL: fashion_product_db
  ├── /api/v1/categories/**→ product-service  (:8082)
  ├── /api/v1/wishlists/** → product-service  (:8082)
  ├── /api/v1/cart/**      → cart-service     (:8083) → Redis
  ├── /api/v1/orders/**    → order-service    (:8084) → MySQL: fashion_order_db
  ├── /api/v1/payments/**  → order-service    (:8084) → VNPay integration
  ├── /api/v1/promotions/**→ promotion-service(:8085) → MySQL: fashion_promotion_db
  ├── /api/v1/reviews/**   → review-service   (:8086) → MongoDB: fashion_review_db
  └── /api/v1/chatbot/**   → chatbot-service  (:8087) → OpenAI API

Infrastructure: MySQL 8.0 (:3307) | Redis 7.0 (:6379) | MongoDB 7.0 (:27017)
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
| `JWT_SECRET` | ✅ | Generate: `openssl rand -base64 32` (shared with Kong Gateway) |
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
- Start **MySQL**, **Redis**, and **MongoDB** containers
- Build and start all **7 Spring Boot microservices**
- Start **Kong Gateway** (replaces Spring Cloud Gateway)
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

| Service | URL | Description |
|---|---|---|
| **Frontend** | http://localhost:5173 | React + Vite |
| **Kong Gateway (Proxy)** | http://localhost:8080 | All API traffic goes through here |
| **Kong Admin API** | http://localhost:8001 | Route inspection & debug |
| **User Service** | http://localhost:8081 | Auth & user management |
| **Product Service** | http://localhost:8082 | Products, categories, wishlists |
| **Cart Service** | http://localhost:8083 | Redis-based shopping cart |
| **Order Service** | http://localhost:8084 | Orders & VNPay payments |
| **Promotion Service** | http://localhost:8085 | Promotions & coupons |
| **Review Service** | http://localhost:8086 | Product reviews (MongoDB) |
| **Chatbot Service** | http://localhost:8087 | AI chatbot (OpenAI) |

---

## 🔒 Kong Gateway

This project uses [Kong Gateway](https://konghq.com/) in **DB-less mode** as the API Gateway, replacing the previous Spring Cloud Gateway.

### Key Features
- **High Performance**: Built on Nginx/OpenResty — handles massive throughput with minimal RAM
- **CORS**: Global CORS policy for frontend origins
- **Rate Limiting**: 100 requests/minute per IP (spam protection)
- **JWT Auth**: Custom Lua plugin (`jwt-auth`) validates HS256 tokens and injects `X-User-Id`/`X-User-Email` headers

### Configuration Files
| File | Purpose |
|---|---|
| `docker/kong.yml` | Declarative routing config (services, routes, plugins) |
| `docker/kong/plugins/jwt-auth/handler.lua` | Custom JWT validation logic |
| `docker/kong/plugins/jwt-auth/schema.lua` | Plugin config schema |
| `docker/kong/docker-entrypoint-wrapper.sh` | Injects JWT_SECRET into config at startup |

### Useful Commands
```bash
# Check Kong status
curl http://localhost:8001/status

# List all registered routes
curl http://localhost:8001/routes

# List all registered services
curl http://localhost:8001/services

# Check Kong logs
docker compose -f docker/docker-compose.yml logs -f kong
```

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

# View Kong Gateway logs
docker compose -f docker/docker-compose.yml logs -f kong

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
│   ├── api-gateway/          # [DEPRECATED] Spring Cloud Gateway — replaced by Kong
│   └── services/
│       ├── user-service/
│       ├── product-service/
│       ├── cart-service/     # Redis-based cart
│       ├── order-service/    # Orders + VNPay integration
│       ├── promotion-service/
│       ├── review-service/   # MongoDB
│       └── chatbot-service/  # OpenAI
├── docker/
│   ├── docker-compose.yml
│   ├── kong.yml              # Kong Gateway declarative config
│   ├── kong/
│   │   ├── docker-entrypoint-wrapper.sh  # JWT secret injection
│   │   └── plugins/jwt-auth/            # Custom JWT Lua plugin
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

**Kong Gateway not starting?**
> Check Kong logs for config errors:
```bash
docker compose -f docker/docker-compose.yml logs kong
```
> Common issue: invalid YAML in `kong.yml`. Validate with: `python -c "import yaml; yaml.safe_load(open('docker/kong.yml'))"`

**Port already in use?**
> Stop any local MySQL/Redis running on the same ports, or edit `docker-compose.yml` to change the port mappings.

**Docker build failed?**
> Make sure Docker Desktop is running and has at least 4GB of memory allocated.