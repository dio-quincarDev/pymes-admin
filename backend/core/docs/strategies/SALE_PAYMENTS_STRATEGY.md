# Activos de venta — desglose por método de pago (Core)

Estado: **plan cerrado, sin código** (2026-10-27). Validado contra staging (`vm2-test2`, `pymes_db`, profile `stg`).
Objetivo: que el activo (efectivo, Yappy, tarjeta, ACH…) deje de vivir como texto libre en `core.daily_sales.description` y pase a ser dato estructurado, sin perder ni reescribir un solo histórico.

> Relacionado: `CORE.md` (§Flyway), `docs/strategies/CORE_MIGRATIONS_STRATEGY.md` (por qué V9 y no consolidar), `DAILY_REPORTS_CORE_SOLUTIONS.md` (bitácora).

## Problema

Una venta diaria es **una fila por día** con un `gross_amount` y una `description` libre donde el dueño escribe el desglose a mano:

```
Efectivo: 119.15, Yappy: 317.65, PCobro: 30.00     -- gross_amount = 465.75
```

Ese texto no es consultable: no se puede sumar por activo, ni comparar meses, ni detectar que los números no cuadran. En staging **20 de 43 días (47%) no sumaban el total**, con huecos de B/.0,05 a B/.132,30; uno de ellos tenía el total del día copiado como si fuera el monto de tarjeta.

## Evidencia en staging (2026-10-27)

- 45 ventas, **1 tenant** (`restaurante`), 2026-09-01 → 2026-10-23, **B/.14.543,08**, una fila por fecha (45 fechas, 0 duplicadas).
- 2 filas sin descripción: `2026-09-30` (B/.451,05) y `2026-10-10` (B/.377,55) → B/.828,60 sin desglose posible.
- 43 filas con texto → B/.13.714,48.

### Vocabulario real (43 filas, 134 líneas)

| Etiqueta escrita | Líneas | Enum |
|---|---|---|
| `efectivo` | 43 | `EFECTIVO` |
| `yappy` 41 · `yappi` 2 · `ben` 3 | 46 | `YAPPY` |
| `pventa` 31 · `punto de venta` 5 · `punto venta` 3 · `p.venta` 1 · `pcobro` 3 | 43 | `TARJETA` |
| `ach` | 2 | `ACH` |

`BEN`/`BAN` = Yappy de otro banco. `PVenta` = `PCobro` = `Punto de Venta` = el mismo datáfono. Ambos confirmados por el usuario. `CHEQUE` y `CREDITO` **nunca** aparecen en ventas; entran al enum porque existen en gastos/facturas y otras empresas podrían usarlos.

### Limpieza previa (usuario, 2026-10-27)

Se corrigieron a mano las 22 filas descuadradas editando la descripción en la app. Estado final: **43/43 cuadran**, suma de desgloses = B/.13.714,48 = suma de `gross_amount`. **El backfill no necesita tolerancia ni recálculo.**

| Activo | Líneas | Monto |
|---|---|---|
| `EFECTIVO` | 43 | 6.967,28 |
| `YAPPY` | 46 | 5.372,37 |
| `TARJETA` | 43 | 1.356,78 |
| `ACH` | 2 | 18,05 |
| `CHEQUE` / `CREDITO` | 0 | 0,00 |

## Modelo

```
core.daily_sales              sin cambios: gross_amount sigue siendo el total del día
core.daily_sale_payments      (id, sale_id, metodo, monto)   <-- nuevo
```

- **Enum cerrado `MetodoPago`**: `EFECTIVO, ACH, YAPPY, TARJETA, CHEQUE, CREDITO`. Global, igual para todos los tenants.
- **Una fila por activo y día** (`UNIQUE (sale_id, metodo)`). Si el texto trae dos etiquetas del mismo enum (Yappy + Ben, pasa en 3 filas), el backfill las **suma** en una sola fila.
- **Invariante**: con desglose, `gross_amount = SUM(monto)` y lo calcula el backend. Sin desglose (filas viejas o sin texto), `gross_amount` manda y la UI muestra "sin desglose".

## Migración `V9__sale_payments_breakdown.sql`

Aditiva. **No se consolida en V1** (ver `CORE_MIGRATIONS_STRATEGY.md`): no hay redundancia ni contradicción que ganar, reescribir V1 rompe el checksum en toda DB con history y el CD no corre `flyway repair`.

