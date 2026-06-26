# Estrategia: Fase 1 - Parte 2 (Espacio de Trabajo - CRUD Manual)

> **REALITY CHECK (2026-06):** Documento de planificación original. Lo implementado difiere:
> - ✅ Productos + Presentaciones = implementado en `product/`
> - ✅ Facturas + Proveedores = implementado en `invoice/`
> - ⬜ Ventas = NO implementado
> - ⬜ Contabilidad/métricas = NO implementado
> - ⬜ Reportes/dashboard = NO implementado
> - 🔄 Analytics implementado (no estaba en este plan original)
> - Ver `PROGRESS.md` para estado actual preciso

---

## 1. Alcance Fase 1 - Parte 2

**Qué se desarrolla:**

Dentro de cada módulo, solo operaciones CRUD básicas:

- **Configuración:** Ver plantilla cargada (ya existe de Parte 1)
- **Inventario:** CRUD Productos + Presentaciones (sin movimientos aún)
- **Facturas:** CRUD Facturas manuales (sin QR/imagen)
- **Contabilidad:** Cálculos básicos (sin proyecciones)
- **Reportes:** Dashboards simples (sin analytics avanzados)

**Qué NO va aquí:**
- Captura QR/imagen (Fase 2)
- Movimientos automáticos de stock (Fase 2)
- Análisis predictivos (Fase 3)

---

## 2. Flujo por Módulo

### Módulo: Configuración

**Ya cargado de Parte 1.** Solo lectura en Parte 2:

```
GET /api/core/configuracion/{tenant_id}
  └── Retorna:
      - Categorías
      - Unidades
      - Ubicaciones
      - Motivos
```

**No hay CRUD de configuración en Parte 2.** Tenant edita después (Parte 3+).

---

### Módulo: Inventario

**CRUD: Productos y Presentaciones**

```
POST /api/core/productos
  ├── Nombre
  ├── SKU
  ├── Categoría (select de ConfiguracionTenant)
  ├── Unidad Base (select de ConfiguracionTenant)
  └── Imagen (opcional)
  └── Publica: ProductoCreado

GET /api/core/productos
  └── Lista todos del tenant

PUT /api/core/productos/{id}
  └── Edita producto

DELETE /api/core/productos/{id}
  └── Elimina (solo si no tiene inventario)
```

```
POST /api/core/productos/{id}/presentaciones
  ├── Nombre (Caja, Six Pack, etc)
  ├── Conversión (24 botellas = 1 caja)
  └── Publica: PresentacionCreada

GET /api/core/productos/{id}/presentaciones
  └── Lista presentaciones del producto

PUT /api/core/presentaciones/{id}
  └── Edita presentación

DELETE /api/core/presentaciones/{id}
  └── Elimina (solo si no tiene movimientos)
```

**No hay Inventario (stock) registrado aún.** Se crea con Facturas.

---

### Módulo: Facturas

**CRUD: Facturas y Proveedores (manual)**

```
POST /api/core/proveedores
  ├── Nombre
  ├── RUC
  ├── Contacto (opcional)
  └── Categoría (opcional)

GET /api/core/proveedores
  └── Lista proveedores del tenant

PUT /api/core/proveedores/{id}
  └── Edita proveedor

DELETE /api/core/proveedores/{id}
  └── Elimina (solo si no tiene facturas)
```

```
POST /api/core/facturas
  ├── Proveedor (select de proveedores)
  ├── Número (generado automático: F-PROV-YYYY-NNNN)
  ├── Fecha
  ├── Items: [{producto_id, cantidad, precio_unitario, descuento}]
  ├── Descuento global (opcional)
  ├── Método pago
  ├── Tipo: FACTURA (enum, vs GASTO_OPERATIVO)
  └── Publica: FacturaCreada

GET /api/core/facturas
  └── Lista todas del tenant (filtrable por estado, proveedor, fecha)

GET /api/core/facturas/{id}
  └── Detalle con items

PUT /api/core/facturas/{id}
  └── Edita (solo si estado=REGISTRADA)

DELETE /api/core/facturas/{id}
  └── Elimina (solo si estado=REGISTRADA)

POST /api/core/facturas/{id}/pagar
  └── Marca como PAGADA
  └── Publica: FacturaPagada
```

