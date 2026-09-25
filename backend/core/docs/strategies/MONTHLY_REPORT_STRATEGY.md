# Reporte Mensual Automático — Estrategia (Core)

Estado: **planificado** (2026-09-25).

Objetivo: el día 1 de cada mes a las 06:00 `America/Panama`, cada OWNER de cada
tenant activo recibe por mail el resumen en PDF (lectura del dueño) + XLSX
(contable), con brand PymeQ. Cero intervención manual, cero proyecciones:
solo lo realmente cargado en el mes cerrado.

> Relacionado: `ANALYTICS.md` (motores CTE que alimentan los datos),
> `COSTOS_ENGINE.md` (costo operativo diario), `FUTURE_MODULES.md` §Spring AI
> (fase 2: párrafo IA opcional), `CORE_MIGRATIONS_STRATEGY.md` (flyway en core).

## Hechos verificados en entorno staging (2026-09-25)

Inspección read-only del entorno de staging:

| Dato | Valor verificado |
|---|---|
| Flyway core | **V1–V6** aplicadas → siguiente `V7__report_log.sql` (NO V2) |
| TZ container core | **UTC** → cron requiere `zone` explícita |
| `core` mail | Sin `spring-boot-starter-mail`, sin `JavaMailSender`, sin `MAIL_*` en compose (solo el container **auth** los tiene: puerto `587`, `MAIL_FROM=<remitente configurado>`) |
| `auth.tenants` | **Sin** `created_by`/`owner_id` (16 columnas verificadas) |
| OWNER | `auth.user_tenants.role='OWNER'`; 4/4 tenants con exactamente 1 OWNER (invariante por código: `AuthServiceImpl:125`, `TenantServiceImpl:128`, bloqueo de segundo OWNER en `MemberServiceImpl:94-96`) |
| Owner query | EXPLAIN verificado: `Index Scan idx_user_tenants_tenant_active`, rows=1 |
| Índices para el reporte | Ya existen, **cero índices nuevos**: `idx_invoices_tenant_date_type` (covering `INCLUDE total`), `idx_daily_sales_tenant_date`, `uk_metrics_tenant_period` |
| Scheduling | `@EnableScheduling` ya activo (`EventConfig.java:13`); sin ShedLock |
| Feign | `@EnableFeignClients` activo + `services.auth.base-url: ${AUTH_SERVICE_URL}` **configurada y sin usar** |
| Deploy stage | Entorno de staging sin fuente (solo `.env`, `docker-compose.yml`, `infra/`); CD = scp + `docker compose pull` |
| Tenant con data (ejemplo ficticio) | `tenant-demo`: 15 ventas, 40 facturas, métricas `2026-09` (ingresos 8,500.00 / COGS 2,100.00 / opex 1,800.00 / neto 4,600.00). `tenant-sin-ventas`: 4 facturas, 0 ventas. Resto vacío. |

Correo del creador == OWNER (el registro crea `user_tenants` con `RoleName.OWNER`
en el mismo flow). Query batch — **multitenant desde el día 1, cero mails
hardcodeados**:

```sql
SELECT ut.tenant_id, u.email, t.name, t.timezone, t.currency
FROM auth.user_tenants ut
JOIN auth.users u ON u.id = ut.user_id
JOIN auth.tenants t ON t.id = ut.tenant_id
WHERE ut.role = 'OWNER' AND ut.is_active
  AND u.is_active AND u.deleted_at IS NULL
  AND t.is_active AND t.deleted_at IS NULL;
```

## Problema

El dueño no quiere abrir la app para saber cómo le fue el mes. Hoy ningún
proceso genera ni envía resumenes. El reporte debe llegar solo, leerse en 30
segundos y servirle a su contable sin pedirle nada más.

Regla de oro: **nunca proyectar**. Si el mes tiene 13 días cargados, el reporte
dice "$4,192.61 en 13 días cargados, faltan 17" — no "tu venta mensual es X".

## Concepto (una frase, en el mail)

