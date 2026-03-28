# ── Build stage ──────────────────────────────────────────────────────────────
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

COPY pom.xml .
COPY common/pom.xml common/pom.xml
COPY common/src common/src

COPY services/order-service/pom.xml services/order-service/pom.xml
RUN mvn -pl common -pl services/order-service -am dependency:resolve -q 2>/dev/null || true

COPY services/order-service/src services/order-service/src
RUN mvn -pl common -pl services/order-service -am clean package -DskipTests -q

# ── Run stage ────────────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/services/order-service/target/*.jar app.jar
EXPOSE 8084
ENTRYPOINT ["java", "-jar", "app.jar"]
