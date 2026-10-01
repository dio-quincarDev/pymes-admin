# Reporte Mensual Automático — Estrategia (Core)

Estado: **fase 1 (PDF) implementada** (2026-09-29). Fase 2 (XLSX) pendiente.

Objetivo: el día 1 de cada mes a las 06:00 `America/Panama`, cada OWNER de cada
tenant activo recibe por mail el resumen en PDF (lectura del dueño) + XLSX
(contable), con brand PymeQ. Cero intervención manual, cero proyecciones:
solo lo realmente cargado en el mes cerrado.

**Hecho hoy:** PDF en 1 página A4 + envío + idempotencia + `SKIPPED` + endpoint
`POST /api/v1/core/reportes/monthly` con `dryRun`. **Pendiente:** XLSX (3 hojas)
— la dependencia `jasperreports-excel-poi` ya está en el POM, se cambia el
exporter y se agrega el test.

> Relacionado: `ANALYTICS.md` (motores CTE que alimentan los datos),
> `COSTOS_ENGINE.md` (costo operativo diario), `FUTURE_MODULES.md` §Spring AI
> (fase 2: párrafo IA opcional), `CORE_MIGRATIONS_STRATEGY.md` (flyway en core).

## Hechos verificados en entorno staging (2026-09-25)

> **Actualizado 2026-09-29:** desde esta inspección se aplicaron **V7**
> (`V7__normalize_units.sql`, unidades) y **V8** (`V8__report_log.sql`, este
> reporte). La fila `Flyway V1–V6 → siguiente V7` de abajo quedó histórica;
> la migración de este feature es **V8**, no V7 (V7 se lo llevó el fix de unidades).

Inspección read-only del entorno de staging:

| Dato | Valor verificado |
|---|---|
| Flyway core | **V1–V6** aplicadas → siguiente `V7__report_log.sql` (NO V2) — *hoy: V8* |
| TZ container core | **UTC** → cron requiere `zone` explícita |
| `core` mail | *histórico:* sin `spring-boot-starter-mail`. **Hoy sí**: `spring-boot-starter-mail` en el POM, `JavaMailSender` en `ReportEmailServiceImpl`, `SPRING_MAIL_*` + `MAIL_FROM` ya en `docker-compose.yml` (`core-service`) |
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
    Requiere `net.sf.jasperreports:jasperreports-jdt` en el POM (misma versión
    `${jasperreports.version}`): sin él Jasper usa el `javac` del sistema, que
    **no existe** en la imagen `eclipse-temurin:21-jre-alpine` de producción.
    *Falencia documentada 2026-10-01:* el primer fix agregó solo
    `org.eclipse.jdt:ecj` suelto y staging siguió fallando con
    `Cannot run program "javac"`. Causa: en JR 7 la compilación se modularizó
    igual que el export a PDF — el núcleo ya no trae `JRJdtCompiler` y ECJ solo
    no sirve; se necesita el adaptador oficial `jasperreports-jdt` (trae ECJ
    transitivo). Evidencia: `JasperCompileManager` busca primero
    `net.sf.jasperreports.jdt.JRJdtCompiler` y si no está cae a `JRJavacCompiler`.
    *Optimización futura (no hacer ahora):* precompilar `.jrxml` → `.jasper` con
    el plugin Maven en el empaquetado eliminaría ECJ + jdt de producción, pero el
    ahorro es ~3 MB y segundos por arranque — no justifica la complejidad hoy.
    Reevaluar solo con decenas de plantillas o memoria muy justa.
5. **Idempotencia = PK de `report_log` + `ON CONFLICT DO NOTHING RETURNING`** —
   atómico y race-safe incluso con 2 réplicas. Sin ShedLock por ahora.
6. **Sin CSV** — decisión del usuario: solo PDF + XLSX. Sin subreportes. Test IT sí.
7. **Sin Spring AI** — fase 2: un `ChatClient` dentro del mismo job que agregue 2
   bullets al cuerpo del mail. Sin `pgvector`, sin nuevo deploy.

