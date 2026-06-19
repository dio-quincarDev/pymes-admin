# Pymeq — SaaS Financial Management Platform

> Multi-tenant SaaS platform con toolkit de contabilidad forense impulsado por IA para PYMEs LATAM.

---

## Stack

| Componente | Tecnologia |
|------------|------------|
| Frontend | Quasar 2 + Vue 3 + TypeScript (PWA, hash routing) |
| Auth Service | Java 21 + Spring Boot 3.4.3 (OAuth2 + JWT + RBAC + Thymeleaf email) |
| Gateway | Spring Cloud Gateway (WebFlux) — JWT validation + Swagger aggregation |
| Database | PostgreSQL 15 (single instance, multi-schema: `auth`, `core`) |
| Cache | Redis 7 (blacklist + permissions cache) |
| CI/CD | GitHub Actions + Docker (multi-arch AMD64/ARM64) |
| Infra | Oracle Cloud Free Tier (ARM64) |

Ver `AGENTS.md` para arquitectura detallada y comandos de desarrollo.

---

## Quick Start

```bash
# Frontend
cd frontend/pymes && npm install && npm run dev  # port 9200

# Backend Auth
cd backend/auth
cp .env.example .env  # editar con valores reales
./mvnw spring-boot:run -Pdev  # port 8081

# Gateway
cd backend/gateway-pymes
./mvnw spring-boot:run -Pdev  # port 8080

# Docker (full stack: frontend + gateway + auth + postgres + redis)
docker compose up -d  # requiere .env en raiz
```

---

## Test Suite

| Servicio | Unit | Integration | Consistency | Total |
|----------|------|-------------|-------------|-------|
| Auth Service | 100 | 43 | 10 | 153 |
| Gateway | 33 | — | — | 33 |
| **Total** | **133** | **43** | **10** | **186** |

### Ejecucion

```bash
# Auth — unitarios
cd backend/auth && ./mvnw test -B

# Auth — integracion (requiere Docker)
cd backend/auth && ./mvnw verify -B -Dspring.profiles.active=integration

# Gateway — unitarios (sin Docker)
cd backend/gateway-pymes && ./mvnw test -B
```

### Infraestructura

- **Unitarios**: Mockito, sin Docker
- **Integracion**: Testcontainers (PostgreSQL 15-alpine + Redis 7-alpine), `@DynamicPropertySource`
- **Base class**: `AbstractIntegrationTest` — lifecycle de containers, `@MockitoBean` en EmailService
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

### Secrets (GitHub Settings)

| Secret | Descripcion |
|--------|-------------|
| `DOCKER_USERNAME` | Usuario Docker Hub |
| `DOCKER_PASSWORD` | Access Token Docker Hub |
| `STAGING_HOST` | IP instancia OCI staging |
| `STAGING_USER` | `ubuntu` |
| `STAGING_SSH_KEY` | Llave privada SSH |

Produccion: `PROD_HOST`, `PROD_USER`, `PROD_SSH_KEY`.

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
- `pymes-global-network` — Nginx Proxy Manager (externa)
- `pymes-internal-network` — DB, Redis, backend (bridge)

---

## Known Issues

- **CORS en Gateway** (Spring Cloud Gateway 3.2.0+): OPTIONS (preflight) funciona pero POST retorna 403 "Invalid CORS request". El procesador interno de CORS intercepta antes de que `globalcors` procese. Ver `backend/gateway-pymes/docs/` para detalle.

---

## Roadmap

### Fase 1: MVP (Q2 2026)
- [x] Core Auth (OAuth2, JWT, RBAC, invitaciones, password reset)
- [x] Auth local (registro/login usuario+password)
- [x] OAuth2 Google (intent cookie + code exchange)
- [x] Email system (Thymeleaf templates)
- [x] API Gateway (JWT validation, Swagger aggregation)
- [x] Test suite (186 tests, Testcontainers)
- [ ] Core Business Service (gastos, ingresos, facturacion)
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
│   └── core/            # Business service ( scaffold )
├── frontend/
│   └── pymes/           # Quasar 2 PWA
├── docs/                # Estrategias, daily reports, testcontainers
├── .github/workflows/   # CI/CD pipelines
└── scripts/             # Setup y deploy
```

---

**Licencia:** Apache 2.0 — ver [LICENSE](LICENSE)

[![CI/CD](https://github.com/dio-quincarDev/pymes-admin/actions/workflows/ci.yml/badge.svg)](https://github.com/dio-quincarDev/pymes-admin/actions)
