# ── Build stage ──────────────────────────────────────────────────────────────
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

COPY pom.xml .
COPY api-gateway/pom.xml api-gateway/pom.xml
COPY common/pom.xml common/pom.xml
COPY services/config-service/pom.xml services/config-service/pom.xml
COPY services/user-service/pom.xml services/user-service/pom.xml
COPY services/product-service/pom.xml services/product-service/pom.xml
COPY common/src common/src

COPY services/cart-service/pom.xml services/cart-service/pom.xml
COPY services/order-service/pom.xml services/order-service/pom.xml
COPY services/promotion-service/pom.xml services/promotion-service/pom.xml
COPY services/review-service/pom.xml services/review-service/pom.xml
COPY services/chatbot-service/pom.xml services/chatbot-service/pom.xml
RUN mvn -pl common -pl services/cart-service -am dependency:resolve -q 2>/dev/null || true

COPY services/cart-service/src services/cart-service/src
RUN mvn -pl common -pl services/cart-service -am clean package -DskipTests -q

# ── Run stage ────────────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/services/cart-service/target/*.jar app.jar
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "app.jar"]
