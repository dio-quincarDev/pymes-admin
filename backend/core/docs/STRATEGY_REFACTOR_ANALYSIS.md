# Estrategia de Refactorización: Motor de Inteligencia de Compras (Agnóstico)

> **REALITY CHECK (2026-06):** Este refactor ya está **parcialmente aplicado** en la base de código actual:
> - ✅ `ItemFactura` tiene `presentacionId` + `conversionFactor` — la normalización de precios ya se captura
> - ✅ No existe entidad `Inventario` ni control de stock físico (cero stock)
> - ✅ `AnalyticsServiceImpl` normaliza precios a unidad base via `unit_price / conversion_factor` en los CTEs
> - ✅ Catálogo basado en plantillas por industria (SeedDataRunner)
> - ⬜ Pendiente: renombrar formalmente `Producto` → `InsumoTemplate` si se desea

Este documento establece la ruta técnica oficial para transformar el módulo de inventario tradicional (Kárdex, existencias, stock físico) en un **Motor de Análisis de Gastos y Normalización de Precios** basado puramente en datos históricos y plantillas precargadas por industria.

---

## 1. El Cambio de Paradigma: Del Stock Físico a la Normalización Matemática

### Enfoque Anterior (Descartado)
* Controlar cantidades físicas en almacenes (`Inventario`).
* Registro estricto de entradas, salidas, mermas y ajustes.
* CRUD manual y complejo de productos específicos por cada cliente.

### Enfoque Actual (Validado)
* **Cero Stock:** No se calculan existencias disponibles.
* **Catálogo Precargado:** El cliente selecciona un insumo base de la plantilla de su industria (ej. *Pollo* dentro de *Perecederos → Proteínas*).
* **Normalización Matemática:** Las unidades de medida y presentaciones no controlan inventario físico; actúan como **factores de conversión** para unificar precios históricos y permitir comparativas exactas entre proveedores.

---

## 2. Modelado de Datos y Estructura Técnica

### A. Catálogo Base: `InsumoTemplate` y `UnidadBase`
Al inicializar el Tenant, las plantillas exponen los insumos con una unidad homogénea de comparación:
* **Insumo:** Elemento genérico (Ej: `Pollo`).
* **Unidad Base:** Métrica estándar asignada por el sistema para el análisis de esa categoría (Ej: `Lb` o `Kg`).

### B. El Detalle de la Compra: `ItemFactura`
Cuando el usuario registra una factura manual, se capturan las variables requeridas para la normalización:
* **Insumo Relacionado:** Vínculo al `InsumoTemplate` idóneo.
* **Cantidad Comprada:** El volumen físico de la transacción (Ej: `2`).
* **Unidad / Presentación de Compra:** La presentación real del proveedor (Ej: `Caja de 25 Lb` o `Bolsa de 5 Lb`).
* **Factor de Conversión:** Valor numérico que traduce la presentación de compra a la `Unidad Base`.
  * *Si la Unidad Base es "Lb" y la presentación es "Caja de 25 Lb", el Factor es `25.00`.*
  * *Si se compra directamente en la Unidad Base, el Factor es `1.00`.*
* **Precio Unitario de Compra:** El costo pactado por la presentación entera en esa factura.

---

## 3. Flujo de Normalización y Análisis Asíncrono

El motor analítico se activa inmediatamente después de la persistencia de los documentos comerciales, operando de manera asíncrona sobre hilos virtuales (**Java 21 Virtual Threads**).

```
[ Registro de Factura ]
          │
          ▼
[ ItemFactura: Persiste Cantidad, Unidad, Precio y Factor ]
          │
          ▼
[ Publicación: FacturaCreadaEvent ]
          │
          ▼
[ Async Listener (Virtual Threads) ]
          │
          ├──► 1. Normalizar Precios a la Unidad Base del Template
          │      Formula: Precio Base = Precio Compra / Factor
          │
          └──► 2. Ejecutar los 6 Motores Analíticos Agnósticos
```

### Algoritmo Core de Normalización
Para comparar proveedores que venden bajo diferentes presentaciones, el sistema calcula internamente el **Precio Unitario Base**:

$$\text{Precio Unitario Base} = \frac{\text{Precio Unitario de Compra}}{\text{Factor de Conversión}}$$

* **Ejemplo Práctico:**
  * **Proveedor A:** Vende 1 `Caja de 25 Lb` a **$48.75**. Factor = `25`. Precio Unitario Base = **$1.95 / Lb**.
  * **Proveedor B:** Vende `Libras sueltas` a **$1.90**. Factor = `1`. Precio Unitario Base = **$1.90 / Lb**.
  * **Resultado del Motor:** El sistema detecta y sugiere comprar al Proveedor B (Ahorro de **$0.05 / Lb**).

---

## 4. Los 6 Motores Analíticos (Agnósticos por Industria)

Una vez normalizado el precio, se ejecutan de forma paralela los siguientes análisis sobre el histórico de datos:

1. **ABC de Gastos (Pareto):** Clasifica los insumos acumulados en los últimos 30 días. Los insumos de categoría **A** representan el 80% del presupuesto de compras, permitiendo al comercio priorizar dónde negociar.
2. **Tendencia de Precios (Serie Temporal):** Compara el precio unitario base actual frente al promedio móvil de los últimos 90 días para identificar tendencias alcistas o bajistas y disparar alertas de oportunidad de compra.
3. **Impacto en Márgenes:** Evalúa el impacto directo de la fluctuación de precios de compra sobre los márgenes bruto, operativo y neto del negocio en comparación con el periodo previo.
4. **Costo Operativo como % de Ventas:** Relación porcentual entre el consolidado de facturas de compra y los ingresos totales por ventas dentro del mes en curso.
5. **Proyección de Gastos:** Simulación lineal del ritmo de gasto proyectado a 30, 60 y 90 días en función del promedio diario actual y las tendencias detectadas.
6. **Alertas de Cambios Significativos:** Identificación automática de anomalías estadísticas o eventos únicos (ej. primer registro de un proveedor, variación de precio superior al 15% o volúmenes de compra inusuales).

---

## 5. Ruta de Refactorización del Proyecto

Para aplicar este enfoque sobre la base actual sin comprometer la estabilidad del sistema, se ejecutará el plan en tres fases estrictas:

### Fase 1: Limpieza e Infraestructura (`inventario`)
* **Acción:** Retirar toda lógica asociada a control de existencias, stocks mínimos, almacenes físicos y kárdex de movimientos.
* **Acción:** Renombrar el catálogo de productos a `InsumoTemplate`. Modificar su estructura para que aloje únicamente el nombre genérico y la `Unidad Base` de comparación analítica.

### Fase 2: Adaptación Comercial (`facturas`)
* **Acción:** Actualizar la entidad `ItemFactura` para que deje de apuntar a un inventario físico y se vincule directamente al `InsumoTemplate`.
* **Acción:** Asegurar que el DTO de creación de facturas reciba explícitamente la unidad de compra seleccionada por el usuario y su respectivo factor de conversión matemática.

### Fase 3: Implementación del Motor Analítico (`contabilidad`)
* **Acción:** Crear la estructura de persistencia consolidada para almacenar los resultados indexados por periodos mensuales y identificadores de inquilinos (`tenant_id`).
* **Acción:** Desarrollar los queries nativos de agregación en base de datos para delegar el cálculo estadístico pesado a PostgreSQL, consumiendo estos datos eficientemente a través del listener asíncrono optimizado con hilos virtuales.