> **"La Cafetería Central — Septiembre 2026: vendiste $8,500, te quedaron $4,600."**

PDF bonito para el dueño (1 página A4) + XLSX con detalle para el contable
(3 hojas). Ver ejemplo: `frontend/pymes/docs/DAILY_REPORT_FRONTEND.md 2026-09-19`.

## Cálculo

```
periodo        = mes anterior (yyyy-MM), límites sargables [from, to)
ingresos       = Σ daily_sales.gross_amount en [from, to)
costo_compras  = Σ invoices.total WHERE type='FACTURA' AND status<>'ANULADA'
gastos_oper.   = Σ invoices.total WHERE type='GASTO_OPERATIVO' AND status<>'ANULADA'
neto           = ingresos − costo_compras − gastos_oper.
semanas        = 1-7, 8-14, 15-21, 22-fin (no lunes-domingo: cualquier dueño lo entiende)
top_prov       = TOP 5 proveedores por Σ total FACTURA + count
```

### Reglas de decisión

| Condición | Resultado |
|---|---|
| `ventas = 0 AND facturas = 0` en el período | **SKIP** — no se envía, se loguea `SKIPPED` (cubre duplicados y tenants sin datos) |
| Hay facturas pero `ventas = 0` | Se envía **parcial**, banner "sin ventas registradas" |
| Hay data | PDF + XLSX completos |

## Decisiones de diseño (ponytail)

1. **Módulo interno `core_pymes/report/`** — sin microservicio nuevo. Un cron que
   corre 1 vez/mes no justifica deploy, Flyway, gateway route ni imagen extra.
2. **Owner vía opción A** — query batch cross-schema directa
   (`// ponytail: cross-schema read (auth), mover a Feign si auth se separa de DB`).
   Feign interno (opción B) queda como upgrade path: `@EnableFeignClients` y
   `services.auth.base-url` ya existen y están sin usar.
3. **Fuentes DejaVu** (ya en `jasperreports-fonts`) — acentos OK, cero TTFs embebidos.
4. **Compilación JRXML en runtime + cache en memoria** — sin `jasperreports-maven-plugin`
   (riesgo de compatibilidad con JR 7); el test de integración valida la plantilla.
5. **Idempotencia = PK de `report_log` + `ON CONFLICT DO NOTHING RETURNING`** —
   atómico y race-safe incluso con 2 réplicas. Sin ShedLock por ahora.
6. **Sin CSV** — decisión del usuario: solo PDF + XLSX. Sin subreportes. Test IT sí.
7. **Sin Spring AI** — fase 2: un `ChatClient` dentro del mismo job que agregue 2
   bullets al cuerpo del mail. Sin `pgvector`, sin nuevo deploy.

## Plan de implementación (backend)

1. **Deps** `backend/core/pom.xml`: `jasperreports:7.0.0`,
   `jasperreports-jackson:7.0.0`, `jasperreports-poi:7.0.0`,
   `jasperreports-fonts:6.0.0`, `spring-boot-starter-mail`.
2. **`V7__report_log.sql`**:
   ```sql
   CREATE TABLE core.report_log (
       tenant_id UUID        NOT NULL,
       period    VARCHAR(7)  NOT NULL,   -- '2026-09'
       format    VARCHAR(10) NOT NULL,   -- 'PDF' | 'XLSX'
       status    VARCHAR(10) NOT NULL DEFAULT 'SENDING', -- SENDING|SENT|FAILED|SKIPPED
       sent_at   TIMESTAMPTZ,
       error_msg TEXT,
       PRIMARY KEY (tenant_id, period, format)
   );
   CREATE INDEX idx_report_log_failed
     ON core.report_log (tenant_id, period) WHERE status = 'FAILED';
   ```
