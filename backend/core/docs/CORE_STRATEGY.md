# Estrategia Arquitectónica: Core Service Event-Driven

---

## 1. Visión General

Core Service es el corazón del SaaS. Maneja operaciones del negocio con arquitectura event-driven, aprovechando Virtual Threads de Java 21 para concurrencia eficiente.

```
Frontend (PWA)
    ↓ JWT
Gateway (8080)
    ↓
Core Service (8082)
├── Módulo: Configuración
├── Módulo: Inventario
├── Módulo: Facturas
├── Módulo: Contabilidad
└── Módulo: Reportes

(todos comunican vía Spring Events - no bloqueantes)
```

---

## 2. Módulos del Core Service

### Módulo: Configuración
**Responsabilidad:** Gestionar setup inicial del tenant

Contiene:
- Plantillas (precargadas)
- Categorías (jerárquicas, 3 niveles)
- Unidades (Kg, Litro, Unidad, etc)
- Ubicaciones (Bodega, Cocina, etc)
- Motivos de movimiento (Entrada, Salida, Ajuste)

Eventos que genera:
- ConfiguracionCargada (cuando tenant se registra)
- CategoriaAgregada
- UbicacionAgregada
- MotivoAgregado

Eventos que escucha:
- TenantCreated (desde Auth) → Carga plantilla

---

### Módulo: Inventario
**Responsabilidad:** Gestionar productos, presentaciones, stock

Contiene:
- Producto (nombre, SKU, imagen, categoría, unidad base)
- Presentación (Caja=24, Six Pack=6, etc)
- Inventario (cantidad por producto+ubicación)
- Movimiento (entrada, salida, ajuste)

Eventos que genera:
- ProductoCreado
- PresentacionCreada
- MovimientoRegistrado
- StockCrítico (alerta)
- AjusteInventario

Eventos que escucha:
- ConfiguracionCargada → Sabe qué categorías/ubicaciones/motivos existen
- FacturaCreada → Reduce stock automáticamente (COGS)
- MovimientoRegistrado → Actualiza inventario

---

### Módulo: Facturas
**Responsabilidad:** Registrar compras a proveedores

Contiene:
- Factura (número, fecha, proveedor, total)
- ItemFactura (producto, cantidad, precio unitario)
- Proveedor

Eventos que genera:
- FacturaCreada
- FacturaPagada
- FacturaAnulada

Eventos que escucha:
- ConfiguracionCargada → Sabe qué proveedores/métodos de pago existen
- MovimientoRegistrado → Para vincular QR/imagen a factura

---

### Módulo: Contabilidad
**Responsabilidad:** Calcular márgenes, ingresos, egresos, rentabilidad

Contiene:
- MetricaFinanciera (diaria, semanal, mensual)
- Margen (bruto %, neto %, operativo %)
- CostoMercancia (suma de facturas)
- GastosOperativos (del Auth Service o local?)
- FlujoCaja

Eventos que genera:
- MetricasCalculadas
- AlertaMargenBajo
- AlertaFlujoCajaNegativo
- ProyeccionGenerada

Eventos que escucha:
- FacturaCreada → Recalcula COGS
- MovimientoRegistrado → Afecta costos
- GastoOperativoRegistrado (desde Facturas) → Recalcula márgenes

---

### Módulo: Reportes
**Responsabilidad:** Consumir datos, generar reportes, dashboards

Contiene:
- ReportInventario (stock por categoría, ubicación)
- ReportContable (márgenes, flujo caja, rentabilidad)
- DashboardResumen (KPIs principales)
- AlertasActivas

Eventos que genera:
- ReporteGenerado

Eventos que escucha:
- ConfiguracionCargada
- ProductoCreado
- MovimientoRegistrado
- MetricasCalculadas
- FacturaCreada

No modifica datos, solo consume y presenta.

---

## 3. Flujo de Eventos Completo

### Escenario 1: Tenant Se Registra

