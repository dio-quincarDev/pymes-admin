# PyMes Admin - Auth Microservice 🔐

> **Spring Boot OAuth2** - Servicio de autenticación, gestión de usuarios y multi-tenancy para la plataforma PyMes Admin

---

## 📋 Descripción

Este microservicio es el **centro de identidad** de la arquitectura, responsable de:

- **Autenticación OAuth2** con Google y Facebook
- **Gestión de usuarios** y su pertenencia a múltiples tenants (empresas)
- **Emisión y validación de JWT** (Access Token + Refresh Token)
- **Multi-tenancy** con aislamiento de datos por empresa
- **Sistema de roles** (OWNER, ADMIN, CONTABLE, VIEWER)
- **Invitaciones** a tenants
- **Auditoría** de todas las acciones

### Estado Actual

| Componente | Estado | Descripción |
|------------|--------|-------------|
| **OAuth2 (Google/FB)** | ✅ Implementado | Login con proveedores externos |
| **Base de Datos** | ✅ Schema listo | Flyway migration V1 completada |
| **Entities** | ✅ Completas | 6 entidades principales |
| **Security Config** | ✅ Configurado | Spring Security + OAuth2 |
| **JWT Provider** | 📝 Pendiente | Generación y validación de tokens |
| **Controllers** | 📝 Pendiente | Endpoints REST |
| **Servicios** | 📝 Pendiente | Lógica de negocio |

---

## 🏗️ Arquitectura

### Stack Tecnológico

| Componente | Tecnología | Versión |
|------------|------------|---------|
| **Framework** | Spring Boot | 3.4.3 |
| **Java** | Eclipse Temurin | 21 |
| **Database** | PostgreSQL | 15+ |
| **Cache** | Redis | 7+ |
| **ORM** | Hibernate/JPA | - |
| **Migrations** | Flyway | - |
| **JWT** | JJWT (io.jsonwebtoken) | 0.12.6 |
| **OAuth2** | Spring Security OAuth2 | - |
| **API Docs** | OpenAPI 3 (Springdoc) | 2.8.5 |
| **Build Tool** | Maven | 3.9 |

### Dependencias Clave

```xml
<!-- OAuth2 Client + Resource Server -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-oauth2-client</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-oauth2-resource-server</artifactId>
</dependency>

<!-- Database -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
</dependency>
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
</dependency>

<!-- JWT -->
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.12.6</version>
</dependency>

<!-- Redis -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
```

---

## 🗄️ Modelo de Datos

### Entidades Principales

```
┌─────────────────────────────────────────────────────────────┐
│  users (globales)                                           │
│  - id, email, name, provider, provider_id, picture_url      │
│  - Un usuario puede pertenecer a MÚLTIPLES tenants          │
└─────────────────────────────────────────────────────────────┘
                            │
                            │ 1:N
                            ▼
┌─────────────────────────────────────────────────────────────┐
│  user_tenants (relación)                                    │
│  - user_id, tenant_id, role, accepted_at                    │
│  - Rol: OWNER, ADMIN, CONTABLE, VIEWER                      │
└─────────────────────────────────────────────────────────────┘
                            │
                            │ N:1
                            ▼
┌─────────────────────────────────────────────────────────────┐
│  tenants (empresas)                                         │
│  - id, name, slug, plan, max_users, industry                │
│  - Planes: free, starter, pro, enterprise                   │
└─────────────────────────────────────────────────────────────┘
```

### Tablas de la Base de Datos

| Tabla | Propósito |
|-------|-----------|
| `users` | Usuarios globales (pueden estar en múltiples tenants) |
| `tenants` | Empresas/organizaciones |
| `user_tenants` | Relación usuario-tenant con rol |
| `invitations` | Invitaciones pendientes a tenants |
| `refresh_tokens` | Tokens JWT revocables (logout) |
| `audit_log` | Auditoría de todas las acciones |

### Schema SQL (Flyway V1)

El schema inicial está en `src/main/resources/db/migration/V1__initial_schema.sql`

---

## 🔐 Autenticación y Autorización

