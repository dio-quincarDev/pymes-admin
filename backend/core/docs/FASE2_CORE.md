# Estrategia: Fase 1 - Parte 2 (Simplificada)

> **REALITY CHECK (2026-06):** Versión simplificada del plan original.
> - ✅ Productos, Presentaciones, Facturas, Proveedores = implementados
> - ⬜ Ventas = NO implementado
> - ⬜ Contabilidad/métricas = NO implementado
> - ⬜ Reportes = NO implementado
> - 🔄 Analytics implementado (no listado aquí)

---

## 1. Alcance Exacto

**Qué sí:**
- Productos (CRUD, select del template)
- Presentaciones (factor de conversión básico)
- Facturas (registro manual)
- Ventas (monto diario manual)
- Márgenes (cálculo automático)
- Dashboard simple

**Qué no:**
- Análisis ABC, tendencias, proyecciones
- Normalización de precios complejos
- Alertas de anomalías
- Cualquier cosa que no sea CRUD + márgenes

---

## 2. Entidades (Mínimas)

```
Producto (del template)
├── id, nombre, categoría, unidad_base

Presentacion
├── id, producto_id, nombre, factor (Caja=24, etc)

Proveedor
├── id, nombre, ruc, contacto

Factura
├── id, proveedor_id, numero, fecha, total, tipo (FACTURA o GASTO)

ItemFactura
├── id, factura_id, producto_id, cantidad, precio_unitario, subtotal

Venta
├── id, fecha, monto_bruto

MetricaFinanciera (calculada, no editable)
├── tenant_id, periodo, total_ingresos, total_egresos, costo_mercancia
├── gastos_operativos, margen_bruto_%, margen_neto_%, etc
```

---

## 3. Eventos (Simples)

```
FacturaCreada
  → ContabilidadListener: Suma a CostoMercancia o GastosOperativos
  → Recalcula métricas
  → Publica MetricasCalculadas

VentaRegistrada
  → ContabilidadListener: Suma a Ingresos
  → Recalcula métricas
  → Publica MetricasCalculadas

MetricasCalculadas
  → ReportesListener: Actualiza dashboard
```

---

## 4. Endpoints (CRUD)

```
POST   /api/core/v1/productos
GET    /api/core/v1/productos
PUT    /api/core/v1/productos/{id}
DELETE /api/core/v1/productos/{id}

POST   /api/core/v1/presentaciones
GET    /api/core/v1/presentaciones
PUT    /api/core/v1/presentaciones/{id}
DELETE /api/core/v1/presentaciones/{id}

POST   /api/core/v1/proveedores
GET    /api/core/v1/proveedores
PUT    /api/core/v1/proveedores/{id}

POST   /api/core/v1/facturas
GET    /api/core/v1/facturas
PUT    /api/core/v1/facturas/{id}
DELETE /api/core/v1/facturas/{id}

POST   /api/core/v1/ventas
GET    /api/core/v1/ventas
PUT    /api/core/v1/ventas/{id}

GET    /api/core/v1/contabilidad/metricas
GET    /api/core/v1/reportes/dashboard
```

---

## 5. Listeners (Solo 2)

**ContabilidadEventListener:**
- Escucha FacturaCreada, VentaRegistrada
- Calcula márgenes básicos
- Publica MetricasCalculadas

**ReportesEventListener:**
- Escucha MetricasCalculadas
- Actualiza dashboard

---

## 6. Checklist Fase 1 - Parte 2

- [ ] Entidades JPA (6 entidades)
- [ ] Controllers CRUD (6 recursos)
- [ ] Services (lógica básica)
- [ ] 2 Event Listeners
- [ ] Transaccionalidad
- [ ] Tests unitarios básicos

---

** CORE **
