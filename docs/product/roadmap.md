# Roadmap

> **Última verificación:** 2026-11-22 · Fuente: `README.md`, `frontend/pymes/docs/FUTURE.md`,
> `backend/core/docs/CORE.md`, `docs/GAPS.md`.
> Aquí vive **todo lo planeado**. Lo implementado está en [Características](features.md).

## Q2 2026 — MVP ✅

OAuth2 + JWT + RBAC, invitaciones, reset de contraseña, gateway con JWT y Swagger,
dominio core completo (setup, productos, proveedores, facturas con ITBMS, costos,
préstamos, ventas, contabilidad, analítica), 536 checks.

## Q3 2026 — Beta (en curso)

| Ítem | Estado | Fuente |
|------|--------|--------|
| 10-20 negocios piloto, feedback, modelos con datos reales | En curso | `README.md` |
| Reporte mensual al dueño en PDF con envío automático | ✅ Implementado | `backend/core/docs/CORE.md` |
| Exportación XLSX del reporte | Planned | `MONTHLY_REPORT_STRATEGY.md` |
| Escaneo de QR en facturas (PWA) | Planned | `README.md` |
| Detección básica de anomalías | Planned | `README.md` |

## Q4 2026 — Lanzamiento

| Ítem | Estado |
|------|--------|
| Rollout Panamá / LATAM | Planned |
| Integración con Claude API | Planned |
| Certificaciones de seguridad | Planned |

## Pendientes pequeños

- `PUT/PATCH` de onboarding (hoy solo `GET/POST /onboarding`).
- `CategoryPicker.vue` (~40 categorías).
- `ConfiguracionPage` con ida/vuelta completa de `PUT`.
- i18n y `og:image`; ajuste residual de color (`red→negative`, `amber→warning`).
- Descuento en factura como porcentaje (hoy monto fijo).
- Precio unitario con factor de conversión desde el frontend.

`Source: README.md`, `docs/GAPS.md` (Frontend 3-4)

## Hardening diferido (aceptado, no bug)

Registrado en `docs/GAPS.md` con estado "Diferido (ponytail)": cache de usuarios
activos, hash de JWT como key Redis, rate limit en `/exchange`, Caffeine para la
blacklist, SSL en DB, password en Redis, logs con límite. Se activan solo si el
load o un incidente lo demanda.

## Ver también

- [Características](features.md) — lo que ya funciona.
- [Arquitectura](../architecture/architecture.md) — decisiones y contexto.
- [GAPS](../GAPS.md) — discrepancias documento/código con su estado.