3. **`MonthlyReport.jrxml`** (diseñado en Jaspersoft Studio 7 — JRXML de v6 son
   incompatibles): bandas title/pageHeader/columnHeader/detail/summary/pageFooter;
   paleta `--pq-*` (`app.scss`); logo `frontend/pymes/public/icons/logo.svg`
   (Batik nativo JR — fallback: rasterizar a PNG si no resuelve); chart JFreeChart
   **2D** (v7/1.5.4 eliminó 3D) para ventas semanales; XLSX sin chart (JR no exporta
   charts a XLSX).
   XLSX en 3 hojas (`Resumen` KPIs · `Semanal` tabla · `Detalle` facturas+ventas)
   con `detectCellType=true` (numéricos reales para el contable).
4. **`ReportDataRepository`** — 2 queries batch (owners + CTE datos, sargable,
   columnas explícitas; reutiliza índices covering existentes).
5. **`MonthlyReportService`** — fill a PDF (`JasperExportManager`) y XLSX
   (`JRXlsxExporter`); plantilla compilada una vez y cacheada.
6. **`ReportEmailService`** — `JavaMailSender` + `MimeMessage` multipart: asunto
   `"<Tenant> — Resumen <mes> <año>"`, cuerpo HTML corto, 2 adjuntos.
7. **`MonthlyReportController`** — `POST /api/v1/core/reports/monthly?period=YYYY-MM&dryRun=true`,
   `@PreAuthorize("hasAnyRole('OWNER','ADMIN')")` (patrón existente).
8. **`MonthlyReportScheduler`** — `@Scheduled(cron="0 0 6 1 * *", zone="America/Panama")`.
   Flow por tenant: batch owners → `INSERT report_log ... ON CONFLICT DO NOTHING
   RETURNING` → sin fila = skip → CTE data → fill → mail → `UPDATE status
   SENT/FAILED`. Contador Prometheus (micrometer ya inyectado).
9. **`docker-compose.yml`** — agregar a `core-service` los `MAIL_*`
   (mismos valores OCI que auth; secretos por `.env`, nunca en el YAML).
10. **Tests** — `MonthlyReportIntegrationTest` (Testcontainers PG+Redis, seed
    estilo un tenant de prueba con seed ficticio de ventas/facturas/proveedores): asserts PDF bytes > 0, XLSX bytes > 0,
    SKIP sin data, conflicto de PK no duplica envío.

## Verificación

```bash
cd backend/core && ./mvnw test -B            # unit + integración (Testcontainers)
# stage: POST /api/v1/core/reports/monthly?period=2026-09&dryRun=true
# primer mail real solo tras aprobación explícita del dryRun
```

## Fuera de alcance

- Limpieza de tenants de prueba en el entorno de staging (tarea separada).
- JasperReports 2.x legacy / skill `etendo-report` / skill `ihlamury-design-skills-jasper`
  (ninguna aplica a Spring Boot 3 — verificado).
- iText (licencia AGPL incompatible con código comercial).
- Segundo OWNER, borrado de duplicados sin datos (innecesario: quedan SKIP por regla "sin data").
- ShedLock dedicado (add cuando `core` escale a 2+ réplicas).
- Fuentes custom (Geist/Satoshi) en el PDF — add si el dueño lo pide.

## Archivos afectados

Nuevos:
- `backend/core/src/main/resources/db/migration/V7__report_log.sql`
- `backend/core/src/main/resources/reports/MonthlyReport.jrxml`
- `backend/core/src/main/java/core_pymes/report/ReportDataRepository.java`
- `backend/core/src/main/java/core_pymes/report/MonthlyReportService.java`
- `backend/core/src/main/java/core_pymes/report/ReportEmailService.java`
- `backend/core/src/main/java/core_pymes/report/MonthlyReportController.java`
- `backend/core/src/main/java/core_pymes/report/MonthlyReportScheduler.java`
- `backend/core/src/test/java/core_pymes/report/MonthlyReportIntegrationTest.java`
- `backend/core/docs/strategies/MONTHLY_REPORT_STRATEGY.md` (este archivo)

Modificados:
- `backend/core/pom.xml` (deps JasperReports 7 + starter-mail)
- `docker-compose.yml` (`MAIL_*` en `core-service`)
