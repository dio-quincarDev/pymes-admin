# Estrategia: PostgreSQL Compartido con Schemas Separados

> Detalle de implementación del schema `core`: [`backend/core/docs/CORE.md`](/backend/core/docs/CORE.md)

## Resumen Ejecutivo

Usar 1 instancia PostgreSQL con 2 schemas separados (auth y core) en lugar de 2 instancias. Esto optimiza recursos en infraestructura limitada (OCI Free Tier) sin comprometer seguridad ni escalabilidad.

---

## Problema

- Infraestructura limitada: 1 VMs con 1 CPU + 6GB RAM c/u
- Consumo actual: 1 PostgreSQL para Auth
- Necesidad: PostgreSQL para Core Service
- Solución ingenua: 2 PostgreSQL = doble consumo RAM/CPU = colapso

---

## Solución: 1 PostgreSQL + 2 Schemas

### Estructura
```
pymes-postgres-auth (instancia única)
├── Database: pymes_db
│   ├── Schema: auth (datos autenticación, usuarios, tenants)
│   └── Schema: core (datos operativos, facturas, gastos, ventas)
```

---

## Por Qué Esta Estrategia

### 1. Optimización de Recursos
- Sin cambio: 1 proceso PostgreSQL (512MB-1GB RAM)
- Con 2 instancias: 2 procesos PostgreSQL (1GB-2GB RAM) = inaceptable
- Impacto: Ahorra ~500MB-700MB RAM críticos en VM limitada

### 2. Aislamiento de Datos
- Schemas de PostgreSQL = namespace completamente separado
- Auth Service nunca accede a schema core
- Core Service nunca accede a schema auth
- Mismo nivel de seguridad que 2 bases de datos separadas

### 3. Backups y Recuperación
- Ventaja: 1 dump/restore para toda la aplicación
- Facilidad: Backup centralizado de ambos servicios
- Coherencia: Snapshots consistentes en tiempo

### 4. Escalabilidad Futura
- Agregar Service 3, 4, 5... sin agregar más PostgreSQL
- Patrón: 1 DB = N schemas
- Cuando la data crezca, migrar a RDS/Cloud SQL sin cambiar arquitectura

### 5. Costo OCI Always Free
- PostgreSQL siempre free (hasta 20GB storage)
- No hay costo adicional por schema
- Vs. 2 instancias = salir del free tier

---

## Implementación

### A. PostgreSQL (Una sola instancia)

Crear database única:
```sql
CREATE DATABASE pymes_db;
```

Schemas automáticamente creados por Flyway:
- Auth Service: Flyway crea schema:auth (V1__initial_schema.sql)
- Core Service: Flyway crea schema:core (V1__initial_schema.sql)
- Cada uno con sus tablas, índices, constraints

### B. Auth Service

application-dev.yaml:
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/pymes_db  # DB única
  flyway:
    schemas: auth  # Flyway target schema
```

Resultado: Migraciones se aplican solo en schema auth

### C. Core Service (Nuevo)

application-dev.yaml:
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/pymes_db  # MISMA DB
  flyway:
    schemas: core  # Flyway target schema DIFERENTE
```

Resultado: Migraciones se aplican solo en schema core

### D. Docker Compose

Antes:
```yaml
postgres-auth:
  POSTGRES_DB: pymes_auth  # Una DB por servicio
  
postgres-core:  # Segunda instancia (eliminada)
  POSTGRES_DB: pymes_core
```

Después:
```yaml
postgres:  # Servicio único
  POSTGRES_DB: pymes_db  # Una DB para todo
  
# postgres-core: ELIMINADO
```

Ambos servicios conectan a la misma instancia:
```yaml
auth-service:
  DB_HOST: pymes-postgres-db
  DB_NAME: pymes_db
  
core-service:
  DB_HOST: pymes-postgres-db  # MISMO HOST
  DB_NAME: pymes_db           # MISMA DB
  DB_SCHEMA: core             # SCHEMA diferente
```

