# Etapa 1: Construcción (Builder)
FROM maven:3.9.9-eclipse-temurin-21-alpine AS builder
WORKDIR /app

# Copiamos el pom y bajamos dependencias (aprovechando el caché de capas de Docker)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copiamos el código fuente y compilamos
COPY src ./src
RUN mvn clean package -DskipTests -B

# Etapa 2: Imagen de ejecución (Runtime)
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Seguridad: Crear un usuario no root para ejecutar la app
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Copiamos el jar generado desde la etapa anterior
# Nota: Asegúrate de que el nombre del jar coincida con tu artifactId en el pom.xml
COPY --from=builder /app/target/gateway-pymes-*.jar app.jar

# El Gateway por defecto usa el 8080
EXPOSE 8080

# Parámetros optimizados para Java 21 en contenedores
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]