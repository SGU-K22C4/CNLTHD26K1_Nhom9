# Migration Plan: Spring Cloud Gateway to Kong Gateway

Yes, your project is currently using **Spring Cloud Gateway** (as seen in the configuration at `backend/api-gateway/src/main/resources/application.yml`). Spring Gateway runs on Java/Spring Boot, integrates well with the Spring ecosystem, but can be memory-intensive.

**Kong Gateway** is a high-performance API Gateway built on top of Nginx/OpenResty. It is extremely popular due to its blazing-fast throughput and high scalability via Plugins (like JWT, Rate Limiting, CORS, Prometheus).

Below is the detailed plan and guide to transition your system from Spring Gateway to Kong Gateway.

---

## 1. Core Differences

| Feature | Current Spring Cloud Gateway | Kong Gateway (DB-less mode) |
| :--- | :--- | :--- |
| **Routing** | Configured via `spring.cloud.gateway.routes` in `application.yml` | Configured via a single YAML file (e.g., `kong.yml`) loaded into memory on startup. |
| **Security (JWT)** | Custom Java filter `JwtAuthFilter`. | Built-in `kong-plugin-jwt`. |
| **CORS** | Configured globally via `globalcors`. | Built-in `kong-plugin-cors`. |
| **Performance** | High memory footprint (JVM). Good speed. | Minimal RAM usage, handles massive throughput (Nginx core). |

---

## 2. Migration Plan

### Step 1: Remove Spring API Gateway
1. Remove (or comment out) the `api-gateway` service in your `docker-compose.yml` file.
2. Stop building and running the `backend/api-gateway` module completely.

### Step 2: Deploy Kong via Docker Compose
We will deploy Kong in **DB-less mode** (without an accompanying PostgreSQL database) to keep the system lightweight and manageable via a single declarative config file.
Add the following block to your `docker-compose.yml`:

```yaml
  kong:
    image: kong:3.4-ubuntu
    container_name: kong-gateway
    environment:
      KONG_DATABASE: "off"
      KONG_DECLARATIVE_CONFIG: /kong/declarative/kong.yml
      KONG_PROXY_ACCESS_LOG: /dev/stdout
      KONG_PROXY_ERROR_LOG: /dev/stderr
      KONG_PROXY_LISTEN: 0.0.0.0:8080
      KONG_ADMIN_LISTEN: 0.0.0.0:8001
    ports:
      - "8080:8080"
      - "8001:8001"
    volumes:
      - ./kong.yml:/kong/declarative/kong.yml
    networks:
      - ecommerce-network
    depends_on:
      - user-service
      - product-service
      - order-service
```

### Step 3: Create Kong Route Configuration (`kong.yml`)
In Spring Gateway, you routed paths like `/api/v1/auth/**` to their respective target Service URLs. Similarly, we define this in a `kong.yml` file placed at the root of your project:

```yaml
_format_version: "3.0"

# --- GLOBAL PLUGINS ---
plugins:
  - name: cors
    config:
      origins: 
        - "http://localhost:5173"
        - "http://localhost:3000"
      methods: ["GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"]
      headers: ["*"]
      credentials: true

# --- SERVICES & ROUTES DECLARATION ---
services:
  # 1. User & Auth Service
  - name: user-service
    url: http://user-service:8081
    routes:
      - name: auth-route
        paths: ["/api/v1/auth"]
        strip_path: false
      - name: user-secured-route
        paths: ["/api/v1/users"]
        strip_path: false
        # In Spring Gateway you used JwtAuthFilter for /users. In Kong, we use the jwt plugin:
        plugins:
          - name: jwt
            config:
              secret_is_base64: false
              claims_to_verify: ["exp"]

  # 2. Product Service
  - name: product-service
    url: http://product-service:8082
    routes:
      - name: product-route
        paths: ["/api/v1/products", "/api/v1/categories"]
        strip_path: false

  # 3. Cart Service
  - name: cart-service
    url: http://cart-service:8083
    routes:
      - name: cart-route
        paths: ["/api/v1/cart"]
        strip_path: false

  # 4. Order Service
  - name: order-service
    url: http://order-service:8084
    routes:
      - name: order-route
        paths: ["/api/v1/orders"]
        strip_path: false

  # 5. Promotion Service
  - name: promotion-service
    url: http://promotion-service:8085
    routes:
      - name: promotion-route
        paths: ["/api/v1/promotions"]
        strip_path: false
        
  # Repeat similarly for review-service and chatbot-service...
```

### Step 4: Integrate JWT Token Verification in Kong
In Spring Gateway, you manually verified the token signature in your `JwtAuthFilter`. Kong Gateway has a built-in **JWT** plugin; however, this plugin requires clients (Consumers) and secret keys to be registered directly against the Kong Gateway. 
*Recommended Approaches:*
- **Method 1 (Fastest & Simplest):** Keep checking the JWT in the backend Spring services (`user-service`). Kong simply proxies the requests and forwards the `Authorization` header without stripping it. This means you do not add the `jwt` plugin to `kong.yml` at all; Kong acts purely as a routing reverse proxy, and the destination microservice independently verifies the JWT (pattern: Microservice-Level Security).
- **Method 2 (Advanced/Centralized):** Integrate Kong with an Identity Provider like Keycloak via OIDC plugins, or write a Custom Kong Lua Plugin to automatically decrypt and verify the token using a shared secret key.

### Summary / Next Steps

If you want to migrate to Kong Gateway right away:
1. Confirm if you'd like to delegate "JWT Token Verification" to the respective target microservices (Method 1), rather than blocking invalid tokens at the Gateway level.
2. If you agree, I will directly replace your configurations by updating `docker-compose.yml` and creating `kong.yml`, then we can deprecate the Spring API Gateway.
