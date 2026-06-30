# Plantillas Precargadas - Estructura y Configuración

---

## Plantilla Agnóstica (Por Defecto)

Usada cuando el tenant selecciona "Otro" o no elige industria específica.

### Categorías
```
General
└── Sin subcategorías
```

### Unidades
- Unidad
- Caja
- Paquete
- Kg
- Litro

### Ubicaciones
- Principal

### Motivos de Movimiento
- Entrada
- Salida
- Ajuste

### Método de Pago
- Efectivo
- Transferencia

---

## Plantilla: Restaurante

### Categorías (Jerárquica - 3 niveles máx)

```
PERECEDEROS
├── Proteínas
│   ├── Aves (Pollo, Pato, Pavo)
│   ├── Res (Carne molida, Bistec, Costillas)
│   ├── Cerdo (Chuletas, Pernil, Costillitas)
│   └── Mariscos (Pargo, Atún, Camarones)
├── Vegetales y Verduras
│   ├── Hojas (Lechuga, Repollo, Espinaca)
│   ├── Raíces (Zanahoria, Papa, Yuca)
│   └── Otros (Tomate, Cebolla, Ajo)
├── Frutas
│   ├── Tropicales (Plátano, Piña, Melón)
│   └── Cítricas (Limón, Naranja, Toronja)
└── Lácteos
    ├── Quesos
    └── Leche

NO PERECEDEROS
├── Abarrotes
│   ├── Arroz y Pastas
│   ├── Harinas
│   └── Aceites
├── Condimentos y Especias
│   ├── Salsas
│   ├── Condimentos secos
│   └── Especias
├── Enlatados y Conservas
│   ├── Vegetales
│   ├── Carnes
│   └── Bebidas
└── Otros
    ├── Azúcar
    └── Sal

SUMINISTROS
├── Desechables
│   ├── Fiambreras
│   ├── Vasos y Servilletas
│   └── Bolsas para llevar
└── Limpieza
    ├── Detergentes
    ├── Desinfectantes
    └── Esponjas y Brillas
```

### Unidades
- Kg
- Lb
- Gr
- Litro
- Ml
- Unidad
- Caja
- Bolsa
- Paquete

### Ubicaciones
- Bodega Principal
- Cocina
- Freezer 1
- Freezer 2
- Nevera

### Motivos de Movimiento
- Entrada (Compra a proveedor)
- Salida (Venta, Consumo)
- Ajuste (Conteo físico, Rotura, Robo)

### Método de Pago
- Yappy
- ACH
- Efectivo
- Crédito

---

## Plantilla: Bar

### Categorías

```
BEBIDAS ALCOHÓLICAS
├── Cervezas
│   ├── Nacionales
│   └── Importadas
├── Licores Fuertes
│   ├── Ron
│   ├── Vodka
│   ├── Whisky
│   └── Tequila
└── Vinos

BEBIDAS SIN ALCOHOL
├── Refrescos
├── Jugos
├── Energizantes
└── Agua

BEBIDAS PREPARADAS
├── Ingredientes para Cócteles
│   ├── Jarabes
│   ├── Jugos Naturales
│   └── Otros

CONSUMIBLES
├── Hielo
├── Vasos y Copas
├── Servilletas
├── Garnish (Limón, Menta, etc)
└── Hielo Seco

SUMINISTROS
└── Limpieza
    ├── Detergentes
    ├── Desinfectantes
    └── Paños
```

### Unidades
- Botella
- Lata
- Unidad
- Ml
- Litro
- Caja
- Paquete
- Kg (hielo)

### Ubicaciones
- Bodega
- Barra
- Freezer 1
- Freezer 2
- Depósito

### Motivos de Movimiento
- Entrada (Compra a distribuidor)
- Salida (Servicio, Consumo personal)
- Ajuste (Rotura, Conteo físico)

### Método de Pago
- Yappy
- ACH
- Efectivo
- Crédito
- Consignación

---

## Plantilla: Salón de Belleza

### Categorías

