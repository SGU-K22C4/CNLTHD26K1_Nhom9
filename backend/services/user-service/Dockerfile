# ── Build stage ──────────────────────────────────────────────────────────────
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

# 1) Copy parent POM (multi-module root)
COPY pom.xml .

# 2) Copy common module (shared dependency)
COPY common/pom.xml common/pom.xml
COPY common/src common/src

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
ENTRYPOINT ["java", "-jar", "app.jar"]