```
1. Auth Service crea Tenant
   └── Publica: TenantCreated (tenant_id, industria)

2. Módulo: Configuración escucha TenantCreated
   └── Busca plantilla por industria
   └── Copia categorías a ConfiguracionTenant
   └── Copia unidades a ConfiguracionTenant
   └── Copia ubicaciones a ConfiguracionTenant
   └── Publica: ConfiguracionCargada (tenant_id, categorias, unidades, ubicaciones)
   
3. Módulo: Reportes escucha ConfiguracionCargada
   └── Inicializa dashboard para tenant
   └── Prepara template de reportes
```

**Timeline:** Auth espera respuesta de onboarding, pero Configuración carga async en background.

---

### Escenario 2: Tenant Registra Factura (Compra)

```
1. Módulo: Facturas recibe POST /facturas
   └── Valida datos
   └── Persiste Factura + Items
   └── Publica: FacturaCreada (factura_id, items[], total)

2. Módulo: Inventario escucha FacturaCreada
   └── Para cada item:
       - Busca Producto
       - Suma cantidad a Inventario
   └── Publica: MovimientoRegistrado (tipo: ENTRADA, producto_id, cantidad, ubicacion)

3. Módulo: Contabilidad escucha FacturaCreada
   └── Suma total a CostoMercancia
   └── Recalcula márgenes
   └── Publica: MetricasCalculadas (nuevos márgenes)

4. Módulo: Reportes escucha MetricasCalculadas
   └── Actualiza dashboard con nuevos márgenes
   └── Publica: ReporteGenerado

5. Módulo: Inventario escucha MovimientoRegistrado
   └── Valida stock no sea crítico
   └── Si es crítico, publica: StockCrítico (producto_id, cantidad_actual, minimo)

6. Módulo: Reportes escucha StockCrítico
   └── Agrega alerta a dashboard
```

**Timeline:** Todo ocurre async. Módulos procesan en paralelo vía virtual threads.

---

### Escenario 3: Registrar Gasto Operativo

¿De dónde viene?

**Opción A:** Facturas con tipo=GASTO_OPERATIVO
```
Módulo: Facturas recibe factura con tipo=GASTO
  └── Publica: GastoOperativoRegistrado
  └── Módulo: Contabilidad lo escucha y suma a GastosOperativos
```

**Opción B:** Módulo separado dentro de Configuración
```
Módulo: Configuración maneja gastos operativos
  └── Publica: GastoOperativoRegistrado
```

¿Cuál?

---

### Escenario 4: Registrar Venta (Venta del Restaurante)

¿Cómo entra esto al sistema?

**Opción A:** Módulo: Ventas (nuevo)
```
Core Service recibe POST /ventas (monto diario)
  └── Módulo: Facturas o Ventas lo persiste
  └── Publica: VentaRegistrada
  └── Módulo: Contabilidad recalcula márgenes
```

**Opción B:** Se calcula desde Movimientos tipo SALIDA
```
Módulo: Inventario suma movimientos SALIDA del día
  └── Contabilidad calcula ingresos teóricos
```

¿Cuál?

---

## 4. Arquitectura Técnica

### Spring Events (Broker)

```
ApplicationEventPublisher (publicador)
  └── Publica eventos de dominio

@EventListener (suscriptores)
  └── Escuchan eventos

Virtual Threads (ejecutores)
  └── Cada listener corre en thread virtual
  └── No bloqueante, eficiente
```

**Por qué Spring Events:**
- Simple, sin dependencias externas
- Suficiente para MVP
- Luego migrar a RabbitMQ sin cambiar código (eventos son agnósticos)

---

### Virtual Threads Strategy

**Dónde se usan:**

1. **Event Listeners:**
   - Automático en Spring: `@EventListener` + `@Async` → Virtual Thread
   - Cada listener es independiente, no bloquea otros

2. **Procesamiento Pesado:**
   - Cálculos de márgenes (loops, agregaciones)
   - Generación de reportes
   - Validaciones complejas
   - Delegados a `ExecutorService` con virtual threads

3. **I/O:**
   - Queries a BD
   - Feign calls a Auth Service
   - Se benefician automáticamente de virtual threads

**Ventaja:** Con Java 21, virtual threads hacen que concurrencia sea trivial. 1000 eventos simultáneos = trivial.

---

### Transaccionalidad

**Problema:** Eventos async pueden fallar, ¿qué pasa?

**Estrategia:**