```
CUIDADO CAPILAR
├── Tintes y Decolorantes
│   ├── Permanentes
│   ├── Semi-permanentes
│   └── Temporales
├── Shampoos y Acondicionadores
│   ├── Por tipo de cabello
│   └── Especiales
└── Tratamientos Capilares
    ├── Mascarillas
    ├── Aceites
    └── Alisadores

MANICURE Y PEDICURE
├── Esmaltes
│   ├── Colores
│   └── Top Coat/Base
├── Removedores
└── Accesorios
    ├── Limas
    ├── Cortaúñas
    └── Pinceles

MAQUILLAJE
├── Base y Corrector
├── Sombras
├── Labiales
└── Accesorios
    ├── Brochas
    └── Esponjas

HERRAMIENTAS
├── Secadoras
├── Planchas
├── Rizadores
└── Otros

SUMINISTROS
├── Toallas y Capas
├── Algodón y Gasas
├── Guantes
└── Desechables

LIMPIEZA
├── Desinfectantes
├── Desengrasantes
└── Esterilizantes
```

### Unidades
- Unidad
- Ml
- Litro
- Tubo
- Caja
- Kit
- Botella

### Ubicaciones
- Almacén
- Área de Trabajo
- Recepción
- Sala de Espera

### Motivos de Movimiento
- Entrada (Compra a proveedor)
- Salida (Uso en servicio, Venta)
- Ajuste (Expiración, Conteo físico)

### Método de Pago
- Yappy
- ACH
- Efectivo
- Tarjeta

---

## Plantilla: Ferretería

### Categorías

```
HERRAMIENTAS
├── Herramientas Manuales
│   ├── Martillos y Mazos
│   ├── Destornilladores
│   ├── Llaves
│   ├── Sierras
│   └── Otras
└── Herramientas Eléctricas
    ├── Taladros
    ├── Sierras Eléctricas
    └── Otros

TORNILLERÍA
├── Tornillos
├── Tuercas
├── Arandelas
├── Clavos
└── Pernos

MATERIALES DE CONSTRUCCIÓN
├── Cemento y Concreto
├── Arena y Grava
├── Varillas de Acero
├── Bloques y Ladrillos
└── Madera

PINTURA Y ACABADOS
├── Pinturas
│   ├── Interior
│   ├── Exterior
│   └── Especiales
├── Brochas y Rodillos
├── Espátulas
├── Thinner y Diluyentes
└── Masilla

FONTANERÍA
├── Tuberías
├── Codos y Adaptadores
├── Llaves de Paso
└── Selladores

ELECTRICIDAD
├── Cables
├── Breakers
├── Tomas de Corriente
└── Interruptores

OTROS SUMINISTROS
├── Pegamentos y Adhesivos
├── Cintas
└── Seguridad
    ├── Guantes
    ├── Cascos
    └── Lentes
```

### Unidades
- Unidad
- Metro
- Cm
- Kg
- Lb
- Galón
- Caja
- Bolsa
- Paquete

### Ubicaciones
- Depósito
- Tienda
- Mostrador
- Área de Carga

### Motivos de Movimiento
- Entrada (Compra a distribuidor)
- Salida (Venta a cliente)
- Ajuste (Conteo físico, Daño)

### Método de Pago
- Efectivo
- Tarjeta
- Crédito
- Cheque

---

## Plantilla: Mini Super

### Categorías

```
ALIMENTOS FRESCOS
├── Lácteos
│   ├── Leche
│   ├── Quesos
│   └── Yogur
├── Carnes Frías
│   ├── Jamón
│   ├── Salchicha
│   └── Mortadela
└── Frutas y Verduras
    ├── Frutas
    └── Verduras

ABARROTES
├── Arroz y Granos
├── Pasta
├── Aceites
├── Condimentos
└── Harinas

BEBIDAS
├── Refrescos
├── Agua
├── Jugos
├── Cervezas
└── Licores

CONGELADOS
├── Carnes
├── Verduras Congeladas
└── Otros

SNACKS
├── Papitas
├── Galletas
└── Dulces

HIGIENE PERSONAL
├── Jabones
├── Champús
├── Pasta de Dientes
└── Desodorantes

LIMPIEZA
├── Detergentes
├── Desinfectantes
└── Otros

MASCOTAS
├── Alimento
├── Accesorios
└── Higiene

OTROS
└── Productos varios
```

