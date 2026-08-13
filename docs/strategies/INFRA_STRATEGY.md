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

### Optimizaciones de eficiencia

- **`concurrency: cancel-in-progress`** activo en CI, CD staging y CD production — pushes rápidos a una misma rama cancelan la ejecución anterior.
- **`paths-ignore: ['**/*.md']`** en push a `feature/**` — cambios de solo documentación no ejecutan CI.
- **Cache de `node_modules`** añadido a CD staging y production — `npm ci` usa caché entre deploys.

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

Limite de logs (recomendado — pendiente de agregar a `docker-compose.yml`):
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

> **Estado actual:** `JAVA_OPTS` configurado en `docker-compose.yml` para gateway, auth-service y core-service. Límite heap 384m, metaspace 128m, G1GC.

```yaml
# docker-compose.yml — environment de auth-service, core-service y gateway
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

Valores actuales en `docker-compose.yml`:

| Servicio | Interval | Start period | Retries |
|----------|----------|--------------|---------|
| Gateway  | 10s      | —            | 3       |
| Auth     | 15s      | 45s          | 8       |
| Core     | 15s      | 45s          | 8       |

```yaml
# Recomendado para OCI (menos frecuente)
healthcheck:
  interval: 30s
  start_period: 45s
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

Secrets requeridos: `DOCKER_USERNAME`, `DOCKER_PASSWORD`, `STAGING_HOST`, `STAGING_USER`, `STAGING_SSH_KEY`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, `CORS_ALLOWED_ORIGINS_STAGING`, `CORS_ALLOWED_ORIGINS_PROD`, `SPRING_PROFILES_ACTIVE_STAGING`, `SPRING_PROFILES_ACTIVE_PROD`, `APP_FRONTEND_URL`, `SPRING_MAIL_*`, `GOOGLE_*`, `FACEBOOK_*`, `OAUTH2_REDIRECT_URI`. Ver `.github/SECRETS.md` para la lista completa.

---

## Monitoreo Ligero

No instalar Prometheus/Grafana (consumen mucha RAM).

- `docker stats --no-stream` en el script de deploy para reporte post-actualizacion.
- `GlobalExceptionHandler` puede enviar errores criticos a un canal Slack/Telegram via webhook.

---

## Redes Docker

| Red | Proposito | Tipo |
|-----|-----------|------|
| `proxy-caddy-network` | Caddy ↔ frontend + gateway (externa) | Bridge (external: true) |
| `pymes-internal-network` | DB, Redis, comunicacion interna backend | Bridge (external: true) |

> Ambas redes se crean una sola vez en el setup inicial (`docker network create`, ver `QUICK_START.md`). Estan marcadas como `external: true` en `docker-compose.yml` — sin esto: `network exists but was not created by compose`.

---

## Produccion: Caddy (HTTPS) + OCI

```
Browser → Caddy :80/:443 → Gateway :8080 / Frontend :9200
```

- **Caddy** (`proxy-caddy-network`, contenedor `caddy`): reverse proxy con HTTPS automático (Let's Encrypt). Bloque `https://pymeq.dioquincar.dev` con handle `/api/*`, `/oauth2/*`, `/login/*` → `pymes-gateway:8080`; resto → `pymes-frontend:9200`.
- **HTTPS**: Let's Encrypt automático de Caddy — requerido por Google OAuth (no acepta redirect `http://` en dominios públicos).
- **Frontend**: Quasar SPA servida por Caddy en puerto 9200 (nginx internamente). `VITE_API_URL=/api/v1` (URL relativa, same-origin).
- **Nginx (frontend)**: bundles JS/CSS `immutable` (cache 1y); `sw.js` y `/` con `no-cache` para que el service worker y el HTML siempre se actualicen.

### Puertos expuestos (OCI Security List)

| Puerto | Protocolo | Descripcion |
|--------|-----------|-------------|
| 22 | TCP | SSH |
| 80 | TCP | HTTP (Caddy) |
| 443 | TCP | HTTPS (Caddy — Let's Encrypt) |

> Los puertos 8080, 8081, 8082, 9200 **no** se exponen públicamente — Caddy enruta a los servicios por red interna.

### Cloudflare: Gotchas (si se usa)

| Setting | Valor correcto | Por que |
|---------|---------------|---------|
| SSL/TLS | **Full** | Flexible intenta HTTP:80 al origin y rompe el handshake TLS con el origin. |
| Bot Fight Mode | **Off** | Bloquea XHR POST desde browsers nuevos (retorna 403 + managed challenge). |
| WAF Custom Rules | No disponibles en plan Free. | — |

### PWA Cache (Firefox)

El service worker de Quasar PWA retiene bundles viejos. Si el browser sigue llamando a la URL antigua despues de un deploy:
1. F12 → Storage → Clear All
2. Cerrar y reabrir browser
3. Recargar la pagina

---

## Security List Oracle Cloud (Puertos minimos)

| Puerto | Protocolo | Descripcion |
|--------|-----------|-------------|
| 22 | TCP | SSH |
| 80 | TCP | HTTP (Caddy) |
| 443 | TCP | HTTPS (Caddy — Let's Encrypt) |

> Los puertos 8080, 8081, 8082, 9200 **no** se exponen públicamente.