1. **Publicador es transaccional:**
   ```
   Factura persiste en BD (COMMIT)
   └── Luego publica evento FacturaCreada
   └── Si evento falla, Factura ya está guardada
   ```

2. **Listeners son idempotentes:**
   ```
   Inventario recibe FacturaCreada(factura_id)
   └── Busca factura por ID
   └── Si ya procesó este ID, ignora (idempotencia)
   └── Si falla, puede reintentarse sin duplicar
   ```

3. **Dead Letter Queue (futuro):**
   ```
   Si listener falla, evento no se pierde
   └── Se guarda en table fallidos
   └── Admin puede reintentarlo manualmente
   ```

---

## 5. Estructura de Paquetes

```
backend/pymes-core/
├── src/main/java/com/pymes/core/
│   ├── common/
│   │   ├── config/
│   │   │   ├── EventConfig.java (virtual threads setup)
│   │   │   ├── JwtConfig.java
│   │   │   └── SecurityConfig.java
│   │   ├── dto/
│   │   ├── entity/
│   │   ├── enums/
│   │   └── exception/
│   │
│   ├── configuracion/
│   │   ├── domain/
│   │   │   ├── Plantilla.java
│   │   │   ├── Categoria.java
│   │   │   ├── Unidad.java
│   │   │   ├── Ubicacion.java
│   │   │   └── ConfiguracionTenant.java
│   │   ├── event/
│   │   │   ├── TenantCreatedEvent.java (evento de Auth)
│   │   │   ├── ConfiguracionCargadaEvent.java
│   │   │   └── CategoriaAgregadaEvent.java
│   │   ├── listener/
│   │   │   ├── ConfiguracionEventListener.java
│   │   │   └── TenantCreatedListener.java
│   │   ├── service/
│   │   │   ├── PlantillaService.java
│   │   │   ├── ConfiguracionService.java
│   │   │   └── CategoriaService.java
│   │   ├── repository/
│   │   └── controller/
│   │
│   ├── inventario/
│   │   ├── domain/
│   │   │   ├── Producto.java
│   │   │   ├── Presentacion.java
│   │   │   ├── Inventario.java
│   │   │   └── Movimiento.java
│   │   ├── event/
│   │   │   ├── ProductoCreatedEvent.java
│   │   │   ├── MovimientoRegistradoEvent.java
│   │   │   └── StockCríticoEvent.java
│   │   ├── listener/
│   │   │   ├── InventarioEventListener.java
│   │   │   └── FacturaCreatedListener.java
│   │   ├── service/
│   │   │   ├── ProductoService.java
│   │   │   ├── PresentacionService.java
│   │   │   ├── MovimientoService.java
│   │   │   └── InventarioService.java
│   │   ├── repository/
│   │   └── controller/
│   │
│   ├── facturas/
│   │   ├── domain/
│   │   │   ├── Factura.java
│   │   │   ├── ItemFactura.java
│   │   │   └── Proveedor.java
│   │   ├── event/
│   │   │   ├── FacturaCreatedEvent.java
│   │   │   ├── FacturaPagadaEvent.java
│   │   │   └── GastoOperativoRegistradoEvent.java
│   │   ├── listener/
│   │   │   └── FacturaEventListener.java
│   │   ├── service/
│   │   │   ├── FacturaService.java
│   │   │   ├── ProveedorService.java
│   │   │   └── PagoFacturaService.java
│   │   ├── repository/
│   │   └── controller/
│   │
│   ├── contabilidad/
│   │   ├── domain/
│   │   │   ├── MetricaFinanciera.java
│   │   │   ├── Margen.java
│   │   │   ├── FlujoCaja.java
│   │   │   └── CostoMercancia.java
│   │   ├── event/
│   │   │   ├── FacturaCreatedEvent.java (escucha)
│   │   │   ├── MetricasCalculadasEvent.java
│   │   │   ├── AlertaMargenBajoEvent.java
│   │   │   └── ProyeccionGeneradaEvent.java
│   │   ├── listener/
│   │   │   ├── ContabilidadEventListener.java
│   │   │   └── MovimientoEventListener.java
│   │   ├── service/
│   │   │   ├── MetricasService.java
│   │   │   ├── MargenService.java
│   │   │   ├── CostoService.java
│   │   │   └── ProyeccionService.java
│   │   ├── repository/
│   │   └── controller/
│   │
│   ├── reportes/
│   │   ├── domain/
│   │   │   ├── ReporteInventario.java
│   │   │   ├── ReporteContable.java
│   │   │   └── Dashboard.java
│   │   ├── event/
│   │   │   └── ReporteGeneradoEvent.java
│   │   ├── listener/
│   │   │   ├── ReportesEventListener.java
│   │   │   └── ContabilidadEventListener.java
│   │   ├── service/
│   │   │   ├── ReporteService.java
│   │   │   ├── DashboardService.java
│   │   │   └── AlertasService.java
│   │   ├── repository/
│   │   └── controller/
│   │
│   └── CoreApplication.java
```

