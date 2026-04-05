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

### Estado Actual (Production Ready) 🚀

| Componente | Estado | Descripción |
|------------|--------|-------------|
| **OAuth2 Core** | ✅ Listo | Google/FB configurados con `SuccessHandler` propio. |
| **Auth Local** | ✅ Listo | Registro atómico (User + Tenant FREE + OWNER), Login email/password con BCrypt. |
| **Seguridad JWT** | ✅ Listo | `JwtService` con claims Multi-tenant: `userId`, `tenantId`, `role`, `plan`. |
| **Filtro de Auth** | ✅ Listo | `JwtAuthenticationFilter` activo. |
| **Base de Datos** | ✅ Listo | Flyway V1-V3, esquema PostgreSQL/H2 (Test) alineados. |
| **Modelo de Datos** | ✅ Listo | Soft delete con `deleted_at` (forense) + `is_active` en User/Tenant/UserTenant. |
| **DTOs & Mappers** | ✅ Listo | Java Records inmutables. |
| **Exception Handling** | ✅ Listo | 8+ excepciones + GlobalExceptionHandler. |
| **Auth Service** | ✅ Listo | Login, register, multi-tenant, invitaciones, tokens. |
| **CRUD Usuarios** | ✅ Listo | Listar miembros, cambiar rol (jerarquía), desvincular (soft delete). |
| **Seguridad API** | ✅ Listo | `@PreAuthorize` (OWNER/ADMIN) + validación jerarquía en service layer. |
| **Rate Limiting** | ✅ Listo | Redis-based, 5 intentos/15min por IP en `/login`. |
| **IA Ready** | ✅ Listo | Auditoría con IP + User-Agent en login/register (`audit_log`). |
| **Unit Testing** | ✅ Listo | 31 tests (AuthServiceImpl) — cobertura OAuth2 + local. |
| **Redis Integration** | ✅ Listo | Blacklist de tokens + Rate Limiting. |

---

### 🧪 Estrategia de Testing

Hemos implementado una pirámide de pruebas robusta y desacoplada del entorno:

1.  **Tests Unitarios (JUnit 5 + Mockito):** 
    - Pruebas puras de la lógica de negocio en `AuthServiceImpl`.
    - Aislamiento total mediante Mocks de Repositorios y Servicios JWT.
    - Verificación de records de Java con aserciones fluidas (AssertJ).
2.  **Context Testing (SpringBootTest + H2):**
    - Verificación del levantamiento del contexto de Spring.
    - Base de datos en memoria (H2) configurada con dialecto PostgreSQL.
    - **Zero External Dependency:** El entorno de test usa valores mock para OAuth2 (Google/FB) en `application-test.yaml`, eliminando la necesidad de archivos `.env`.

---

## 🏗️ Arquitectura

### Stack Tecnológico

| Componente | Tecnología | Versión |
|------------|------------|---------|
| **Framework** | Spring Boot | 3.4.3 |
| **Java** | Eclipse Temurin | 21 |
| **Database** | PostgreSQL | 15+ |
| **Cache** | Redis | 7+ |
| **JWT** | JJWT (io.jsonwebtoken) | 0.12.6 |
| **OAuth2** | Spring Security OAuth2 | - |
| **Testing** | JUnit 5, Mockito, H2, AssertJ | - |

---

## 🌐 Endpoints

### Endpoints de Autenticación

| Método | Endpoint | Auth | Descripción |
|--------|----------|------|-------------|
| `POST` | `/auth/register` | No | Registro local (User + Tenant FREE + OWNER) |
| `POST` | `/auth/login` | No | Login email/password (Rate limited: 5/15min) |
| `GET` | `/auth/user` | Sí | Datos del usuario actual |
| `POST` | `/auth/logout` | Sí | Logout (Revoca tokens en Redis) |
| `POST` | `/auth/refresh` | No | Refresh access token |

### Endpoints de Tenants

| Método | Endpoint | Auth | Descripción |
|--------|----------|------|-------------|
| `GET` | `/auth/tenants` | Sí | Lista de tenants del usuario |
| `POST` | `/auth/tenants/select` | Sí | Cambiar tenant activo |
| `POST` | `/auth/tenants` | Sí | Crear nuevo tenant |
| `GET` | `/auth/tenants/{id}/users` | OWNER/ADMIN | Listar miembros del tenant |
| `PUT` | `/auth/tenants/{id}/users/{uid}/role` | OWNER/ADMIN | Cambiar rol (validación jerarquía) |
| `DELETE` | `/auth/tenants/{id}/users/{uid}` | OWNER | Desvincular usuario (soft delete) |

### Endpoints de Invitaciones

| Método | Endpoint | Auth | Descripción |
|--------|----------|------|-------------|
| `GET` | `/auth/invitations` | Sí | Invitaciones pendientes |
| `POST` | `/auth/invitations` | Sí | Crear invitación (OWNER/ADMIN) |
| `POST` | `/auth/invitations/accept` | Sí | Aceptar invitación |
| `DELETE` | `/auth/invitations/{id}` | Sí | Cancelar invitación |

---

## 🔐 Variables de Entorno y Secrets

Para el desarrollo local y el CI, el sistema es inteligente:
- **Local/Prod:** Requiere variables en `.env` (Google ID, JWT Secret, etc.).
- **Tests (CI):** Usa automáticamente `src/test/resources/application-test.yaml` con credenciales ficticias seguras.

📘 **Guía de Secrets:** Ver [`.github/SECRETS.md`](../../.github/SECRETS.md)

---

## 📊 Roadmap de Implementación

### ✅ Completado
- [x] Configuración de seguridad OAuth2 y JWT.
- [x] Lógica de Multi-tenancy (Selección y Creación).
- [x] Gestión de Invitaciones y Roles con validación de jerarquía.
- [x] **Auth Local** (User/Password): Register + Login.
- [x] **Rate Limiting** con Redis en `/login`.
- [x] **CRUD Usuarios**: Listar, cambiar rol, desvincular (soft delete).
- [x] **@PreAuthorize** en endpoints sensibles (OWNER/ADMIN).
- [x] Soft delete forense (`deleted_at` ZonedDateTime) + `is_active`.
- [x] **IA Ready**: Auditoría con IP + User-Agent en login/register.
- [x] Validación de `maxUsers` (Plan FREE) en invitaciones.
- [x] JWT enriquecido con claim `plan`.
- [x] Unit Tests con alta cobertura.
- [x] CI/CD alineado para Staging (OCI ARM) y Producción (AMD64).

### 📝 Pendiente
- [ ] **Unit Tests: JwtServiceImpl** — Generación y validación de JWT.
- [ ] **Testcontainers** — Integración con PostgreSQL real (no H2).
- [ ] **Transfer ownership** — Transferir rol OWNER antes de desvincularse.
- [ ] **Password reset** — Forgot password con token por email.
- [ ] **Session management** — Listar/revoke sesiones activas.
- [ ] **Refresh token rotation** — Invalidar refresh tras cada uso.
- [ ] **Dashboard de auditoría** — Endpoint para consultar `audit_log`.

---

<div align="center">

**PyMes Admin - Auth Microservice** | Estado: **Core Estabilizado** 💎

[![Build & Test](https://github.com/dio-quincarDev/pymes-admin/actions/workflows/ci.yml/badge.svg)](https://github.com/dio-quincarDev/pymes-admin/actions)
[![Java 21](https://img.shields.io/badge/Java-21-blue.svg)](https://openjdk.org/projects/jdk/21/)

</div>
