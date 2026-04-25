# ── Build stage ──────────────────────────────────────────────────────────────
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

# 1) Copy parent POM (multi-module root)
COPY pom.xml .

# 2) Copy common module (shared dependency)
COPY common/pom.xml common/pom.xml
COPY common/src common/src
COPY services/config-service/pom.xml services/config-service/pom.xml
COPY services/product-service/pom.xml services/product-service/pom.xml
COPY services/cart-service/pom.xml services/cart-service/pom.xml
COPY services/order-service/pom.xml services/order-service/pom.xml
COPY services/promotion-service/pom.xml services/promotion-service/pom.xml
COPY services/review-service/pom.xml services/review-service/pom.xml
COPY services/chatbot-service/pom.xml services/chatbot-service/pom.xml

# 3) Copy this service's POM and resolve dependencies
COPY services/user-service/pom.xml services/user-service/pom.xml
RUN mvn -pl common -pl services/user-service -am dependency:resolve -q 2>/dev/null || true

# 4) Copy source and build
COPY services/user-service/src services/user-service/src
RUN mvn -pl common -pl services/user-service -am clean package -DskipTests -q

# ── Run stage ────────────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/services/user-service/target/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
