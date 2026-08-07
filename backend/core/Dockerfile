FROM maven:3.9-eclipse-temurin-21-alpine AS builder
WORKDIR /app

# Cache dependencies: copy pom.xml first
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Now copy source and build
COPY src/ src/
RUN mvn clean package -DskipTests -B

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S spring && adduser -S spring -G spring
COPY --from=builder /app/target/*.jar app.jar
RUN chown spring:spring app.jar
USER spring:spring
EXPOSE 8082
ENV JAVA_TOOL_OPTIONS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"
ENTRYPOINT ["java", "-jar", "app.jar"]
