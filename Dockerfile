# ── Stage 1: Build ──────────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /app

# Copy pom first (Docker caches this layer — speeds up re-builds)
COPY pom.xml .
RUN mvn dependency:go-offline -q

# Copy source and build, skipping tests (tests run in CI separately)
COPY src ./src
RUN mvn package -DskipTests -q

# ── Stage 2: Run ────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copy only the built JAR from the builder stage
COPY --from=builder /app/target/*.jar app.jar

# Port your Spring Boot app listens on
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]