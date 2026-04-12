# 🚀 Estrategia de Optimización de Recursos y Conectividad

Esta guía define las tácticas para operar la plataforma PyMes Admin en entornos de recursos limitados (ej. Oracle Cloud Free Tier con 1GB-4GB RAM).

## 1. Control de Memoria JVM (Crítico)
Los microservicios Spring Boot pueden consumir mucha RAM si no se limitan explícitamente.

### Configuración de `JAVA_OPTS`
Se deben aplicar límites de Heap y Metaspace en el `docker-compose.yml`:
```yaml
environment:
  - JAVA_OPTS=-Xmx384m -Xms256m -XX:MaxMetaspaceSize=128m -XX:+UseG1GC
```
*   **-Xmx384m**: Límite máximo de memoria para objetos (Heap).
*   **-Xms256m**: Memoria inicial reservada al arrancar.
*   **-XX:MaxMetaspaceSize=128m**: Límite para metadatos de clases.
*   **-XX:+UseG1GC**: Recolector de basura eficiente en memoria.

## 2. Service Discovery Ligero
No utilizar Netflix Eureka ni HashiCorp Consul. 
*   **Táctica**: Usar el **DNS interno de Docker**. 
*   **Implementación**: Los servicios se comunican usando el `container_name` definido en el `docker-compose.yml` (ej. `http://pymes-auth-service:8081`).
*   **Ahorro**: ~512MB - 1GB de RAM al evitar un servicio de discovery dedicado.

## 3. Comunicación Inter-Service
Para peticiones síncronas entre microservicios (ej. Core -> Auth):
*   **Cliente**: Usar **Spring Cloud OpenFeign**.
*   **Optimización**: Configurar un cliente HTTP ligero (OkHttp o Apache HC5) en lugar del default para reducir el overhead de hilos.
*   **Cache**: Usar Redis para cachear respuestas de "solo lectura" entre servicios y evitar llamadas repetitivas.

## 4. Mensajería Asíncrona con Redis
Evitar la instalación de RabbitMQ o Apache Kafka.
*   **Táctica**: Aprovechar la instancia de **Redis** que ya se usa para la blacklist de tokens.
*   **Uso**: Implementar **Redis Pub/Sub** o **Redis Streams** para eventos ligeros (ej. "Usuario Creado", "Plan Actualizado").
*   **Ahorro**: ~500MB+ de RAM al reutilizar Redis.

## 5. Healthchecks Eficientes
Los healthchecks frecuentes pueden causar picos de CPU en CPUs compartidas.
*   **Ajuste**: 
    *   `interval`: 30s - 60s (una vez que el servicio esté arriba).
    *   `start_period`: 45s (dar tiempo a la JVM a calentar sin matarla).

## 6. Imágenes Docker Ultra-Ligeras
*   **Base**: Usar siempre `eclipse-temurin:21-jre-alpine`.
*   **Multi-stage**: Compilar en una imagen pesada y copiar solo el JAR a la imagen Alpine final.
*   **Ahorro**: Reducción del tamaño de imagen de ~400MB a ~150MB, acelerando el despliegue.