## Plan de implementación (backend)

1. **Deps** `backend/core/pom.xml` (hecho): `jasperreports`, `jasperreports-json`,
   `jasperreports-pdf`, `jasperreports-fonts`, `jasperreports-excel-poi`
    (todos **7.0.4**, vía propiedad `jasperreports.version`),
    `spring-boot-starter-mail` + `spring-dotenv:4.0.0` +
    `net.sf.jasperreports:jasperreports-jdt` (adaptador de compilación, ver punto 4).
   *OJO:* el artifact de fuentes es `jasperreports-fonts`, **no** `jasperreports-fonts:6.0.0`;
   el de Excel es `jasperreports-excel-poi`, no `jasperreports-poi`.
2. **`V8__report_log.sql`** (hecho):
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
3. **`MonthlyReport.jrxml`** (hecho — escrito a mano en **formato Jasper 7**,
   ver §Formato JR7 abajo): bandas background/title/summary/pageFooter;
   paleta locked de `.ulpi/design/DESIGN.md`; logo `pymeq-logo.svg` (recursos
   del reporte) inyectado como `InputStream`; fuentes **DejaVu Sans** de
   `jasperreports-fonts`. **Sin charts**: JFreeChart no está en `.m2` y 2
   tablas (semanas / top proveedores) cubren el contenido. XLSX (fase 2) no
   exporta charts igual.
4. **`ReportDataRepository`** (hecho) — owners cross-schema (`auth.*`, opción A)
   + mes con solo `PAGADA` en facturas (consistencia con analytics desde
   2026-09-08), sargable `[from,to)`, columnas explícitas.
5. **`MonthlyReportService`** (hecho) — fill PDF (`JasperExportManager`) con
   plantilla compilada una vez y cacheada. XLSX con `JRXlsxExporter` = fase 2.
6. **`ReportEmailService`** (hecho) — `JavaMailSender` + `MimeMessage`
   multipart. **1 adjunto** (PDF) hasta fase 2.
7. **`MonthlyReportController`** (hecho) —
   `POST /api/v1/core/reportes/monthly?period=YYYY-MM&dryRun=true`
   (`reportes`, no `reports`; ruta real en `CorePath.REPORTES_ROUTE`),
   `@PreAuthorize("hasAnyRole('OWNER','ADMIN')")` + `X-User-Role` del gateway.
8. **`MonthlyReportScheduler`** (hecho) —
   `@Scheduled(cron="0 0 6 1 * *", zone="America/Panama")`. Flow por tenant:
   batch owners → `INSERT ... ON CONFLICT DO NOTHING` → sin fila = skip →
   data → fill → mail → `UPDATE status SENT/FAILED|SKIPPED`.
   Contador `pymes_report_monthly_sent`.
9. **`docker-compose.yml`** — ya tenía `SPRING_MAIL_*` + `MAIL_FROM` en
   `core-service` (no hizo falta tocarlo).
10. **Tests** (hecho) — `MonthlyReportIntegrationTest` (4: PDF bytes `%PDF` +
    idempotencia + SKIP vacío + periodo inválido) y
    `MonthlyReportControllerTest` (11: roles 403, periodos 400, dryRun,
    repetidas). XLSX: test pendiente en fase 2.

### Formato Jasper 7 (hallazgo — JRXML v6 NO compila)

El JRXML se escribió a mano y **JR7 rechaza el formato v6**. Los cambios que
hicieron falta (todos en `MonthlyReport.jrxml`):

