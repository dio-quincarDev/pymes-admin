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

## Monitoreo

VictoriaMetrics + Grafana para observabilidad de los 3 backends JVM.

### Stack

| Servicio | Imagen | RAM | Puerto |
|----------|--------|-----|--------|
| VictoriaMetrics | `victoriametrics/victoria-metrics:v1.102.0` | ~30MB | 8428 |
| Grafana | `grafana/grafana:11.2.0` | ~250MB | 3001 (host) → 3000 (container) |

Total: ~300MB. Logs y tracing diferidos hasta que duela.

### Scraping

`infra/monitoring/scrape.yml` — VictoriaMetrics scrape config:

```yaml
global:
  scrape_interval: 15s

scrape_configs:
  - job_name: pymes
    static_configs:
      - targets:
          - pymes-gateway:8080
          - pymes-auth-service:8081
          - pymes-core-service:8082
        labels:
          env: stg
    metrics_path: /actuator/prometheus
    scrape_timeout: 5s
```

- Solo backends JVM con Micrometer (gateway, auth, core).
- El frontend (Quasar PWA) no tiene `/actuator/prometheus` — no se scraea.
- `env: stg` hardcodeado. VictoriaMetrics usa `%{VAR}` para sustitución de variables de entorno, pero requiere `environment:` en `docker-compose.yml` para pasarlas al contenedor.

### Dashboard

`infra/monitoring/grafana/dashboards/pymes.json` — 6 paneles:

1. Request rate (req/s)
2. Response time p50/p95/p99
3. Error rate (5xx)
4. JVM memory (heap/non-heap)
5. CPU usage
6. Active threads

Datasource: VictoriaMetrics (`/api/v1/query`).

### Acceso

- **URL:** `https://monitor-pymeq.dioquincar.dev`
- **Creds:** `admin` / valor de `GRAFANA_PASSWORD` (GitHub Secret)
- **DNS:** CNAME `monitor-pymeq` → `149.130.165.200` (Cloudflare proxy naranja)
- **Caddy:** `http://monitor-pymeq.dioquincar.dev { reverse_proxy pymes-grafana:3000 }`
- **Nota:** `*.dioquincar.dev` solo cubre un nivel de subdominio. Usar guion (`monitor-pymeq`), no punto (`monitor.pymeq`).

### CI/CD: Cleanup de directorios root-owned

Docker bind mount puede crear archivos/directorios como `root` en el host. El deploy user (`ubuntu`) no puede sobrescribirlos.

**Cleanup step** (antes de `Copy monitoring configs`):
```bash
sudo rm -rf ~/pymes-admin/infra          # borra TODO, incluyendo monitoring/ root-owned
mkdir -p ~/pymes-admin/infra/monitoring   # recrea como ubuntu
```

**Deploy step:**
```bash
docker compose pull
docker compose up -d --force-recreate --remove-orphans
docker image prune -af
```

`--force-recreate` es necesario para evitar conflictos de bind mount cuando containers previos quedaron en estado `Created`/`Exited(127)` por deploys fallidos anteriores.

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

- **Caddy** (`~/caddy-proxy/Caddyfile`, contenedor `caddy-proxy`): reverse proxy HTTP puro en puerto 80. TLS lo maneja Cloudflare. Bloque `http://pymeq.dioquincar.dev` con matchers `@sw` (`/sw.js`), `@svg` (`path_regexp \.svg$`), `@root` (`/`) con `Cache-Control: no-cache`; handles `/api/*`, `/oauth2/*`, `/login/*` → `pymes-gateway:8080`; fallback → `pymes-frontend:9200`. Bloque `http://dioquincar.dev` → `portfolio-frontend:80`.
- **HTTPS**: Let's Encrypt automático de Caddy — requerido por Google OAuth (no acepta redirect `http://` en dominios públicos).
- **Frontend**: Quasar SPA servida por Caddy en puerto 9200 (nginx internamente). `VITE_API_URL=/api/v1` (URL relativa, same-origin).
- **Nginx (frontend)**: bundles JS/CSS `immutable` (cache 1y); SVGs, `sw.js` y `/` con `no-cache` para que los logos, el service worker y el HTML siempre se actualicen.

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

