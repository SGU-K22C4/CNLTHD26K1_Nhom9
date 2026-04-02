# ── Build stage ──────────────────────────────────────────────────────────────
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

COPY pom.xml .
COPY common/pom.xml common/pom.xml
COPY common/src common/src

COPY services/promotion-service/pom.xml services/promotion-service/pom.xml
RUN mvn -pl common -pl services/promotion-service -am dependency:resolve -q 2>/dev/null || true

COPY services/promotion-service/src services/promotion-service/src
RUN mvn -pl common -pl services/promotion-service -am clean package -DskipTests -q

# ── Run stage ────────────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/services/promotion-service/target/*.jar app.jar
EXPOSE 8085
ENTRYPOINT ["java", "-jar", "app.jar"]