```sql
CREATE TABLE IF NOT EXISTS core.daily_sale_payments (
    id      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sale_id UUID NOT NULL REFERENCES core.daily_sales(id) ON DELETE CASCADE,
    metodo  VARCHAR(20) NOT NULL CHECK (metodo IN ('EFECTIVO','ACH','YAPPY','TARJETA','CHEQUE','CREDITO')),
    monto   DECIMAL(12,2) NOT NULL CHECK (monto > 0),
    UNIQUE (sale_id, metodo)
);
CREATE INDEX IF NOT EXISTS idx_daily_sale_payments_sale ON core.daily_sale_payments(sale_id);
```

### Backfill

Whitelist de etiquetas **hardcodeada en SQL**: es una data migration congelada, no depende de `template_payment_methods` (lo siembra `SeedDataRunner` en Java, después de Flyway).

- Separa por `,` y por `. ` cuando sigue una letra (`yappi: 87.95. Punto de venta: 8.00`).
- Acepta `;` como separador etiqueta/monto (`PVenta;26.75`).
- Agrupa por enum con `SUM` y respeta el `UNIQUE`.
- **Sólo inserta los días cuyo desglose suma exacto `gross_amount`** (guard de seguridad; hoy 43/43). Si un día no cuadra, no se inserta nada para ese día.
- No toca `description` ni `gross_amount`.

## API

- `VentaRequest` → `desglose: [{ metodo: MetodoPago, monto: BigDecimal }]` + `descripcion` (notas). `montoBruto` queda **opcional**: sin desglose se usa tal cual, para no romper un bundle PWA cacheado.
- `VentaResponse` → incluye `desglose`.
- `VentaDiaria` gana la relación a `DailySalePayment` con `@Enumerated(EnumType.STRING)`, patrón `CategoriaGasto`.
- Sin endpoints nuevos: el desglose viaja con la venta. Un valor de enum inválido lo corta Jackson con 400, sin validación extra.

## Frontend

- `types/index.ts`: `export type MetodoPago = 'EFECTIVO' | 'ACH' | 'YAPPY' | 'TARJETA' | 'CHEQUE' | 'CREDITO'` (convención de `EstadoFactura`).
- `utils/metodosPago.ts`: **única fuente** de opciones y labels (`TARJETA` → "Tarjeta (PVenta/PCobro)"). Razón: `CategoriaGasto` ya está copiada a mano en `GastosPage.vue`, `CostosPage.vue` y `FacturasPage.vue`.
- `VentaFormDialog.vue`: un input por activo con el total corriendo; se elimina el "monto bruto" libre.
- `RegistrarVentaInlineBar.vue`: total + un activo (default `EFECTIVO`).
- `useVentas.ts`: `DayGroup.byMethod`; desglose por activo en el día y en el mes.

## Tests

- JPA (`VentaRepositoryTest`): guardar/leer desglose; el `CHECK` rechaza un enum inválido; el `UNIQUE (sale_id, metodo)` rechaza duplicado.
- Frontend (`utils/__tests__`): cada valor de `MetodoPago` tiene label y las opciones cubren el enum completo.

## Riesgos aceptados

1. **Enum cerrado**: agregar un activo = PR + migración + deploy; ninguna empresa personaliza la lista. Decisión explícita del usuario.
2. **PWA cacheada**: un bundle viejo puede postear sin `desglose`; por eso `montoBruto` se sigue aceptando.
3. **Momento del backfill**: lee la descripción tal como esté cuando corre la migración. Si se siguen corrigiendo días viejos, correrlo con la limpieza terminada (hoy: 43/43).

## Lo que no entra

- Catálogo de activos por tenant (descartado frente al enum).
- Aplicar el enum a gastos/facturas: `invoices`, `operating_expenses`, `loan_payments` y `gastos_fijos_recurrentes` siguen con `payment_method VARCHAR(50)` libre.
- Cierre de caja inmutable o bloqueo de días ya cerrados.
- `template_payment_methods` (semilla muerta: el onboarding sólo copia productos y presentaciones).

## Verificación

```bash
cd backend/core && mvn clean && ./mvnw test -B      # clean obligatorio: target/ cachea migraciones
cd frontend/pymes && npm run lint && npm run build
```

Tras el deploy: Clear site data en el navegador (gotcha PWA del repo).
