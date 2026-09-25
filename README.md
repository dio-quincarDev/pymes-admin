# Pymeq — Forensic Accounting for Small Business

> Multi-tenant SaaS that turns invoices, costs, sales and loans into a 0-100 financial health score you can act on. Built for LATAM SMEs.

![Quasar](https://img.shields.io/badge/Quasar-2.16-16B5CA?style=flat-square&logo=quasar&logoColor=white)
![Vue](https://img.shields.io/badge/Vue-3.5-42B883?style=flat-square&logo=vue.js&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.5%20%2F%203.4-6DB33F?style=flat-square&logo=springboot&logoColor=white)
![Gateway](https://img.shields.io/badge/Gateway-WebFlux-6DB33F?style=flat-square)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-336791?style=flat-square&logo=postgresql&logoColor=white)
![Redis](https://img.shields.io/badge/Redis-7-DC382D?style=flat-square&logo=redis&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-multi--arch-2496ED?style=flat-square&logo=docker&logoColor=white)
![PWA](https://img.shields.io/badge/PWA-enabled-5A0FC8?style=flat-square)
![CI](https://github.com/dio-quincarDev/pymes-admin/actions/workflows/ci.yml/badge.svg)
![License](https://img.shields.io/badge/License-Apache_2.0-blue?style=flat-square)

---

## Why it helps

- **Clarity in one number:** 9 engines (ABC Pareto, trend, margin, opex, projection, 3 alerts, 3 supplier checks) plus a monthly investment KPI roll up into a Financial Health score 0-100 with plain-language guidance.
- **Tax confidence:** Per-item ITBMS 0/7/10% (Panama DGI), default 0% exempt, HALF_UP rounding and a Sin/Con ITBMS breakdown that matches backend to frontend.
- **No duplicates:** Every invoice POST carries an Idempotency-Key — same key within 6 hours replays the same result, not a second invoice. Invoice numbers are locked per tenant.
- **Only your data:** JWT at the gateway plus tenant and role checks — each request only sees its own tenant and only owners can delete or revert sensitive records. Metrics only count paid invoices.

## When to be alert

Real signals already wired in `AnalyticsService`. If you see one, act.

| Signal | What it means | What to do |
|---|---|---|
| `NEGATIVE_OPERATING_MARGIN` | Spending more than you sell | Cut operating costs now |
| `MARGIN_EROSION` | Margin falling for 2 periods | Renegotiate suppliers / price |
| `SUPPLIER_CONCENTRATION >60%` | One supplier owns most spend | Add alternatives |
| `OVER_LEVERAGED >3x` | Debt more than 3x monthly income | Pause new debt |
| `OPEX_CREEP +15% MoM` | Recurring costs jumped | Audit subscriptions |
| `DEAD_INVENTORY >90d` | Product with no sales | Promote or discontinue |
| `PAYBACK >24 months` | Capital + active debt takes too long to recover | Delay capital expense |
| `DAILY_COST >1.2x sales` | A day burned more than it sold | Immediate review |
| `Score <40 / 100` | Composite health is weak | Follow the in-app criolla guide |

---

## How it works

| Layer | What it does | Port |
|---|---|---|
| Frontend | Quasar 2.16 PWA (hash routing, offline cache, auto-update) | 9200 |
| Gateway | WebFlux gateway — validates JWT, injects identity headers, aggregates Swagger | 8080 |
| Auth | OAuth2 Google + JWT refresh rotation + RBAC + email (Thymeleaf) | 8081 |
| Core | Business modules, 44 endpoints `V1..V6`, idempotency + tenant/role checks | 8082 |
| Data | One PostgreSQL 15 `pymes_db` with two isolated schemas `auth` / `core` + Redis 7 (blacklist, permissions, analytics cache) | 5432 / 6379 |

Gateway forwards `/api/v1/auth/**` to Auth and `/api/v1/core/**` to Core. Auth and Core share the same database instance but never touch each other's schema. See `AGENTS.md` for the full architecture and `docs/strategies/DB_STRATEGY.md` for the single-DB rationale.

More detail per service: [`backend/auth/README.md`](backend/auth/README.md) · [`backend/gateway-pymes/README.md`](backend/gateway-pymes/README.md) · [`backend/core/README.md`](backend/core/README.md) · [`frontend/pymes/README.md`](frontend/pymes/README.md)

---

## Infrastructure

Runs on Oracle Cloud Free Tier and scales without extra database servers.

- **Shape:** `VM.Standard.A1.Flex` ARM — 2 OCPUs, 12 GB RAM, 50 GB storage. Only ports 22/80 open; 443 terminates at Cloudflare (mode Full, Bot Fight Mode off). Caddy listens on `http://` and proxies `gateway:8080` or `frontend:9200`.
- **Memory:** Each backend runs with `JAVA_OPTS -Xmx384m -Xms256m -XX:MaxMetaspaceSize=128m -XX:+UseG1GC` so three services fit on one small VM.
- **Networks:** `proxy-caddy-network` (Caddy ↔ frontend/gateway) and `pymes-internal-network` (DB/Redis/backends), both external. Caddy uses `path_regexp \.svg$` and `Cache-Control: no-cache` for the service worker, SVGs and HTML; purge Cloudflare cache after changing logos.
- **Delivery:** Every image builds for `linux/amd64,linux/arm64` via QEMU/Buildx. Health checks every 10-15s, `docker image prune -af` and `max-size 10m x3` log rotation after each deploy. `infra/monitoring` ships VictoriaMetrics + Grafana.
- **Data:** One PostgreSQL holds both `auth` and `core` schemas (Flyway `spring.flyway.schemas: auth` / `core`) — saves ~500 MB RAM and keeps backups to one dump. See `docs/strategies/INFRA_STRATEGY.md`.

---

## CI/CD

| Branch | What runs | What deploys |
|---|---|---|
| `feature/**` | CI only | Nothing |
| `develop` | CI + CD Staging | Staging on OCI |
| `main` | CI + CD Production | Production |

PRs target `develop`, never `main`.

- **CI** (`.github/workflows/ci.yml`): pushes or PRs to `main/develop/feature/**` on `backend/**, frontend/**, docker-compose*.yml, .github/workflows/ci.yml`. `feature/**` skips when only `**/*.md` changed. `concurrency: cancel-in-progress` cancels stale runs. Jobs run as `security (.env/secrets scan) → auth unit → auth integration → core unit → core integration → gateway build → frontend lint+build` with Testcontainers where needed. `TESTCONTAINERS_RYUK_DISABLED=true` and `node_modules` is cached between runs.
- **CD Staging/Prod** (`.github/workflows/cd-staging.yml`, `cd-prod.yml`): triggered only when CI succeeds on `develop` / `main`. Builds a version `YYYYMMDD-<sha>`, pushes four multi-arch images `pymes-auth/core/gateway/frontend:staging|prod` and `:TAG` with `gha` cache, then over SSH copies `docker-compose.yml`, cleans the root-owned `infra/` directory, copies `infra/monitoring/**`, writes a `.env` from GitHub Secrets, and runs `docker compose pull && up -d --force-recreate --remove-orphans && image prune`. Staging and prod secrets are separate (`CORS_ALLOWED_ORIGINS_STAGING/PROD`, `STAGING_HOST/PROD_HOST`, etc. — see `.github/SECRETS.md`).

---

## Tests and Quality

Backend **507** + Frontend **33** = **536** checks (September 2026).

| Service | Unit | Integration | Consistency | Context | Total |
|---|---|---|---|---|---|
| Auth | 138 | 56 | 12 | 1 | 207 |
| Core | 85 | 62 | 104 JPA | 11 analytics + 1 context | 263 |
| Gateway | 9 | 22 | 5 | 1 | 37 |
| Frontend | 29 vitest + 4 e2e | — | — | — | 33 |

- Unit: Mockito, no Docker. Integration: Testcontainers PostgreSQL 15-alpine + Redis 7-alpine from `AbstractIntegrationTest`. Convention `*Test.java` vs `**/integration/**`.
- More coverage detail in each service README. E2E strategy (login, register→verify, onboarding, products, invoices, dashboard, navigation, logout) is documented in `docs/strategies/E2E_TESTING_STRATEGY.md` and runs on `develop/main` only.

```bash
# Auth — unit
cd backend/auth && ./mvnw test -B
# Auth — integration (needs Docker)
cd backend/auth && ./mvnw verify -B -Dspring.profiles.active=integration
# Core — unit / integration
cd backend/core && ./mvnw test -B
cd backend/core && ./mvnw verify -B -Dspring.profiles.active=integration
# Gateway — unit (no Docker)
cd backend/gateway-pymes && ./mvnw test -B
# Frontend — unit + e2e
cd frontend/pymes && npm run test
```

---

## Quick Start

```bash
# Frontend
cd frontend/pymes && npm install && npm run dev  # port 9200, VITE_API_URL=/api/v1

# Auth Service
cd backend/auth
cp .env.example .env  # fill real values
./mvnw spring-boot:run -Pdev  # port 8081

# Gateway
cd backend/gateway-pymes
./mvnw spring-boot:run -Pdev  # port 8080

# Core Service
cd backend/core
./mvnw spring-boot:run -Pdev  # port 8082

# Full stack
docker compose up -d  # needs root .env
```

See `AGENTS.md` for profiles (`dev` default, `stg`, `prod`, `integration`, `test`) and gotchas (Gateway is WebFlux, Auth/Core are MVC; Verifying PWA cache handling).

---

## Roadmap

**Q2 2026 MVP — done:** OAuth2 + JWT + RBAC, invitations, password reset, gateway JWT + Swagger, full core domain (setup, products, suppliers, invoices with ITBMS, costs, loans, sales, accounting, analytics), 536 checks.

**Q3 2026 Beta:** 10-20 pilot businesses, feedback, models with real data. Remaining: invoice QR scan (PWA), basic anomaly detection.

**Q4 2026 Launch:** Panama/LATAM rollout, Claude API integration, security certifications.

Pending small items: tenant onboarding `PUT/PATCH` (today only `GET/POST /onboarding`), `CategoryPicker.vue` (~40 categories), `ConfiguracionPage` full `PUT` round-trip, i18n + `og:image`, residual color polish (`red→negative`, `amber→warning`).

---

## Structure

```
pymes-admin/
├── docker-compose.yml
├── backend/
│   ├── auth/            # OAuth2 + JWT + RBAC + Email
│   ├── gateway-pymes/   # WebFlux gateway
│   └── core/            # Business domain (setup, products, suppliers, invoices, costs, sales, accounting, analytics)
├── frontend/
│   └── pymes/           # Quasar 2 PWA
├── docs/
│   ├── strategies/      # INFRA_STRATEGY, DB_STRATEGY, E2E_TESTING_STRATEGY
│   └── GAPS.md / TO_DO.md
├── .github/workflows/   # ci.yml, cd-staging.yml, cd-prod.yml
└── infra/monitoring/    # VictoriaMetrics + Grafana
```

---

**License:** Apache 2.0 — see [LICENSE](LICENSE)