## Produccion: Cloudflare + OCI Load Balancer + Caddy

```
Browser → Cloudflare (CDN, DNS, WAF) → OCI LB :80 HTTP → Caddy :80 → Gateway :8080 / Frontend :9200
```

- **Cloudflare**: DNS + CDN + SSL terminacion. SSL/TLS mode = **Full** (NO Flexible — OCI LB solo escucha HTTP:80, pero Cloudflare con Full conecta al origin en HTTPS:443 via red interna de Cloudflare).
- **OCI Load Balancer**: HTTP:80 listener → forwards a vm2-test2.
- **Caddy** (`~/caddy-proxy/Caddyfile`): reverse proxy HTTP puro en puerto 80 (TLS lo maneja Cloudflare). Matchers `@sw`, `@svg` (`path_regexp \.svg$`), `@root` con `Cache-Control: no-cache`. Handles `/api/*`, `/oauth2/*`, `/login/*` → gateway:8080; fallback → frontend:9200. Portfolio en `dioquincar.dev` → portfolio-frontend:80.
- **Frontend**: Quasar SPA servida por Caddy en puerto 9200 (nginx internamente). `VITE_API_URL=/api/v1` (URL relativa, same-origin).

### Puertos expuestos (OCI Security List)

| Puerto | Protocolo | Descripcion |
|--------|-----------|-------------|
| 22 | TCP | SSH |
| 80 | TCP | HTTP (OCI LB → Caddy) |

> Los puertos 443, 8080, 8081, 8082, 9200 **no** deben estar abiertos. TLS lo maneja Cloudflare.

### Cloudflare: Gotchas

| Setting | Valor correcto | Por que |
|---------|---------------|---------|
| SSL/TLS | **Full** | Flexible intenta HTTP:80 al origin, pero OCI LB + Cloudflare handshake requiere Full para que Cloudflare maneje TLS end-to-end correctamente. |
| Bot Fight Mode | **Off** | Bloquea XHR POST desde browsers nuevos (retorna 403 + managed challenge). |
| WAF Custom Rules | No disponibles en plan Free. | — |

### Cloudflare: SVG cache

Cloudflare cachea archivos estáticos (SVGs, PNGs, etc.) con un TTL largo si el origin envía `Cache-Control: public` o `immutable`. Si cambiás un SVG y redeployás, Cloudflare sigue entregando la versión vieja hasta que el TTL expire (puede ser 1 año).

**Después de cambiar logos o SVGs visibles:** purgar cache desde Cloudflare Dashboard → Caching → Configuration → "Purge Everything". Sin este paso, el browser nunca ve la versión nueva aunque el origin ya la sirva correctamente.

**Caddy `path_regexp`:** El matcher `path *.svg` de Caddy NO matchea `/icons/logo.svg` porque `*` no cruza `/`. Usar `path_regexp \.svg$` que usa regex y sí funciona para cualquier path terminando en `.svg`.

### PWA Cache (Firefox)

El service worker de Quasar PWA retiene bundles viejos (`localhost:8080`). Si el browser sigue llamando a `localhost:8080` despues de un deploy:
1. F12 → Storage → Clear All
2. Cerrar y reabrir browser
3. Recargar la pagina

### Caddy (sin TLS)

Caddy corre HTTP puro en :80 (sin TLS). TLS termina en Cloudflare. No usar `https://` en el Caddyfile.

---

## Security List Oracle Cloud (Puertos minimos)

| Puerto | Protocolo | Descripcion |
|--------|-----------|-------------|
| 22 | TCP | SSH |
| 80 | TCP | HTTP (OCI LB → Caddy) |

> Los puertos 443, 8080, 8081, 8082, 9200 **no** deben estar abiertos.
