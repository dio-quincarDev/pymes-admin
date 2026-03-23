# ==================== BUILD STAGE ====================
FROM maven:3.9-eclipse-temurin-21-alpine AS builder

WORKDIR /app

# Copy pom.xml first for dependency caching
COPY pom.xml .

# Download dependencies (cached if pom.xml unchanged)
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build the application (skip tests for faster build)
RUN mvn clean package -DskipTests -B

# ==================== RUNTIME STAGE ====================
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Create non-root user for security
RUN addgroup -S spring && adduser -S spring -G spring

# Copy the built jar from builder stage
COPY --from=builder /app/target/*.jar app.jar

# Give ownership to non-root user
RUN chown spring:spring app.jar

# Switch to non-root user
USER spring:spring

# Expose port 8081 (AUTH microservice)
EXPOSE 8081

# JVM options for container optimization
ENV JAVA_TOOL_OPTIONS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