### Flujo OAuth2

```
┌─────────────────────────────────────────────────────────────┐
│  1. Usuario → Frontend → "Login con Google"                 │
└─────────────────────┬───────────────────────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────────────────────┐
│  2. Redirige a Google → Usuario autentica                   │
└─────────────────────┬───────────────────────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────────────────────┐
│  3. Callback → /api/v1/login/oauth2/code/google             │
│     - CustomOAuth2UserService busca/crea usuario en DB      │
│     - Busca user_tenants (¿a qué tenants pertenece?)        │
└─────────────────────┬───────────────────────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────────────────────┐
│  4a. ¿Multi-tenant? → Devuelve lista de tenants             │
│      Frontend muestra selector                              │
│                                                             │
│  4b. ¿Single tenant? → Genera JWT directo                   │
│      JWT: {user_id, tenant_id, role, plan, permissions}     │
└──────────────────────────────────────────────────────────────┘
```

### JWT Token Structure

```json
{
  "sub": "user-uuid",
  "email": "usuario@empresa.com",
  "tenant_id": "tenant-uuid",
  "tenant_name": "Restaurante XYZ",
  "role": "OWNER",
  "plan": "pro",
  "permissions": ["*:*"],
  "iat": 1679000000,
  "exp": 1679000900
}
```

### Configuración de Tokens

| Token | Duración | Almacenamiento |
|-------|----------|----------------|
| **Access Token** | 15 minutos | localStorage (cliente) |
| **Refresh Token** | 7 días | DB (revocable) |

---

## 👥 Roles y Planes

### Roles Disponibles

| Rol | Descripción | Permisos Típicos |
|-----|-------------|------------------|
| `OWNER` | Creador del tenant, acceso total | `*:*` |
| `ADMIN` | Gestiona usuarios y operaciones | `users:*`, `gastos:*`, `ingresos:*` |
| `CONTABLE` | Solo área contable/fiscal | `contabilidad:*`, `reportes:*` |
| `VIEWER` | Solo lectura | `*:read` |

### Planes del SaaS

| Plan | Precio | Usuarios Máx. | Roles Disponibles |
|------|--------|---------------|-------------------|
| **FREE** | $0/mes | 1 | OWNER |
| **STARTER** | $9/mes | 3 | OWNER, ADMIN |
| **PRO** | $29/mes | Ilimitados | Todos |
| **ENTERPRISE** | $99/mes | Ilimitados | Todos + custom |

---

## 🌐 Endpoints

### Públicos (sin autenticación)

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| `GET` | `/oauth2/authorization/google` | Iniciar login con Google |
| `GET` | `/oauth2/authorization/facebook` | Iniciar login con Facebook |
| `GET` | `/login/oauth2/code/google` | Callback de Google OAuth2 |
| `GET` | `/login/oauth2/code/facebook` | Callback de Facebook OAuth2 |
| `GET` | `/swagger-ui.html` | Documentación API |

### Privados (requieren JWT) - 📝 Pendientes de implementar

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| `GET` | `/auth/user` | Datos del usuario actual |
| `GET` | `/auth/tenants` | Lista de tenants del usuario |
| `POST` | `/auth/tenants/{id}/select` | Cambiar tenant activo |
| `POST` | `/auth/logout` | Logout (invalida refresh token) |
| `POST` | `/auth/refresh` | Refresh access token |
| `GET` | `/auth/invitations` | Invitaciones pendientes |
| `POST` | `/auth/invitations` | Crear invitación |
| `DELETE` | `/auth/invitations/{id}` | Cancelar invitación |

---

## ⚙️ Variables de Entorno