---

## 6. Decisiones Técnicas Justificadas

### 1. Spring Events vs RabbitMQ

| Aspecto | Spring Events | RabbitMQ |
|--------|---------------|----------|
| Infraestructura | Embebido en app | Externa |
| MVP | OK | Overkill |
| Persistencia | No | Sí |
| Escalabilidad | Limitada (1 instancia) | Ilimitada |
| Latencia | ms | ms+ |

**Decisión:** Spring Events ahora, RabbitMQ en Fase 3+ cuando tengas múltiples instancias de Core.

---

### 2. Virtual Threads

**Decisión:** Usar `@Async` con virtual threads automáticamente.

```
Spring 3.2+ detecta virtual threads disponibles
└── ApplicationEventPublisher.publishEvent() es non-blocking
└── Listeners procesan en paralelo, sin threads costosos
```

**Impacto:** Concurrencia trivial con recursos mínimos.

---

### 3. Transaccionalidad

**Decisión:** Event Sourcing básico (no complejo).

```
1. Operación persiste en BD (COMMIT)
2. Evento se publica DESPUÉS del COMMIT
3. Listeners procesan evento (pueden fallar sin afectar persistencia)
4. Si listener falla, se reintenta (idempotencia en listener)
```

---

### 4. Ubicación de Gastos Operativos

**Pendiente tu respuesta:** ¿Opción A o B del Escenario 3?

---

### 5. Ubicación de Ventas

**Pendiente tu respuesta:** ¿Opción A o B del Escenario 4?

---

## 7. Flujo de Cumplimiento de Requerimientos

### Márgenes Financieros

```
FacturaCreada (evento)
  ↓
Contabilidad suma a CostoMercancia
  ↓
Contabilidad suma GastosOperativos (de Facturas o módulo)
  ↓
Contabilidad suma Ingresos (de Ventas)
  ↓
Contabilidad calcula:
  - Margen Bruto % = (Ingresos - CostoMercancia) / Ingresos × 100
  - Margen Bruto USD = Ingresos - CostoMercancia
  - Margen Operativo % = (Ingresos - CostoMercancia - GastosOp) / Ingresos × 100
  - Margen Operativo USD = ...
  - Margen Neto % = ...
  - EBITDA
  - Punto Equilibrio
  ↓
Contabilidad publica: MetricasCalculadas
  ↓
Reportes escucha y muestra en dashboard
```

---

### Alertas Informativas

```
MetricasCalculadas
  ↓
Contabilidad valida:
  - Si Margen Neto < 5% → AlertaMargenBajo
  - Si FlujoCaja próx 30 días es negativo → AlertaFlujoCajaNegativo
  - Si Stock < Mínimo → StockCrítico (desde Inventario)
  ↓
Reportes escucha alertas
  ↓
Dashboard muestra alertas activas (lista, colores, acciones sugeridas)
```

---

## 8. Checklist de Decisiones Pendientes

- [ ] Gastos Operativos: ¿Opción A (Facturas) o B (Configuración)?
- [ ] Ventas: ¿Opción A (Módulo Ventas) o B (Cálculo desde Movimientos)?
- [ ] ¿Hay otros eventos que falten?
- [ ] ¿ConfiguracionTenant es una entidad más o parte de Configuración?

---

**¿Esta arquitectura está clara?**
