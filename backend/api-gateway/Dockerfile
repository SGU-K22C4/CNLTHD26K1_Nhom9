# ── Build stage ──────────────────────────────────────────────────────────────
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

COPY pom.xml .
COPY common/pom.xml common/pom.xml
COPY common/src common/src

COPY api-gateway/pom.xml api-gateway/pom.xml
RUN mvn -pl common -pl api-gateway -am dependency:resolve -q 2>/dev/null || true

COPY api-gateway/src api-gateway/src
RUN mvn -pl common -pl api-gateway -am clean package -DskipTests -q

# ── Run stage ────────────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/api-gateway/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