| Variable | Valor por Defecto | Descripción |
|----------|-------------------|-------------|
| `SERVER_PORT` | `8081` | Puerto del servicio |
| `DB_HOST` | `localhost` | Host de PostgreSQL |
| `DB_PORT` | `5432` | Puerto de PostgreSQL |
| `DB_NAME` | `pymes_auth` | Nombre de la base de datos |
| `DB_USERNAME` | `postgres` | Usuario de la DB |
| `DB_PASSWORD` | `postgres` | Contraseña de la DB |
| `REDIS_HOST` | `localhost` | Host de Redis |
| `REDIS_PORT` | `6379` | Puerto de Redis |
| `GOOGLE_CLIENT_ID` | - | Client ID de Google OAuth2 |
| `GOOGLE_CLIENT_SECRET` | - | Client Secret de Google OAuth2 |
| `FACEBOOK_CLIENT_ID` | - | App ID de Facebook OAuth2 |
| `FACEBOOK_CLIENT_SECRET` | - | App Secret de Facebook OAuth2 |
| `JWT_SECRET` | - | Clave secreta (mínimo 256 bits) |
| `JWT_ACCESS_EXPIRATION` | `3600000` | Duración del access token (ms) |
| `JWT_REFRESH_EXPIRATION` | `86400000` | Duración del refresh token (ms) |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173` | Orígenes permitidos |

### Ejemplo `.env`

```env
# Database
DB_HOST=pymes-postgres-auth
DB_PORT=5432
DB_NAME=pymes_auth
DB_USERNAME=postgres
DB_PASSWORD=secure-password

# Redis
REDIS_HOST=pymes-redis-auth
REDIS_PORT=6379

# OAuth2 - Google
GOOGLE_CLIENT_ID=xxx.apps.googleusercontent.com
GOOGLE_CLIENT_SECRET=xxx

# OAuth2 - Facebook
FACEBOOK_CLIENT_ID=xxx
FACEBOOK_CLIENT_SECRET=xxx

# JWT
JWT_SECRET=minimum-256-bits-secret-key-change-in-production
JWT_ACCESS_EXPIRATION=900000
JWT_REFRESH_EXPIRATION=604800000

# Server
SERVER_PORT=8081

# CORS
CORS_ALLOWED_ORIGINS=http://localhost:5173,https://app.pymes.com
```

---

## 🐳 Ejecución con Docker

### Build de la Imagen

```bash
# Desde la raíz del proyecto
docker build -t pymes-auth:latest ./backend/auth
```

### Ejecutar con Docker Compose

```bash
# Levantar toda la infraestructura (Auth + PostgreSQL + Redis + Gateway)
docker compose up -d
```

### Ver Logs

```bash
docker logs -f pymes-auth-service
```

### Health Check

```bash
curl http://localhost:8081/api/v1/actuator/health
```

---

## 💻 Ejecución Local (Desarrollo)

### Requisitos Previos

- Java 21+ instalado
- Maven 3.9+
- PostgreSQL 15+ corriendo
- Redis 7+ corriendo

### Pasos

```bash
cd backend/auth

# Copiar archivo de entorno
cp .env.example .env

# Editar .env con tus credenciales reales

# Compilar
./mvnw clean package -DskipTests