**Nota:** Tipo=GASTO_OPERATIVO usa mismo formulario, se diferencia en cálculos.

---

### Módulo: Ventas (Registro Manual)

**CRUD: Ventas diarias**

```
POST /api/core/ventas
  ├── Fecha
  ├── Monto bruto
  ├── Notas (opcional)
  └── Publica: VentaRegistrada

GET /api/core/ventas
  └── Lista ventas (por rango fecha, totalizado por día/semana/mes)

PUT /api/core/ventas/{id}
  └── Edita venta (solo si no está "cerrada")

DELETE /api/core/ventas/{id}
  └── Elimina venta
```

---

### Módulo: Contabilidad

**Cálculos automáticos (sin CRUD)**

```
GET /api/core/contabilidad/metricas?periodo=mensual&fecha=2026-03
  └── Retorna:
      - Total ingresos (suma ventas)
      - Total egresos (suma facturas + gastos)
      - Costo mercancía (facturas tipo FACTURA)
      - Gastos operativos (facturas tipo GASTO_OPERATIVO)
      - Margen bruto % y USD
      - Margen operativo % y USD
      - Margen neto % y USD
      - EBITDA
      - Punto equilibrio
```

**No hay edición de métricas.** Se calculan automáticamente al registrar facturas/ventas.

---

### Módulo: Reportes

**Lecturas simples (sin CRUD)**

```
GET /api/core/reportes/dashboard?tenant_id=...
  └── Retorna:
      - Total inversión actual
      - Alertas activas (stock crítico, margen bajo)
      - Últimas 10 facturas
      - Últimas 10 ventas
      - Resumen márgenes mes actual

GET /api/core/reportes/inventario?categoria=...
  └── Stock por producto/ubicación (cuando exista inventario - Parte 3+)

GET /api/core/reportes/contabilidad?periodo=...
  └── Márgenes, flujo caja, KPIs
```

---

## 3. Flujo de Eventos en Fase 1 - Parte 2

### Cuando se registra una Factura

```
Frontend POST /api/core/facturas (tipo=FACTURA)
  ↓
FacturaService persiste en BD
  ↓
FacturaService publica: FacturaCreada(factura_id, items, total)
  ↓
[Async, Virtual Thread 1] ContabilidadEventListener escucha
  └── Suma total a CostoMercancia
  └── Recalcula márgenes
  └── Publica: MetricasCalculadas
  
[Async, Virtual Thread 2] ReportesEventListener escucha
  └── Actualiza dashboard con nueva factura
  └── Recalcula totales en vista

[Async, Virtual Thread 3] InventarioEventListener escucha (Parte 2 aún no lo usa)
  └── (Preparado para Parte 3 cuando agregue movimientos)
```

**Timeline:** Frontend recibe respuesta inmediatamente. Cálculos ocurren en background.

---

### Cuando se registra una Venta

```
Frontend POST /api/core/ventas
  ↓
VentaService persiste en BD
  ↓
VentaService publica: VentaRegistrada(venta_id, monto, fecha)
  ↓
[Async] ContabilidadEventListener escucha
  └── Suma monto a Ingresos
  └── Recalcula márgenes
  └── Publica: MetricasCalculadas
  
[Async] ReportesEventListener escucha
  └── Actualiza dashboard
```

---

### Cuando se registra un Gasto Operativo

```
Frontend POST /api/core/facturas (tipo=GASTO_OPERATIVO)
  ↓
FacturaService persiste en BD
  ↓
FacturaService publica: FacturaCreada(factura_id, tipo=GASTO_OPERATIVO, monto)
  ↓
[Async] ContabilidadEventListener escucha
  └── Suma a GastosOperativos (no a CostoMercancia)
  └── Recalcula márgenes
  └── Valida: Si Margen < 5% → publica AlertaMargenBajo
  └── Publica: MetricasCalculadas
  
[Async] ReportesEventListener escucha
  └── Si AlertaMargenBajo, agrega alerta a dashboard
```

---

## 4. Estructura Controllers - Parte 2

### Configuración
```
GET /api/core/v1/configuracion/{tenant_id}
```

### Inventario
```
POST   /api/core/v1/productos
GET    /api/core/v1/productos
GET    /api/core/v1/productos/{id}
PUT    /api/core/v1/productos/{id}
DELETE /api/core/v1/productos/{id}

POST   /api/core/v1/productos/{id}/presentaciones
GET    /api/core/v1/presentaciones
PUT    /api/core/v1/presentaciones/{id}
DELETE /api/core/v1/presentaciones/{id}
```

