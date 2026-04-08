# ── Build stage ──────────────────────────────────────────────────────────────
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

COPY pom.xml .
COPY common/pom.xml common/pom.xml
COPY common/src common/src

COPY services/product-service/pom.xml services/product-service/pom.xml
RUN mvn -pl common -pl services/product-service -am dependency:resolve -q 2>/dev/null || true

COPY services/product-service/src services/product-service/src
RUN mvn -pl common -pl services/product-service -am clean package -DskipTests -q

# ── Run stage ────────────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/services/product-service/target/*.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
