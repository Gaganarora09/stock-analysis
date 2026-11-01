# Multi-stage Dockerfile for Stock Analysis Application

# Stage 1: Build the application with Maven & Eclipse Temurin 17
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /app

# Copy project files
COPY pom.xml .
COPY src ./src

# Build production JAR (skipping test suite for rapid containerization)
RUN mvn clean package -DskipTests

# Stage 2: Minimal JRE runtime image
FROM eclipse-temurin:17-jre
WORKDIR /app

# Copy compiled JAR artifact
COPY --from=builder /app/target/*.jar app.jar

# Expose default HTTP port
EXPOSE 8080

# Environment variables with sensible defaults
ENV PORT=8080

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