# Ejecutar
./mvnw spring-boot:run
```

### Acceder a Swagger UI

```
http://localhost:8081/api/v1/swagger-ui.html
```

---

## 🗂️ Estructura del Proyecto

```
auth/
├── .mvn/                      # Maven wrapper
├── src/
│   ├── main/
│   │   ├── java/auth/pymes/
│   │   │   ├── AuthApplication.java
│   │   │   ├── common/
│   │   │   │   ├── config/
│   │   │   │   │   └── SecurityConfig.java
│   │   │   │   ├── constants/
│   │   │   │   │   └── ApiPathConstants.java
│   │   │   │   └── models/
│   │   │   │       ├── dto/
│   │   │   │       │   ├── request/
│   │   │   │       │   └── response/
│   │   │   │       ├── entities/
│   │   │   │       │   ├── UserEntity.java
│   │   │   │       │   ├── Tenant.java
│   │   │   │       │   ├── UserTenant.java
│   │   │   │       │   ├── Invitation.java
│   │   │   │       │   ├── RefreshToken.java
│   │   │   │       │   └── AuditLog.java
│   │   │   │       ├── enums/
│   │   │   │       └── mappers/
│   │   │   ├── controller/
│   │   │   ├── repositories/
│   │   │   │   └── UserEntityRepository.java
│   │   │   ├── service/
│   │   │   │   └── impl/
│   │   │   │       └── CustomOAuth2UserService.java
│   │   │   └── utils/
│   │   └── resources/
│   │       ├── application.yaml
│   │       └── db/migration/
│   │           └── V1__initial_schema.sql
│   └── test/
│       ├── java/auth/pymes/
│       │   └── AuthApplicationTests.java
│       └── resources/
│           └── application-test.yaml
├── .env.example               # Ejemplo de variables de entorno
├── Dockerfile                 # Multi-stage build
├── pom.xml                    # Dependencias Maven
└── README.md
```

---

## 🧪 Testing

### Tests Unitarios

```bash
./mvnw test
```

### Health Endpoints

| Endpoint | Descripción |
|----------|-------------|
| `GET /api/v1/actuator/health` | Estado de salud del servicio |
| `GET /api/v1/actuator/info` | Información de la aplicación |

---

## 🔌 Redis - Uso de Caché

| Key Pattern | Propósito | TTL |
|-------------|-----------|-----|
| `token_blacklist:{jti}` | Tokens revocados (logout) | 15 min |
| `permissions:{user_id}:{tenant_id}` | Cache de permisos | 5 min |
| `rate:login:{ip}` | Rate limiting login | 1 min |
| `invitation:{token}` | Invitaciones pendientes | 7 días |
| `tenants:{user_id}` | Lista de tenants del usuario | 1 hora |

---

## 🔒 Seguridad

| Aspecto | Implementación |
|---------|----------------|
| **Autenticación** | OAuth2 (Google/Facebook) |
| **JWT Secret** | Mínimo 256 bits, variable de entorno |
| **HTTPS** | Obligatorio en producción |
| **CORS** | Configurado en `application.yaml` |
| **Rate Limiting** | 📝 Pendiente (Redis) |
| **Audit Log** | Todas las acciones se registran en DB |
| **Token Blacklist** | 📝 Pendiente (Redis) |

---

## 📊 Roadmap de Implementación

### ✅ Completado

- [x] Configuración de proyecto (pom.xml)
- [x] application.yaml
- [x] Flyway migration (V1)
- [x] Entities (6 tablas)
- [x] SecurityConfig
- [x] CustomOAuth2UserService

### 📝 Pendiente

- [ ] JWT Utility (JwtTokenProvider)
- [ ] Controllers (AuthController, TenantController)
- [ ] Services (AuthService, InvitationService)
- [ ] DTOs (request/response)
- [ ] Mappers (MapStruct)
- [ ] Enums (RoleName, PlanName, Permission)
- [ ] Repositories adicionales
- [ ] Redis integration
- [ ] Integration tests

---

## 🔧 Troubleshooting

### Error: Flyway migration falla

```bash
# Verificar que la DB esté vacía o hacer clean
docker exec -it pymes-postgres-auth psql -U postgres -c "DROP SCHEMA public CASCADE; CREATE SCHEMA public;"
```

### OAuth2 no funciona - Error 401

Verificar que las credenciales de Google/Facebook estén correctas en el `.env`:
```bash
docker logs pymes-auth-service | grep -i oauth2
```

### Redis no conecta

```bash
# Verificar conectividad desde el contenedor
docker exec -it pymes-auth-service redis-cli -h pymes-redis-auth ping
```

---

## 🔗 Enlaces Relacionados

- [Documentación Principal del Proyecto](../../README.md)
- [API Gateway](../gateway-pymes/README.md)
- [Guía de Quick Start](../../.github/QUICK_START.md)
- [Spring Security OAuth2](https://spring.io/guides/tutorials/spring-boot-oauth2/)
- [Flyway Documentation](https://flywaydb.org/documentation/)

---

<div align="center">

**PyMes Admin - Auth Microservice** | Parte de la arquitectura SaaS para PYMEs

[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.4.3-6db33f.svg)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-21-007396.svg)](https://openjdk.java.net/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-336791.svg)](https://www.postgresql.org/)

</div>
