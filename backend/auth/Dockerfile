FROM maven:3.9-eclipse-temurin-21-alpine AS builder
WORKDIR /app

# Runtime recortado: ~70 MB en vez de los 158 MB del JRE completo.
RUN jlink \
    --add-modules java.base,java.desktop,java.instrument,java.management,java.naming,java.net.http,java.prefs,java.rmi,java.scripting,java.security.jgss,java.security.sasl,java.sql,java.sql.rowset,java.transaction.xa,java.xml,java.xml.crypto,jdk.crypto.cryptoki,jdk.crypto.ec,jdk.jfr,jdk.localedata,jdk.unsupported,jdk.unsupported.desktop \
    --strip-debug --no-man-pages --no-header-files --compress=zip-6 \
    --output /opt/jre

# Cache dependencies: copy pom.xml first
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Now copy source and build
COPY src/ src/
RUN mvn clean package -DskipTests -B

FROM alpine:3.24
WORKDIR /app
RUN addgroup -S spring && adduser -S spring -G spring
COPY --from=builder /opt/jre /opt/jre
ENV PATH="/opt/jre/bin:$PATH"
COPY --chown=spring:spring --from=builder /app/target/*.jar app.jar
USER spring:spring
EXPOSE 8081
ENV JAVA_OPTS="-Xmx384m -Xms256m -XX:MaxMetaspaceSize=128m -XX:MaxDirectMemorySize=128m -XX:+ExitOnOutOfMemoryError -XX:+UseG1GC"
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