### Unidades
- Unidad
- Caja
- Paquete
- Botella
- Lata
- Kg
- Lb

### Ubicaciones
- Depósito
- Estantería
- Nevera
- Congelador
- Mostrador

### Motivos de Movimiento
- Entrada (Compra a distribuidor)
- Salida (Venta a cliente)
- Ajuste (Vencimiento, Rotura, Conteo)

### Método de Pago
- Efectivo
- Tarjeta
- Cheque

---

## Plantilla: Taller Mecánico

### Categorías

```
PIEZAS DE MOTOR
├── Filtros
│   ├── Filtro de Aire
│   ├── Filtro de Aceite
│   └── Filtro de Gasolina
├── Lubricantes
│   ├── Aceites
│   ├── Grasas
│   └── Refrigerantes
└── Líquidos
    ├── Líquido de Frenos
    ├── Líquido de Dirección
    └── Combustible

SISTEMA DE FRENOS
├── Pastillas de Freno
├── Discos de Freno
├── Cilindros
└── Tuberías

SUSPENSIÓN
├── Amortiguadores
├── Resortes
├── Rotulas
└── Silentblocks

SISTEMA ELÉCTRICO
├── Baterías
├── Alternadores
├── Motores de Arranque
├── Cables
└── Fusibles

HERRAMIENTAS
├── Llaves
├── Destornilladores
├── Extractores
└── Especializada

CONSUMIBLES
├── Paños y Trapos
├── Guantes de Trabajo
├── Selladores
└── Adhesivos

PIEZAS VARIADAS
├── Correas y Mangueras
├── Cojinetes
└── Sellos
```

### Unidades
- Unidad
- Litro
- Ml
- Caja
- Juego
- Kit
- Kg

### Ubicaciones
- Depósito
- Área de Trabajo
- Mostrador
- Banco de Trabajo

### Motivos de Movimiento
- Entrada (Compra a distribuidor)
- Salida (Uso en servicio, Venta)
- Ajuste (Conteo físico, Daño)

### Método de Pago
- Efectivo
- Tarjeta
- Crédito
- ACH

---

## Plantilla: Farmacia

### Categorías

```
MEDICAMENTOS
├── Analgésicos
│   ├── Ibuprofeno
│   ├── Paracetamol
│   └── Aspirina
├── Antibióticos
├── Antiinflamatorios
├── Antigripales
├── Antidiarreicos
├── Laxantes
└── Otros

CUIDADO PERSONAL
├── Higiene Bucal
│   ├── Pasta de Dientes
│   ├── Enjuague
│   └── Cepillos
├── Higiene Corporal
│   ├── Jabones
│   ├── Champús
│   └── Desodorantes
├── Cuidado de la Piel
│   ├── Cremas
│   ├── Lociones
│   └── Protectores Solares
└── Otros

PRODUCTOS PARA BEBÉS
├── Pañales
├── Toallitas Húmedas
├── Champú y Jabón
└── Cremas para Bebé

PRIMEROS AUXILIOS
├── Vendajes
├── Desinfectantes
├── Gasas
├── Apósitos
└── Otros

SUPLEMENTOS Y VITAMINAS
├── Multivitaminas
├── Probióticos
├── Minerales
└── Otros

EQUIPOS Y DISPOSITIVOS
├── Termómetros
├── Glucómetros
├── Tensiómetros
└── Otros

HIGIENE DEL HOGAR
├── Desinfectantes
└── Repelentes
```

### Unidades
- Unidad
- Caja
- Blíster
- Frasco
- Ml
- Gr
- Botella

### Ubicaciones
- Almacén
- Mostrador
- Vitrina (productos controlados)
- Refrigeración (si aplica)

### Motivos de Movimiento
- Entrada (Compra a distribuidor)
- Salida (Venta a cliente)
- Ajuste (Vencimiento, Conteo físico)

### Método de Pago
- Efectivo
- Tarjeta
- Crédito
- Seguro médico

---

## Notas Importantes para Implementación

### Sobre Categorías

