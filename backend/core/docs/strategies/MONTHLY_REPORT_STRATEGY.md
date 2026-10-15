# Reporte Mensual Automático — Estrategia (Core)

Estado: **fase 1 (PDF 1 página) + etapa 2 (página 2: top productos, top gastos,
cierre de retorno) implementadas**. **Trimestral en diseño** (sin código).

Objetivo: el día 1 de cada mes a las 06:00 `America/Panama`, cada OWNER de cada
tenant activo recibe por mail el resumen en PDF (lectura del dueño) + XLSX
(contable), con brand PymeQ. Cero intervención manual, cero proyecciones:
solo lo realmente cargado en el mes cerrado.

**Hecho hoy:** PDF en 1 página A4 + envío + idempotencia + `SKIPPED` + endpoint
`POST /api/v1/core/reportes/monthly` con `dryRun`. **Etapa 2 (construida):**
página 2 con top 5 productos + top 3 gastos + cierre de retorno (ver §Etapa 2).
**Pendiente:** XLSX (3 hojas) — la dependencia `jasperreports-excel-poi` ya está
en el POM, se cambia el exporter y se agrega el test.

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
| `isStretchWithOverflow="true"` (JR6, ya no existe) | `textAdjust="StretchHeight"` (verificado 2026-10-15: el loader JR7 rechaza el nombre viejo con `UnrecognizedPropertyException`; el valor se confirma en `TextAdjustEnum`: `CutText`/`StretchHeight`/`ScaleFont`). El elemento siguiente lleva `positionType="Float"` para que baje cuando el texto se estira. Ojo al probar: una línea de ~150 caracteres cabe en 2 renglones y no se estira — el test usa la línea duplicada (~380) para forzar 3+ renglones y afirmar `height > diseño` + no-solape. |

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

## Etapa 2 — Página 2: top productos + top gastos + cierre de retorno (construida)

**Concluido con el dueño:** el PDF pasa a 2 páginas. La 1 queda igual; la 2 trae:

1. **Top 5 productos más comprados**, por cantidad de mayor a menor
   (ej: `Pechuga — 40 kg — $175.00`). Cantidades en unidad base
   (`quantity × conversion_factor`), sin precios promedio entre presentaciones.
   Desempate por monto.
2. **Top 3 gastos operativos**, por monto de mayor a menor
   (ej: `Salarios — $1,750.00`). Agrupados por categoría tal cual se registró.
3. **Cierre "Recupera tu inversión"** (2 líneas, en palabras normales):
   aporte del mes con ritmo (`Te quedaron $4,600: aparta $1,666.67 para el
   retorno de tu inversión inicial ($10,000) (ritmo 6 meses).`) + resto con
   rango de días y % del gasto (`Con $2,933.33 te alcanza para operar entre
   8 y 10 días (39% del gasto mensual).`). Mes que no dio → `Este mes no
   apartes: protege la caja.` Sin capital registrado → el bloque no aparece.

**Reglas:** solo lo pagado del mes cerrado, sin proyecciones; mismo correo
(+1 línea avisando lo nuevo) y mismo adjunto; mes vacío se salta igual.
Cada sección aparece solo si tiene filas (mes parcial: solo la que tenga
datos). Menos de 5/3: se muestran los que haya, sin relleno.

**Diseño técnico:** `ReportData` suma `TopProducto(nombre, cantidad, unidad, total)`
+ `TopGasto(nombre, total, facturas)` + `capital` (`core.patrimony`, null = ausente);
`LEFT JOIN products` (fallback a snapshot `product_name`) + `template_units` por
texto vía subquery (`LIMIT 1`, sin fan-out); unidad sin match → `"Unidad"`.
JRXML extiende `summary` con `printWhen` por sección (fluye a página 2, footer
ya numera). **Pivot (decisión del dueño):** el rango de días y el % salen del
motor de análisis en proceso — `AnalyticsService.proyeccionOperativa`
(`gastoMensual, gastoDiario, gastoMin, gastoMax`, 1 query dueña del gasto diario,
misma JVM sin HTTP). Días = `floor(resto / gastoMax)…floor(resto / gastoMin)`;
`% = resto / gastoMensual`. Sin gasto en el mes → solo la línea de aparte.
Cálculo del aporte en `report/support/RetornoInversion.java` (réplica de
`recuperacion.ts` del front: bandas 6m ≥ 38% · 12m ≥ 19% · 18m ≥ 13%, tope 70%),
puro y con unit test sin Docker (`RetornoInversionTest`, 5 casos).

## Etapa trimestral — PDF cara cliente + 2 XLSX (modelado 2026-10-10)

**Principio:** el trimestral lo lee el cliente (dueño), no el contable. Carta de
resultados, máx 2 páginas, lista para reenviar por WhatsApp/correo. Rango rodante:
los 3 meses que cierran el mes de envío.

**Regla dura:** prohibido atribuir ganancia por producto. Las ventas son totales
del día (sin porcionado por producto), así que no existe rentabilidad por producto.
La ganancia vale solo a nivel negocio (ventas − compras − gastos). El producto
estrella se define por volumen de compra, nunca por aporte a la ganancia.

