# ── Build stage ──────────────────────────────────────────────────────────────
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

COPY pom.xml .
COPY common/pom.xml common/pom.xml
COPY common/src common/src

COPY services/chatbot-service/pom.xml services/chatbot-service/pom.xml
RUN mvn -pl common -pl services/chatbot-service -am dependency:resolve -q 2>/dev/null || true

COPY services/chatbot-service/src services/chatbot-service/src
RUN mvn -pl common -pl services/chatbot-service -am clean package -DskipTests -q

# ── Run stage ────────────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/services/chatbot-service/target/*.jar app.jar
EXPOSE 8087
ENTRYPOINT ["java", "-jar", "app.jar"]
