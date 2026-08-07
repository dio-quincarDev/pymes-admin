# Recuperación de Inversión — Estrategia (Core)

Estado: **implementado** (2026-08-02). Backend: señal `PAYBACK_RECOVERY` en `analisisSaludFinanciera`, `PrestamoRepository.findByTenantIdAndStatus`, `financialHealth` expuesto en `AnalyticsResponse`. Tests: 4 unit (rojo/verde/amarillo/deuda ACTIVA suma al tiempo) + 2 IT (rojo, deuda ACTIVA suma al tiempo). Frontend: pendiente — ver TO_DO.
Objetivo: reemplazar el concepto de "meses de capital de respaldo" (`CAPITAL_BURN`/`CAPITAL_READINESS`) por **tiempo de recuperación de la inversión** — la pregunta que un dueño de PYME sin formación financiera realmente se hace: *"¿en cuántos meses, al ritmo de mis ventas actuales, recupero la plata que metí y la que debo?"*.

> Relacionado: `EXPENSES_MODEL_STRATEGY.md` §7b (define el enfoque actual que se reemplaza), `COSTOS_ENGINE.md` (costo operativo diario), `FUTURE_MODULES.md` §4 (patrimonio e inversión).

## Problema

1. **`CAPITAL_BURN`/`CAPITAL_READINESS` parten de un supuesto irreal** — calculan `initialCapital / (costoOperativoDiario × 30)` (meses que el capital cubre los costos). Pero en la realidad el capital inicial **ya está gastado** (mercancía, activos, alquileres iniciales). Preguntarse "¿cuántos meses me cubre?" es una pregunta académica que el dueño no siente como suya.
2. **No usa las ventas** — el retorno de una inversión se mide contra la ganancia que el negocio genera, no contra el costo. Un negocio que vende mucho con margen negativo está "ocupado pero sin avanzar", y el sistema debe mostrarlo.
3. **Ignora la deuda** — el `remaining_balance` de los préstamos ACTIVOS (lo que aún se debe) no lo consume ningún motor. Para el dueño, su inversión y su deuda son **la misma plata** que tiene que recuperar/pagar.
4. **Jerga financiera** — el público objetivo (PYME sin experiencia financiera) no habla de "payback", "ROI" ni "margen neto". Necesita UNA frase en lenguaje humano y un semáforo.

## Concepto (una frase, en pantalla)

> **"A este ritmo, recuperas tu plata en X meses."**

Nada de "margen neto", "payback" o "ROI". Un solo número que responde *¿cuándo vuelvo a tener mi plata?*. Presentado como semáforo:

| Estado | Mensaje (texto de UI) | Color |
|--------|----------------------|-------|
| ✅ Bueno | "¡Buen ritmo! Recuperas tu inversión en ~X meses." | Verde |
| 🟡 Lento | "Vas lento: recuperarás tu inversión en X meses. Sube tus ventas o baja gastos." | Amarillo |
| 🔴 Perdiendo | "Estás perdiendo dinero y no recuperas tu inversión." | Rojo |

## Cálculo

```
plata a recuperar = capital inicial (patrimony.initial_capital)
                    + Σ saldo_pendiente de préstamos status = 'ACTIVO' (loans.remaining_balance)
ganancia mensual  = ingresos del mes (total_income) × (net_margin_pct / 100)
meses             = plata a recuperar ÷ ganancia mensual
```

### Reglas de decisión

| Condición | Resultado |
|-----------|-----------|
| `ganancia mensual ≤ 0` | 🔴 Rojo — nunca recupera (pierde dinero por cada venta) |
| `meses ≤ 12` | ✅ Verde — buen ritmo |
| `meses > 24` | 🟡 Amarillo — lento, recomendar subir ventas / bajar gastos |
| entre 12 y 24 | Neutro — no emite señal de alerta ni de expansión |

### Señales resultantes