### Facturas
```
POST   /api/core/v1/proveedores
GET    /api/core/v1/proveedores
PUT    /api/core/v1/proveedores/{id}
DELETE /api/core/v1/proveedores/{id}

POST   /api/core/v1/facturas
GET    /api/core/v1/facturas
GET    /api/core/v1/facturas/{id}
PUT    /api/core/v1/facturas/{id}
DELETE /api/core/v1/facturas/{id}
POST   /api/core/v1/facturas/{id}/pagar
```

### Ventas
```
POST   /api/core/v1/ventas
GET    /api/core/v1/ventas
PUT    /api/core/v1/ventas/{id}
DELETE /api/core/v1/ventas/{id}
```

### Contabilidad
```
GET    /api/core/v1/contabilidad/metricas
```

### Reportes
```
GET    /api/core/v1/reportes/dashboard
GET    /api/core/v1/reportes/contabilidad
```

---

## 5. Listeners - Parte 2

### ContabilidadEventListener
Escucha:
- FacturaCreada → recalcula CostoMercancia o GastosOperativos
- VentaRegistrada → recalcula Ingresos

Publica:
- MetricasCalculadas
- AlertaMargenBajo (si aplica)

### ReportesEventListener
Escucha:
- FacturaCreada → agrega a lista facturas recientes
- VentaRegistrada → agrega a lista ventas recientes
- MetricasCalculadas → actualiza métricas en dashboard
- AlertaMargenBajo → agrega alerta visible

Publica:
- ReporteActualizado

### InventarioEventListener (Preparado, no usado en Parte 2)
Escucha:
- FacturaCreada → (será usado en Parte 3 para crear movimientos)
- VentaRegistrada → (será usado en Parte 3 para reducir stock)

---

## 6. Transaccionalidad Fase 1 - Parte 2

```
FacturaService.crearFactura():
  1. @Transactional: Persiste Factura + Items en BD
  2. Commit
  3. applicationEventPublisher.publishEvent(new FacturaCreada(...))
  
ContabilidadEventListener.onFacturaCreada():
  1. @Transactional
  2. Lee datos de BD
  3. Calcula métricas
  4. Persiste MetricaFinanciera
  5. publishEvent(new MetricasCalculadas(...))
  
Si listener falla:
  - Factura ya está guardada (no se pierde)
  - Métricas no se recalculan hasta retry
  - Frontend muestra factura guardada ✓
  - Reporte muestra datos antiguos temporalmente
```

Implementar retry simple: `@Retryable` en listeners con `maxAttempts=3`.

---

## 7. Almacenamiento de Imágenes - Preparación

Para Fase 2 (captura de QR/imagen), dejar preparado:

```
POST /api/core/v1/facturas/{id}/comprobante
  ├── Recibe multipart/form-data (imagen)
  ├── Guarda en MinIO (o S3)
  ├── Actualiza factura.comprobanteUrl
  └── Publica: ComprobanteAgregado
```

**En Parte 2:** Campo existe pero es opcional (null).

---

## 8. Checklist Fase 1 - Parte 2

- [x] Entidades de Inventario: Producto, Presentación
- [x] Entidades de Facturas: Factura, ItemFactura, Proveedor
- [ ] Entidad de Ventas: Venta (Pendiente — módulo `ventas`)
- [ ] Entidad de Contabilidad: MetricaFinanciera (Pendiente — módulo `accounting`)
- [x] Controllers CRUD básicos (7 recursos: producto+presentación+proveedor+factura)
- [x] Services con lógica de negocio
- [ ] EventListeners: Contabilidad, Reportes (Pendiente — módulo `accounting`)
- [x] Validaciones (producto sin presentaciones no se puede usar en factura)
- [x] Generación automática números factura (`F-PROV-{year}-{sequential:04d}`)
- [x] Transaccionalidad + Retry en listeners
- [x] Tests unitarios básicos (6+7=13 tests, 2 integration suites diseñadas)
- [x] Frontend: Onboarding post-login (selección de industria + router guard)
- [x] Frontend: CRUD Productos, Proveedores, Facturas, Configuración

---