**PDF trimestral (qué + qué hago, para leer en 2 minutos):**
- Página 1: frase resumen en 1 línea + 3 totales grandes (ingresos, gastos,
  ganancia del trimestre) + barras mes a mes ingresos vs gastos (mitad de la
  página, dibujo simple con elementos del template, sin librerías nuevas).
- Página 2: sección grande de consejos (media página o más), 2–3 consejos con su
  porqué en 1 línea, en palabras normales, solo sobre compras y gastos medidos.
  Cierra con comparativa ↑↓ del trimestre.
- Sin tablas mensuales, sin puntajes, sin tortas (eso vive en el Excel).

**XLSX contable:** 1 hoja `Mes | Proveedor | Facturas | Total`, una fila por
proveedor+mes, ordenada por fecha. Cero fórmulas raras.

**XLSX comportamiento + salud (el respaldo, no redundante con el PDF):**
- `Datos`: tabla mes a mes (ingresos, compras, gastos, ganancia, días cargados,
  salud 0–100). El detalle detrás de los totales del PDF.
- `Gráficos` (solo lo que el PDF no tiene): torta de composición del gasto del
  trimestre + línea de tendencia de ganancia. Sin barras ingresos-vs-gastos (ya
  están en el PDF). Generados con POI vía `jasperreports-excel-poi` (ya en el POM),
  sin JFreeChart (ausente).
- `Salud`: puntaje + pilares + todas las alertas y recomendaciones en texto simple
  (diagnóstico completo). Regla: el PDF dice qué hacer, el Excel dice por qué.
  Mes sin datos → `"sin datos"`, nunca inventar.
- Salud del motor existente (`core_pymes.analytics`) llamado en proceso, sin cableado
  nuevo: último mes del trimestre + delta vs hace 3 meses. Sin proyecciones.
- El XLSX mensual pendiente de Fase 2 queda absorbido: el contable lo cubre a nivel trimestre.

**Diseño técnico:** queries del mensual reutilizadas con rango `[from, to)` de 3 meses;
`period` trimestral convive en `report_log` sin migraciones (mismo tenant, período
distinto: mensual `YYYY-MM`, trimestral con formato propio). Cada archivo con su fila
en `report_log` (reintento independiente por formato).

**Envío:** el mensual va todos los meses para todos. El trimestral va cada 3 meses
por tenant, anclado a su primer registro (ej: tenant creado en febrero → cierres en
abr/jul/oct/ene). El scheduler del día 1 solo genera trimestral (+ 2 XLSX) a los
tenants que cumplen `meses_desde_primer_registro % 3 == 0`. El mensual de ese mes se
envía igual junto al paquete trimestral.

## Recuperación de inversión en reportes (diseño 2026-10-11)

**Idea:** el reporte le dice al dueño *"si tomaste las recomendaciones, debes haber
recuperado $X de $10,000 hasta el momento"*. Misma lógica de la tarjeta de
Patrimonio (`PatrimonioPage.vue`, sección "Plan de retorno"), repetida sobre meses
cerrados. Solo lo real: mes sin datos = 0 aportado, nunca inventar.

**Cálculo por mes cerrado (decisión final del dueño: cierre en mensual pág. 2,
formato corto):** aporte = fijo del plazo (`capitalInicial / plazo`), con tope
70% de la rentabilidad del mes; si el mes no dio (rentabilidad ≤ 0 o margen
< 13%), aporte 0 y línea `protege la caja`. Resto = rentabilidad − aporte;
días = `floor(resto / gastoMax)…floor(resto / gastoMin)` con el gasto
diario/min/max del pivot; `% = resto / gastoMensual`. Sin arrastres entre meses
(decisión del dueño 2026-10-11: la tarjeta es solo mes actual; el acumulado vive
en el trimestral, en diseño).
- Mensual pág. 2: `Te quedaron $X: aparta $Y para el retorno de tu inversión
  inicial ($Z) (ritmo N meses).` + `Con $R te alcanza para operar entre N y M
  días (P% del gasto mensual).`
- Trimestral pág. 1 (diseño, sin código): `recuperaste $Y de $Z` + aparte vigente
  + avance `Llevas recuperado $Y de $Z` (idea #2 del dueño: suma de aportes del
  rango, sale del historial guardado).
- Ritmo vigente = el calificado por el último mes cerrado (bandas 6m ≥ 38% ·
  12m ≥ 19% · 18m ≥ 13% · < 13% = protege la caja).

**Dónde vive:** PDF trimestral página 1, una línea debajo de los 3 totales
(es cara cliente y el tema es inversión). El mensual no se toca (lleno con etapa 2).
XLSX `Datos`: una columna más por mes (aporte recomendado + recuperado acumulado) —
el detalle detrás de la línea del PDF. Sin capital registrado → la sección no aparece.

**Diseño técnico (cuando se construya):** replay en proceso sobre
`tenant_financial_metrics` (`netMargin`, `netMarginPct` por período) + capital de
`core.patrimony`; reutiliza el mismo helper de cálculo que la tarjeta (un solo punto
de verdad). Sin migración, sin endpoint nuevo para la v1.