### E. Gateway Routes

application.yaml:
```yaml
routes:
  - id: core-service
    uri: http://pymes-core-service:8082
    predicates:
      - Path=/api/v1/core/**
```

Permite descubrimiento automático de Core Service vía Gateway.

---

## Seguridad

### ¿Son los datos realmente separados?

SÍ. Completamente:

1. Nivel PostgreSQL: Schemas son namespaces independientes
   - SELECT * FROM auth.users ≠ SELECT * FROM core.facturas
   - Cada schema tiene sus propios tables, indexes, constraints

2. Nivel Aplicación: Cada servicio especifica spring.flyway.schemas
   - Auth Flyway: Solo toca schema auth
   - Core Flyway: Solo toca schema core
   - Imposible sobrescribir tablas del otro

3. Nivel ORM (Hibernate): Cada servicio tiene su Entity mapping
   - Auth: @Table(name = "users") → auth.users
   - Core: @Table(name = "facturas") → core.facturas
   - Cero riesgo de colisión

---

## Comparativa

| Aspecto | 2 Instancias PostgreSQL | 1 PostgreSQL + 2 Schemas |
|--------|----------------------|------------------------|
| RAM | 1GB-2GB | 512MB-1GB |
| CPU | 2 procesos | 1 proceso |
| Aislamiento datos | Máximo | Máximo (igual) |
| Backups | 2 archivos | 1 archivo |
| Mantenimiento | Duplicado | Simple |
| Costo OCI | Fuera free tier | Free tier OK |
| Escalabilidad | Pesada | Fácil (agregar schemas) |

---

## Flujo de Datos

```
Frontend (PWA)
    |
    Gateway (8080)
    |-- /api/v1/auth/** → Auth Service (8081) → pymes_db.auth
    |-- /api/v1/core/** → Core Service (8082) → pymes_db.core
    
pymes-postgres-auth (una sola instancia)
├── Schema: auth
│   └── tables: users, tenants, refresh_tokens, etc
├── Schema: core
│   └── tables: proveedores, facturas, gastos, ventas, etc
```

---

## Ventajas a Futuro

1. Agregar Service 3: Solo crear schema service3 + aplicación
2. Migración a Cloud: Cambiar DB_HOST a RDS/CloudSQL, schemas se mantienen
3. Sharding (Escala masiva): Dividir schemas en múltiples DBs sin cambiar código
4. Multi-tenant avanzado: Migrar de columna discriminadora a schema por tenant si escala

---

## Limitaciones

| Limitación | Impacto | Cuándo Migrar |
|-----------|--------|---------------|
| Max 1000 schemas por DB (PG limit) | Bajo (N servicios < 1000) | Cuando tengas 500+ microservicios |
| Backups + restore no selectivos | Bajo (backupeas todo) | Cuando necesites BCP granular |
| Monitoreo centralizado | Bajo (1 instancia) | Cuando necesites métricas por servicio |

---

## Checklist de Implementacion

- [x] Cambiar Docker Compose: pymes_auth → pymes_db
- [x] Eliminar servicio postgres-core
- [x] Auth application.yml: spring.flyway.schemas: auth
- [x] Core application.yml: spring.flyway.schemas: core
- [x] Core Flyway V1: Crear schema core al inicio
- [x] Gateway routes: Agregar /api/core/**
- [x] Variables env: DB_NAME, DB_USERNAME, DB_PASSWORD
- [x] Test: Conectar ambos servicios a la misma instancia
- [x] Validar: ambos schemas en una sola DB

---

## Conclusión

Esta estrategia maximiza eficiencia con recursos limitados sin sacrificar seguridad, escalabilidad o mantenibilidad. Es el patrón estándar en startups/MVPs con constraints de infraestructura.

Cuándo cambiar: Solo cuando la data o el número de servicios crezcan exponencialmente (Fase production con 50+ microservicios).
