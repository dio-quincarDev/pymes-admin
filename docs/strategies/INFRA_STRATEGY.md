# Estrategia de Infraestructura — pymes-admin

Decisiones y tacticas para operar en Oracle Cloud Free Tier (ARM64, recursos limitados).

---

## CI/CD: Multi-arquitectura

GitHub Actions corre en AMD64. Oracle Cloud Free Tier usa ARM64. Los builds deben ser multi-arquitectura.

```yaml
# En todos los steps de docker/build-push-action
platforms: linux/amd64,linux/arm64
```

Si los tiempos superan 10 min, usar jobs paralelos por arquitectura.

---

## Deploy RAM-Safe (Anti-OOM)

Docker Compose recrea servicios con solapamiento. En servidores con ≤2GB RAM causa picos OOM.

**Fix:** Detener antes de arrancar en servicios criticos:
```bash
docker compose stop pymes-auth-service
docker compose up -d pymes-auth-service
```
Costo: 10-15 s de downtime. Beneficio: no se bloquea el servidor.

---

## Gestion de Espacio en Disco

Las imagenes multi-arquitectura y builds de Quasar consumen mucho espacio.

```bash
# Despues de cada deploy — borra todo lo que no este en uso
docker image prune -af
```

Limite de logs en `docker-compose.yml`:
```yaml
logging:
  driver: "json-file"
  options:
    max-size: "10m"
    max-file: "3"
```

---

## JVM Memory Control (Critico)

Spring Boot sin limites explicitos consume RAM agresivamente.

```yaml
# docker-compose.yml — environment de auth-service y gateway
JAVA_OPTS: -Xmx384m -Xms256m -XX:MaxMetaspaceSize=128m -XX:+UseG1GC
```

- `-Xmx384m` — maximo heap
- `-Xms256m` — heap inicial
- `-XX:MaxMetaspaceSize=128m` — limite de metadatos
- `-XX:+UseG1GC` — GC eficiente en memoria

---

## Service Discovery: Docker DNS (No Eureka)

No usar Netflix Eureka ni Consul — consumen 512MB-1GB de RAM.

**Tactica:** DNS interno de Docker. Los servicios se referencian por service name en `docker-compose.yml`:
```yaml
# Los servicios se referencian por service name (DNS de Docker Compose)
AUTH_SERVICE_HOST: pymes-auth-service
```

---

## Mensajeria Asincrona: Redis Pub/Sub (No Kafka/RabbitMQ)

Reutilizar la instancia Redis ya usada para blacklist de tokens.

- **Redis Pub/Sub** para eventos ligeros (ej. "Usuario Creado").
- **Redis Streams** si se necesita persistencia de mensajes.
- Ahorro: ~500MB+ de RAM al evitar un broker separado.

---

## Comunicacion Inter-Service Sincrona

Para llamadas sincronas entre microservicios (Core → Auth):
- **Cliente:** Spring Cloud OpenFeign.
- **HTTP client:** OkHttp o Apache HC5 en vez del default (menos overhead de hilos).
- **Cache:** Redis para respuestas de solo lectura frecuentes.

---

## Healthchecks

En CPUs compartidas de OCI, healthchecks muy frecuentes causan picos de CPU.

```yaml
healthcheck:
  interval: 30s      # cada 30 s (no cada 10 s)
  start_period: 45s  # da tiempo a la JVM para calentar sin matar el contenedor
```

---

## Imagenes Docker

- **Base:** `eclipse-temurin:21-jre-alpine` (no JDK completo).
- **Multi-stage:** compilar con imagen pesada, copiar solo el JAR a Alpine.
- **Resultado:** ~150MB vs ~400MB. Deploy mas rapido.

---

## Gestion de Secretos

No depender de `.env` editados manualmente en el servidor.

**Flujo:**
1. GitHub Actions genera `.env` dinamico en el servidor via SSH heredoc
2. Variables vienen de GitHub Secrets (nunca del repositorio)
3. El servidor nunca tiene credenciales hardcodeadas

Secrets requeridos: `DOCKER_USERNAME`, `DOCKER_PASSWORD`, `STAGING_HOST`, `STAGING_USER`, `STAGING_SSH_KEY`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, `CORS_ALLOWED_ORIGINS`. Ver `README.md` para la lista completa.

---

## Monitoreo Ligero

No instalar Prometheus/Grafana (consumen mucha RAM).

- `docker stats --no-stream` en el script de deploy para reporte post-actualizacion.
- `GlobalExceptionHandler` puede enviar errores criticos a un canal Slack/Telegram via webhook.

---

## Redes Docker

| Red | Proposito | Tipo |
|-----|-----------|------|
| `pymes-global-network` | Nginx Proxy Manager ↔ servicios frontend/backend | Externa |
| `pymes-internal-network` | DB, Redis, comunicacion interna backend | Bridge (external: true) |

> `pymes-internal-network` marcada como `external: true` en `docker-compose.yml` porque se crea con `setup-server.sh`. Sin esto: `network exists but was not created by compose`.

---

## Security List Oracle Cloud (Puertos minimos)

| Puerto | Protocolo | Descripcion |
|--------|-----------|-------------|
| 22 | TCP | SSH |
| 80 | TCP | HTTP (Nginx Proxy Manager) |
| 443 | TCP | HTTPS (Nginx Proxy Manager) |

> Los puertos 8080, 8081, 9200 **no** deben estar abiertos. Todo el trafico externo pasa por Nginx en 80/443.