| JR6 | JR7 |
|---|---|
| `<jasperReport xmlns=...>` | raíz **sin namespace** (el loader exige `namespaceURI` vacío) |
| `<subDataset>` / `subDataset="X"` | `<dataset>` / `datasetName="X"` |
| `isBold="true"` | `bold="true"` |
| `<title><band>...</band></title>` | la sección **es** el `<band>` (sin wrapper) |
| `<reportElement x=... />` dentro de `<staticText>` | atributos **directos** en el elemento |
| `<textElement><font size="15"/></textElement>` | `fontSize="15"` directo en el elemento |
| `<textAlignment="Right">` | `hTextAlign="Right"` |
| `<imageExpression>` / `<textFieldExpression>` | `<expression>` |
| hijo suelto en la banda | `<element kind="rectangle\|staticText\|textField\|image\|line\|component">` |
| `<jr:table>` / `<jr:column>` / `<jr:columnHeader>` / `<jr:detailCell>` | `<component kind="table">` / `<column kind="single">` / `<columnHeader>` / `<detailCell>` |
| `datasetRun datasetName=` | `datasetRun subDataset=` |

Cómo se descubrió (método que vale para cualquier JR7): serializar objetos
`JRDesign*` con `JacksonUtil.getXmlMapper().writeValueAsString(...)` y copiar
la salida. `JacksonReportLoader.detectRootElement` solo acepta raíz sin
namespace — por eso un `xmlns` tarda en fallar con un error engañoso.

> **Ojo con el filtrado de Maven:** `src/main/resources` tiene
> `filtering=true` en core, así que el JRXML **no puede llevar `${}`** (el único
> que hay está dentro de un comentario).

## Verificación

```bash
cd backend/core && ./mvnw test -B            # unit + integración (Testcontainers)
# local/stage: POST /api/v1/core/reportes/monthly?period=2026-09&dryRun=true
#   header X-User-Role: OWNER (o ADMIN)
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
- **XLSX = fase 2** (no descartado): `jasperreports-excel-poi` ya en POM,
  solo falta cambiar el exporter + 3 hojas + test.
- Chart JFreeChart en el PDF (dependencia no presente; 2 tablas lo cubren).

## Archivos afectados (fase 1 — PDF)

Nuevos:
- `backend/core/src/main/resources/db/migration/V8__report_log.sql`
- `backend/core/src/main/resources/reports/MonthlyReport.jrxml`
- `backend/core/src/main/resources/reports/pymeq-logo.svg`
- `backend/core/src/main/java/core_pymes/report/`
  (`config/MonthlyReportScheduler`, `controller/MonthlyReportApi` +
  `controller/impl/MonthlyReportController`, `dto/{ReportData,ReportOwner}`,
  `exception/ReportGenerationException`, `repository/{ReportDataRepository,impl}`,
  `service/{MonthlyReportService,ReportEmailService,impl}`,
  `support/ReportLabels`)
- `backend/core/src/test/java/core_pymes/report/MonthlyReportIntegrationTest.java`
- `backend/core/src/test/java/core_pymes/report/MonthlyReportControllerTest.java`
- `backend/core/.env.example` (ver paso 0 abajo)
- `backend/core/docs/strategies/MONTHLY_REPORT_STRATEGY.md` (este archivo)

Modificados:
- `backend/core/pom.xml` (deps Jasper 7.0.4 + starter-mail + `spring-dotenv`)
- `backend/core/README.md` (módulo report, `.env`)
- `backend/core/docs/CORE.md` (módulo, paquetes, endpoint, V8)
- `backend/core/docs/DAILY_REPORTS_CORE_SOLUTIONS.md` (entrada 2026-09-29)
- `backend/core/src/test/java/core_pymes/integration/AbstractIntegrationTest.java`
  (`@MockBean ReportEmailService` — sin esto el contexto revienta en CI)
- `backend/core/src/main/java/core_pymes/common/exception/{CodigoError,CoreApiException}.java`
  (1 línea: `RPT001`; única excepción fuera de `report/`)

### Paso 0 (obligatorio para correr tests locales)

`./mvnw test -B` falla sin `.env`: el bean `ReportEmailServiceImpl` resuelve
`${app.mail.from:${spring.mail.username}}` al arrancar el contexto.

```bash
cd backend/core && cp .env.example .env   # y pon tus valores de mail/DB
```

En **CI no hace falta**: el `@MockBean ReportEmailService` de
`AbstractIntegrationTest` reemplaza el bean y la property nunca se evalúa.
