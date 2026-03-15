# ---------- Build stage ----------
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# Copy Maven wrapper and pom first for better layer caching
COPY healthapp/.mvn/ .mvn/
COPY healthapp/mvnw ./
COPY healthapp/pom.xml ./

RUN chmod +x mvnw
RUN ./mvnw dependency:go-offline -B

# Copy source and build
COPY healthapp/src ./src
RUN ./mvnw clean package -DskipTests -B

# ---------- Runtime stage ----------
FROM eclipse-temurin:21-jre-alpine AS runtime

WORKDIR /app

# Optional: create non-root user
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Copy built jar from builder
COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-Duser.timezone=America/Jamaica", "-jar", "app.jar"]
