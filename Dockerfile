# ── Stage 1: Build ────────────────────────────────────────────────────────────
FROM gradle:8.8-jdk17 AS builder

WORKDIR /app
COPY build.gradle.kts settings.gradle.kts gradle.properties ./
COPY src ./src

RUN gradle dependencies --no-daemon || true
RUN gradle buildFatJar --no-daemon

# ── Stage 2: Runtime ──────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

COPY --from=builder /app/build/libs/*-all.jar app.jar

RUN mkdir -p uploads/todos uploads/users

EXPOSE 8080

ENV APP_HOST=0.0.0.0
ENV APP_PORT=8080

ENTRYPOINT ["java", "-jar", "app.jar"]
