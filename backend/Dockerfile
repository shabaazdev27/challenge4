# ─── Stage 1: Build ──────────────────────────────────────────────────────────
FROM maven:3.9.8-eclipse-temurin-21-alpine AS builder

WORKDIR /app

# Cache dependencies first (only re-downloads when pom.xml changes)
COPY pom.xml .
RUN mvn dependency:go-offline -q

# Copy source and build
COPY src ./src
RUN mvn package -DskipTests -q

# ─── Stage 2: Runtime ────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

# Security: run as a non-root user
RUN groupadd -r stadiummate && useradd -r -g stadiummate stadiummate
USER stadiummate

# Copy only the fat JAR from the build stage
COPY --from=builder /app/target/stadium-mate-backend-*.jar app.jar

# Cloud Run injects PORT; Spring Boot reads it via ${PORT:8080}
EXPOSE 8080

# JVM flags for Cloud Run container environment:
#   -Xms128m   initial heap (keeps start-up fast)
#   -Xmx512m   max heap (sufficient for ~30-node graph + concurrent requests)
#   -XX:+UseContainerSupport  respects container memory limits
ENTRYPOINT ["java", \
            "-Xms128m", "-Xmx512m", \
            "-XX:+UseContainerSupport", \
            "-Djava.security.egd=file:/dev/./urandom", \
            "-jar", "app.jar"]
