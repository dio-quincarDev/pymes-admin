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

### Estado Actual (Core Logic Phase) 🚀

| Componente | Estado | Descripción |
|------------|--------|-------------|
| **OAuth2 Core** | ✅ Listo | Google/FB configurados con `SuccessHandler` propio. |
| **Seguridad JWT** | ✅ Listo | `JwtService` e `impl` con claims Multi-tenant. |
| **Filtro de Auth** | ✅ Listo | `JwtAuthenticationFilter` activo. |
| **Base de Datos** | ✅ Listo | Flyway V1 y esquema PostgreSQL/H2 (Test) alineados. |
| **Modelo de Datos**| ✅ Listo | Entidades con soft delete (@SQLDelete, @Where). |
| **DTOs & Mappers** | ✅ Listo | Java Records para mayor inmutabilidad y rendimiento. |
| **Exception Handling** | ✅ Listo | 8 excepciones + GlobalExceptionHandler. |
| **Auth Service** | ✅ Listo | Lógica de selección de tenant, invitaciones y tokens. |
| **Unit Testing** | ✅ Listo | 8 tests críticos (85%+ cobertura en AuthService). |
| **Redis Integration** | ✅ Listo | Blacklist de tokens (Logout) y caché de permisos. |

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

### Implementados y Verificados ✅

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| `GET` | `/auth/user` | Datos del usuario actual |
| `GET` | `/auth/tenants` | Lista de tenants del usuario (Paginado) |
| `POST` | `/auth/tenants/select` | Cambiar tenant activo (Genera nuevos JWT) |
| `POST` | `/auth/logout` | Logout (Revoca tokens en Redis) |
| `POST` | `/auth/refresh` | Refresh access token |
| `GET` | `/auth/invitations` | Invitaciones pendientes (Paginado) |
| `POST` | `/auth/invitations` | Crear invitación (Admin/Owner) |
| `POST` | `/auth/invitations/accept` | Aceptar invitación |
| `DELETE` | `/auth/invitations/{id}` | Cancelar invitación |

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
- [x] Gestión de Invitaciones y Roles.
- [x] Implementación de **Unit Tests** con alta cobertura.
- [x] Integración de Redis para Logout/Blacklist.
- [x] CI/CD alineado para Staging (OCI ARM) y Producción (AMD64).

### 📝 Pendiente
- [ ] Implementar Rate Limiting por IP en Redis.
- [ ] Lógica de registro local (User/Password) - *Opcional*.
- [ ] Testcontainers para integración total con Postgres real.
- [ ] Dashboard de auditoría avanzada.

---

<div align="center">

**PyMes Admin - Auth Microservice** | Estado: **Core Estabilizado** 💎

[![Build & Test](https://github.com/dio-quincarDev/pymes-admin/actions/workflows/ci.yml/badge.svg)](https://github.com/dio-quincarDev/pymes-admin/actions)
[![Java 21](https://img.shields.io/badge/Java-21-blue.svg)](https://openjdk.org/projects/jdk/21/)

</div>
