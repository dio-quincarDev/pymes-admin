# Características

> **Estado general:** Implemented · **Última verificación:** 2026-11-22
> Solo funciones presentes en el código. Lo planeado vive en [Roadmap](roadmap.md).

## Negocio

| Función | Estado | Fuente |
|---------|--------|--------|
| Onboarding en 2 pasos con 8 rubros y plantilla previa | Implemented | `OnboardingPage.vue` |
| Productos y presentaciones (con factor de conversión) | Implemented | `core.product`, `ProductosPage.vue` |
| Proveedores | Implemented | `core.providers`, `ProveedoresPage.vue` |
| Facturas de compra con ITBMS por ítem (0/7/10%), exento/gravado y total | Implemented | `V6__itbms_per_item.sql`, `FacturasPage.vue` |
| Idempotencia en POST (clave válida 6 h) y numeración por tenant | Implemented | `IdempotencyFilter.java` |
| Ventas diarias | Implemented | `core.daily_sales`, `VentasPage.vue` |
| Gastos operativos y estructura de costos (frecuencias, día de ejecución) | Implemented | `V2__costos_engine.sql`, `CostosPage.vue` |
| Préstamos y pagos | Implemented | `core.loans`, `core.loan_payments` |
| Patrimonio / inversión | Implemented | `core.patrimony`, `PatrimonioPage.vue` |
| Contabilidad | Implemented | `AccountingPage.vue` |

## Inteligencia

| Función | Estado | Fuente |
|---------|--------|--------|
| 9 motores analíticos CTE (ABC, tendencias, márgenes, gasto variable, proyección, alertas + 3 de proveedores) | Implemented | `backend/core/docs/ANALYTICS.md` |
| Motor de salud financiera (pilar 10) con score 0-100 y 4 pilares | Implemented | `backend/core/docs/CORE.md`, `FINANCIAL_HEALTH_STRATEGY.md` |
| Alertas accionables en la UI, con drill-down a Análisis | Implemented | `DashboardPage.vue`, `FinancialHealthPanel` |
| Crecimiento sobre meses cerrados; "Aún no hay historial" con <3 meses | Implemented | `AnalyticsServiceImpl.java` |
| Reporte mensual y trimestral en PDF, enviado al OWNER el día 1 06:00 Panamá | Implemented | `core.report_log`, `MonthlyReportScheduler.java` |
| Exportación XLSX del reporte | Planned → [Roadmap](roadmap.md) | `MONTHLY_REPORT_STRATEGY.md` |

## Plataforma

| Función | Estado | Fuente |
|---------|--------|--------|
| Registro local, verificación de correo, olvido/reset de contraseña | Implemented | `backend/auth` |
| Ingreso con Google y Facebook | Implemented | `CustomOAuth2UserService.java` |
| JWT con rotación de refresh y blacklist en Redis | Implemented | `JwtServiceImpl.java`, `TokenBlacklistService.java` |
| RBAC de 4 roles e invitaciones por tenant | Implemented | `RoleName.java`, `MemberApi.java` |
| Aislamiento de tenant validado en cada request | Implemented | `TenantValidationFilter.java` |
| PWA offline, caché y auto-actualización | Implemented | `frontend/pymes/docs/PWA_OFFLINE.md` |
| Tutorial guiado (7 pasos) y pantallas legales BETA | Implemented | `frontend/pymes/docs/FUTURE.md` |
| Swagger agregado en el gateway (Auth) y spec propia de Core | Partial — Core no aparece en el Swagger del gateway (`/v3/api-docs/core` da 404) | `docs/api/README.md` |
| Facturación / cambio de plan | Planned → [Roadmap](roadmap.md) | `JwtServiceImpl.java` |

## Ver también

- [Arquitectura](../architecture/architecture.md) — componentes, flujo, datos y decisiones.
- [Contrato API](../api/README.md) — 62 operaciones de Core y 23 de Auth generadas desde el código.
- [Guía de usuario](../guides/user-guide.md) — cómo se usa.