- **Se eliminan**: `CAPITAL_BURN`, `CAPITAL_READINESS` (concepto equivocado).
- **Se crea**: una señal de salud que expone `meses` de recuperación + el estado del semáforo (`PAYBACK_RECOVERY` o nombre equivalente, con `monthsToRecover` y el mensaje humano).
- **Se mantienen**: `OVER_LEVERAGED`, `DEBT_CAPACITY`, `DEBT_CUSHION` — miden la cuota mensual de deuda contra el margen/ingresos, siguen siendo útiles en la vista detallada.
- Los críticos (`criticals`) y expansiones (`expansions`) se alimentan igual que hoy: rojo → `criticals`, verde → `expansions`, amarillo → solo recomendación (sin señal de expansión).

## Decisiones de diseño (ponytail)

1. **Un solo número, no dos** — capital y deuda se suman en `plata a recuperar`. Para el dueño son la misma plata; separarlos lo obliga a pensar como contador.
2. **Ganancia neta, no ventas** — se proyecta con el margen neto real del período. Vender sin ganar no recupera nada.
3. **Sin tablas nuevas** — solo lectura de `PatrimonioRepository` (ya existe, ya inyectado en `AnalyticsServiceImpl`) + `PrestamoRepository` (nuevo para `AnalyticsServiceImpl`, método para sumar `remaining_balance` de ACTIVOS).
4. **Sin migraciones** — no se toca esquema.
5. **Umbrales fijos** — 12 y 24 meses hardcodeados (constantes). Configurables cuando un usuario lo pida.

## Plan de implementación (backend)

> Completado (2026-08-02).

1. ✅ **`AnalyticsServiceImpl.analisisSaludFinanciera`** — reemplazado el bloque "Capital de respaldo" por el payback (`plata a recuperar` = capital + deuda ACTIVA; `meses` = plata ÷ ganancia mensual). Señal `PAYBACK_RECOVERY` alimenta `criticals`/`expansions`/`recommendations` según el semáforo, mensajes en lenguaje humano.
2. ✅ **`PrestamoRepository`** — `findByTenantIdAndStatus(UUID, EstadoPrestamo.ACTIVO)` (derived query). `@Where(is_active=true)` excluye prestamos soft-deleted.
3. ✅ **Tests**:
   - `AnalyticsServiceImplTest`: 4 casos (rojo → crítica, verde → expansión, amarillo → solo recomendación, deuda ACTIVA suma al tiempo).
   - `ModeloGastosIntegrationTest`: `paybackRecovery_perdidaSeActiva` (crítica con margen negativo) + `paybackRecovery_deudaActivaSumaAlTiempo` (deuda ACTIVA suma, `current=0.50`).
4. ✅ **Docs**: actualizadas — TO_DO, esta estrategia, COSTOS_ENGINE.md changelog, DAYLY_REPORTS_CORE_SOLUTIONS.md, EXPENSES_MODEL_STRATEGY.md §7b.

## Fuera de alcance

- Frontend (se mueve a TO_DO como pendiente, consistente con el resto del cluster).
- Cambiar los umbrales 12/24 meses a configuración — add cuando un usuario lo pida.
- Depuración de `operating_expenses` (tabla muerta) — ver `CORE_MIGRATIONS_STRATEGY.md`.
- Desglose separado "recupero inversión" vs "pago deuda" — decisión explícita de mantener UN número.

## Archivos afectados

- `backend/core/src/main/java/core_pymes/analytics/service/impl/AnalyticsServiceImpl.java`
- `backend/core/src/main/java/core_pymes/prestamo/repository/PrestamoRepository.java`
- `backend/core/src/main/java/core_pymes/inversion/service/impl/PatrimonioServiceImpl.java` (solo lectura, ya existe)
- Tests: `backend/core/src/test/java/core_pymes/analytics/service/impl/AnalyticsServiceImplTest.java`, `backend/core/src/test/java/core_pymes/integration/ModeloGastosIntegrationTest.java`
- Docs: `DAYLY_REPORTS_CORE_SOLUTIONS.md`, `TO_DO.md`, `COSTOS_ENGINE.md`, `EXPENSES_MODEL_STRATEGY.md`