- Máximo 3 niveles de jerarquía
- Las subcategorías pueden modificarse
- Los tenant pueden agregar categorías personalizadas
- Las categorías iniciales pueden renombrarse

### Sobre Unidades

- Vienen precargadas pero el tenant puede agregar más
- Al crear Producto, debe seleccionar una unidad base
- Las Presentaciones usan las mismas unidades

### Sobre Ubicaciones

- Necesarias para rastrear inventario (Parte 3)
- Pueden editarse, agregarse, eliminarse
- Cada movimiento debe indicar ubicación

### Sobre Motivos

- Fijos para MVP (Entrada, Salida, Ajuste)
- Posibilidad de agregar más en futuro (Fase 3+)
- Importante para reportes y análisis

### Sobre Métodos de Pago

- Varían según industria
- Pueden modificarse según necesidad del tenant
- Usados en Facturas para rastreo

---

## Plantillas de Productos (Implementado)

### Problema
Las plantillas actuales solo tienen categorías, unidades y ubicaciones. Al completar onboarding, el usuario tiene una estructura vacía y debe crear productos uno por uno.

### Objetivo
Al completar onboarding → se copian productos genéricos por categoría → al facturar ya hay catálogo cargado con SKU y unidad.

### Alcance
- Productos genéricos (sin marca), ~20-25 por industria (~160 total)
- 1-2 presentaciones por producto (~280 presentaciones total)
- SKU auto-generado al copiar al tenant: `P-0001`, `P-0002`... (secuencial por tenant)

### Implementación: Tablas + Seed (vía SeedDataRunner DDL)

Las tablas se crean via DDL en `SeedDataRunner.createTables()` (mismo patrón que `template_units`), **no** con Flyway:

```sql
template_products (
    id UUID PRIMARY KEY,
    industry_code VARCHAR(50) NOT NULL,
    category_id UUID,
    name VARCHAR(150) NOT NULL,
    base_unit VARCHAR(50) NOT NULL,
    sort_order INTEGER DEFAULT 0
);

template_product_presentations (
    id UUID PRIMARY KEY,
    template_product_id UUID NOT NULL,
    name VARCHAR(100) NOT NULL,
    conversion INTEGER DEFAULT 1,
    sort_order INTEGER DEFAULT 0
);
```

- Sin FK a industries ni template_categories (datos readonly, orphan aceptable)
- Sin columna `sku` — se genera al copiar al tenant
- Sin FK entre presentaciones y products (misma razón)
- Índices: `idx_tp_industry` en industry_code, `idx_tpp_product` en template_product_id
- Creadas con `CREATE TABLE IF NOT EXISTS` en SeedDataRunner

Cada industria tiene su método `seed<Industria>()` que inserta productos como batch de arrays con el helper `addProd()`:

```java
private Object[] addProd(String name, UUID catId, String unit, Object[]... pres) {
    return new Object[]{UUID.randomUUID(), industryCode, catId, name, unit, 0};
}
// pres se pasa como Object[][]{name, conversion, sort}
```

Ejemplo para restaurante:
```
Bebidas > Cervezas:           Cerveza (Unidad) → Caja x12
Bebidas > Refrescos:          Refresco (Botella 2L) → Unidad
Bebidas > Jugos:              Jugo natural (Litro) → Unidad
Abarrotes > Arroz:            Arroz (Kg) → Bolsa 5lb
Abarrotes > Fideos:           Fideos (Paquete 500g) → Unidad
Aceites:                      Aceite vegetal (Litro) → Garrafa 5L
Condimentos:                  Sal (Kg), Azúcar (Kg)
Limpieza:                     Detergente (Litro) → Galón
```

**~20-25 productos por industria**, 1-2 presentaciones cada uno. Todas las industrias menos `default` (2 productos).

### Implementación: Onboarding copia productos

En `SetupServiceImpl.completeOnboarding()` (transaccional):

1. Carga `template_products` + `template_product_presentations` por `industry_code` vía JdbcTemplate
2. Para cada template_product → batch insert en `core.products` con SKU auto-generado `P-%04d` secuencial
3. Mapa: `template_product_id → producto_id` para vincular presentaciones
4. Para cada template_product_presentations → batch insert en `core.product_presentations`
5. SKU arranca en `P-0001` cada vez (tenant nuevo = sin productos previos)

