# Pymeq — SaaS Financial Management Platform

> Multi-tenant SaaS platform con toolkit de contabilidad forense impulsado por IA para PYMEs LATAM.

---

## Stack

| Componente | Tecnologia |
|------------|------------|
| Frontend | Quasar 2 + Vue 3 + TypeScript (PWA, hash routing) |
| Auth Service | Spring Boot 3.4.3 — OAuth2 + JWT + RBAC + Thymeleaf email |
| Gateway | Spring Cloud Gateway (WebFlux) — JWT validation + Swagger aggregation |
| Core Service | Spring Boot 3.5.14 — 5 modulos: setup, gastos, prestamos, ventas, accounting |
| Database | PostgreSQL 15 (single instance, multi-schema: `auth`, `core`) |
| Cache | Redis 7 (blacklist, permissions, debounce, analytics cache) |
| CI/CD | GitHub Actions + Docker (multi-arch AMD64/ARM64) |
| Infra | Oracle Cloud Free Tier (ARM64) |

Ver `AGENTS.md` para arquitectura detallada y comandos de desarrollo.

---

## Quick Start

```bash
# Frontend
cd frontend/pymes && npm install && npm run dev  # port 9200

# Auth Service
cd backend/auth
cp .env.example .env  # editar con valores reales
./mvnw spring-boot:run -Pdev  # port 8081

# Gateway
cd backend/gateway-pymes
./mvnw spring-boot:run -Pdev  # port 8080

# Core Service
cd backend/core
./mvnw spring-boot:run -Pdev  # port 8082

# Docker (full stack)
docker compose up -d  # requiere .env en raiz
```

---

## Test Suite

| Servicio | Unit | Integration | Consistency | Total |
|----------|------|-------------|-------------|-------|
| Auth Service | 140 | 55 | 12 | 207 |
| Core Service | 173 | 45 | — | 218 |
| Gateway | 37 | — | — | 37 |
| **Total** | **350** | **100** | **12** | **462** |

### Ejecucion

```bash
# Auth — unitarios
cd backend/auth && ./mvnw test -B

# Auth — integracion (requiere Docker)
cd backend/auth && ./mvnw verify -B -Dspring.profiles.active=integration

# Core — unitarios
cd backend/core && ./mvnw test -B

# Core — integracion (requiere Docker)
cd backend/core && ./mvnw verify -B -Dspring.profiles.active=integration

# Gateway — unitarios (sin Docker)
cd backend/gateway-pymes && ./mvnw test -B
```

### Infraestructura de Test

- **Unitarios**: Mockito, sin Docker
- **Integracion**: Testcontainers (PostgreSQL 15-alpine + Redis 7-alpine)
- **Base class**: `AbstractIntegrationTest` — lifecycle de containers, perfiles de test
- **Convencion**: `*Test.java` (unit), `*IntegrationTest.java` (integracion, en paquete `**/integration/**`)

Ver READMEs individuales para detalle de cobertura por dominio.

---

## CI/CD

| Rama | Pipeline | Deploy |
|------|----------|--------|
| `feature/*` | CI (build + test) | Ninguno |
| `develop` | CI + CD | Staging (OCI) |
| `main` | CI + CD | Produccion |

PRs van a `develop`, nunca directamente a `main`.

**Optimizaciones activas:** cancelación automática de runs stale (`concurrency`), CI salta en pushes de solo docs (`**/*.md`) a `feature/**`, CD cachea `node_modules` entre deploys.

### Secrets (GitHub Settings)

| Secret | Descripcion |
|--------|-------------|
| `DOCKER_USERNAME` | Usuario Docker Hub |
| `DOCKER_PASSWORD` | Access Token Docker Hub |
| `STAGING_HOST` | IP instancia OCI staging |
| `STAGING_USER` | `ubuntu` |
| `STAGING_SSH_KEY` | Llave privada SSH |
| `CORS_ALLOWED_ORIGINS_STAGING` | Origenes CORS permitidos (staging) |
| `GOOGLE_CLIENT_ID` | OAuth2 Google client ID |
| `GOOGLE_CLIENT_SECRET` | OAuth2 Google client secret |
| `FACEBOOK_CLIENT_ID` | OAuth2 Facebook client ID (condicional) |
| `FACEBOOK_CLIENT_SECRET` | OAuth2 Facebook client secret (condicional) |
| `OAUTH2_REDIRECT_URI` | URI base para redirect OAuth2 |
| `APP_FRONTEND_URL` | URL del frontend para redirect post-OAuth2 |

Produccion: `PROD_HOST`, `PROD_USER`, `PROD_SSH_KEY`, `CORS_ALLOWED_ORIGINS_PROD`.

---

## Infraestructura

| Recurso | Valor |
|---------|-------|
| Shape | VM.Standard.A1.Flex (ARM) |
| OCPUs | 2 |
| RAM | 12 GB |
| Storage | 50 GB |
| Puertos abiertos | 22 (SSH), 80 (HTTP), 443 (HTTPS) |

**Redes Docker:**
- `pymes-internal-network` — DB, Redis, backend (bridge)
- `proxy-caddy-network` — Caddy reverse proxy (externa)

---

## Known Issues

- ~~**CORS en Gateway** (403 "Invalid CORS request" en POST): **resuelto 2026-08-11**. La causa raíz era el perfil Maven `dev` horneado en el auth-service (`allowed-origins: localhost`); el perfil ahora se inyecta en runtime via `SPRING_PROFILES_ACTIVE`. Ver daily reports de gateway/auth — 2026-08-11.~~

---

## Roadmap

### Fase 1: MVP (Q2 2026)
- [x] Core Auth (OAuth2, JWT, RBAC, invitaciones, password reset)
- [x] Auth local (registro/login usuario+password)
- [x] OAuth2 Google (intent cookie + code exchange)
- [x] Facebook OAuth2 (condicional, pendiente verificacion Meta)
- [x] Email system (Thymeleaf templates)
- [x] API Gateway (JWT validation, Swagger aggregation)
- [x] Test suite (462 tests, Testcontainers)
- [x] Core Business Service (gastos, prestamos, inversiones, ventas, accounting)
- [ ] Escaneo QR facturas (PWA)
- [ ] IA basica (deteccion de anomalias)

### Fase 2: Beta (Q3 2026)
- [ ] 10-20 negocios piloto
- [ ] Feedback y ajustes
- [ ] Modelos IA con datos reales

### Fase 3: Lanzamiento (Q4 2026)
- [ ] Marketing PYMEs Panama/LATAM
- [ ] Integracion Claude API
- [ ] Certificaciones seguridad

---

## Contribuir

1. Fork del repositorio
2. Crear rama `feature/nueva-funcionalidad`
3. PR a `develop`
4. CI debe pasar antes de merge

---

## Estructura

```
pymes-admin/
├── docker-compose.yml
├── backend/
│   ├── auth/            # Spring Boot — OAuth2 + JWT + RBAC + Email
│   ├── gateway-pymes/   # Spring Cloud Gateway (WebFlux)
│   └── core/            # Spring Boot — gastos, prestamos, ventas, accounting
├── frontend/
│   └── pymes/           # Quasar 2 PWA
├── docs/                # Estrategias, daily reports, testcontainers
├── .github/workflows/   # CI/CD pipelines
└── scripts/             # Setup y deploy
```

---

**Licencia:** Apache 2.0 — ver [LICENSE](LICENSE)

[![CI/CD](https://github.com/dio-quincarDev/pymes-admin/actions/workflows/ci.yml/badge.svg)](https://github.com/dio-quincarDev/pymes-admin/actions)
