# ─── Stage 1: Build ───────────────────────────────────────────────
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# Copy maven wrapper and pom first (layer caching — only re-downloads deps if pom changes)
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

RUN ./mvnw dependency:go-offline -B

# Copy source and build
COPY src ./src
RUN ./mvnw clean package -DskipTests -B

# ─── Stage 2: Run ─────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine AS runtime

WORKDIR /app

# Non-root user for security
RUN addgroup -S healthapp && adduser -S healthapp -G healthapp

COPY --from=builder /app/target/*.jar app.jar

RUN chown healthapp:healthapp app.jar

USER healthapp

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]