**SetupResponse** — campo `products`:
```json
{
  "products": [
    { "name": "Cerveza", "baseUnit": "Unidad", "categoryName": "Cervezas" },
    ...
  ]
}
```
`ProductTemplateDTO` sin SKU (aún no se ha generado — solo visible tras onboarding completo).

**loadIndustryData()** — query SQL con JOIN a template_categories para obtener `categoryName`.

### Fase 3: Frontend

**OnboardingPage.vue** — step 2: sección "Productos precargados (N)" con tabla de nombre, unidad y categoría.

**SetupInfo type** — campo `products: ProductTemplateDTO[]`.

### Escala real
- ~160 productos × 8 industrias (= ~160 inserts en seed)
- ~280 presentaciones (= ~280 inserts)
- ~440 filas total en SeedDataRunner (menos que los ~1000-1600 estimados)

---

## Estructura de Datos en BD (Actual)

Cada plantilla se almacena en tablas normalizadas con FK a `industries(code)`:

### Tablas (schema `core`)

```
industries                          — Creada por Flyway V2
├── code: VARCHAR(50) PK            — "restaurante", "bares", etc.
└── name: VARCHAR(100)

template_categories                 — Creada por Flyway V2
├── id: UUID PK
├── industry_code: VARCHAR(50) FK → industries
├── name: VARCHAR(100)
├── parent_id: UUID FK → template_categories (auto-ref, 3 niveles)
└── sort_order: INTEGER

template_locations                  — Creada por Flyway V2
├── id: UUID PK
├── industry_code: VARCHAR(50) FK → industries
├── name: VARCHAR(100)
└── sort_order: INTEGER

template_units                      — Creada por SeedDataRunner (DDL)
├── id: UUID PK
├── industry_code: VARCHAR(50) FK → industries (INDEX)
├── name: VARCHAR(100)
└── sort_order: INTEGER

template_movement_reasons           — Creada por SeedDataRunner (DDL)
├── id: UUID PK
├── industry_code: VARCHAR(50) FK → industries (INDEX)
├── name: VARCHAR(100)
├── movement_type: VARCHAR(20)      — "ENTRADA", "SALIDA", "AJUSTE"
└── sort_order: INTEGER

template_payment_methods            — Creada por SeedDataRunner (DDL)
├── id: UUID PK
├── industry_code: VARCHAR(50) FK → industries (INDEX)
├── name: VARCHAR(100)
└── sort_order: INTEGER

template_products                   — Creada por SeedDataRunner (DDL, sin FK)
├── id: UUID PK
├── industry_code: VARCHAR(50)       — INDEX, sin FK (datos readonly)
├── category_id: UUID                — sin FK (datos readonly)
├── name: VARCHAR(150)
├── base_unit: VARCHAR(50)           — sin SKU (se genera al copiar al tenant)
└── sort_order: INTEGER

template_product_presentations      — Creada por SeedDataRunner (DDL, sin FK)
├── id: UUID PK
├── template_product_id: UUID NOT NULL  — INDEX, sin FK (datos readonly)
├── name: VARCHAR(100)
├── conversion: INTEGER DEFAULT 1
└── sort_order: INTEGER
```

### Seed Data

El seed se ejecuta vía `SeedDataRunner` (ApplicationRunner) al startup:
1. `createTables()` — DDL con `CREATE TABLE IF NOT EXISTS` + índices (incluye `template_products` y `template_product_presentations`)
2. `seedIndustries()` — inserta 8 industrias
3. `seed{Nombre}()` — por industria inserta categorías, ubicaciones, unidades, motivos, pagos, **productos** y **presentaciones** (todo en un mismo método)
4. Es idempotente: verifica `SELECT COUNT(*) FROM industries` antes de insertar

### Nota sobre migración futura

Si en producción se necesita modificar las tablas creadas por SeedDataRunner,
agregar una migration Flyway con `ALTER TABLE`. SeedDataRunner solo ejecuta
`CREATE TABLE IF NOT EXISTS`, no altera tablas existentes.

---