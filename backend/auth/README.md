# PyMes Admin - Auth Microservice 🔐

> **Spring Boot OAuth2** - Servicio de autenticación, gestión de usuarios y multi-tenancy para la plataforma PyMes Admin

---

## 📋 Descripción

Este microservicio es el **centro de identidad** de la arquitectura, responsable de la autenticación, identidad del usuario y gestión de la estructura multi-tenant.

### 🏗️ Evolución Arquitectónica: Desacoplamiento de Dominios
El servicio está diseñado con una **arquitectura orientada a dominios (SRP)**, logrando una separación clara de responsabilidades:

1.  **Auth Domain**: Autenticación pura (Login, Registro, Refresh, Logout).
2.  **User Domain**: Identidad y perfil del usuario autenticado.
3.  **Tenant Domain**: Estructura multi-tenant (Crear y seleccionar empresas).
4.  **Member Domain**: Gestión de usuarios *dentro* de una empresa (Roles y permisos).
5.  **Invitation Domain**: Ciclo de vida completo de invitaciones.

---

## ⚙️ Configuración & Perfiles

El microservicio utiliza **Perfiles de Maven** para gestionar diferentes entornos de forma segura y eficiente.

### 📋 Perfiles Disponibles

| Perfil | Propósito | Comando de Ejecución |
|--------|-----------|----------------------|
| **`dev`** (Default) | Desarrollo local con logs en `DEBUG` y conexión a `localhost`. | `./mvnw spring-boot:run -Pdev` |
| **`stg`** | Entorno de Staging/QA. Configuración estricta vía variables de entorno. | `./mvnw clean package -Pstg` |
| **`prod`** | Producción. Máxima seguridad, logs en `WARN` y optimización de recursos. | `./mvnw clean package -Pprod` |

### 🔐 Gestión de Secretos

**IMPORTANTE:** Nunca se deben incluir secretos en los archivos `application.yaml`.
- En **desarrollo**, utiliza un archivo `.env` en la raíz de `backend/auth/`. El proyecto usa `spring-dotenv` para cargarlos automáticamente.
- En **Staging/Producción**, las variables de entorno deben ser inyectadas por el orquestador (Docker Compose, Kubernetes o GitHub Secrets).

Variables críticas requeridas:
- `JWT_SECRET`: Clave para firmar tokens.
- `DB_PASSWORD`: Contraseña de PostgreSQL.
- `SPRING_MAIL_PASSWORD`: App password para el envío de correos.
- `GOOGLE_CLIENT_SECRET` / `FACEBOOK_CLIENT_SECRET`: Credenciales OAuth2.

---

## 🌐 Endpoints (V1)

La API está organizada bajo la ruta base `/api/v1` y sigue una estructura RESTful por recursos:

### 🔑 Autenticación (`/auth`)
| Método | Endpoint | Descripción |
|--------|----------|-------------|
| `POST` | `/register` | Registro atómico (User + Tenant FREE + OWNER) |
| `POST` | `/login` | Login email/password (Rate limited IP+Email) |
| `POST` | `/refresh` | Refresca el access token |
| `POST` | `/logout` | Invalida la sesión actual |
| `POST` | `/verify-email` | Verifica email con token recibido por correo |
| `POST` | `/resend-verification` | Reenvía token de verificación |
| `POST` | `/forgot-password` | Solicita enlace de recuperación de contraseña |
| `POST` | `/reset-password` | Establece nueva contraseña con token de recuperación |

### 👤 Usuarios (`/users`)
| Método | Endpoint | Descripción |
|--------|----------|-------------|
| `GET` | `/me` | Obtiene el perfil del usuario autenticado |

### 🏢 Tenants (`/tenants`)
| Método | Endpoint | Descripción |
|--------|----------|-------------|
| `GET` | `/` | Lista todas las empresas a las que pertenece el usuario |
| `POST` | `/` | Crea una nueva empresa/tenant |
| `POST` | `/select` | Selecciona la empresa activa y genera nuevos tokens |

### 👥 Miembros (`/tenants/{id}/members`)
| Método | Endpoint | Descripción |
|--------|----------|-------------|
| `GET` | `/` | Lista los miembros activos del tenant (OWNER/ADMIN) |
| `PUT` | `/{userId}/role` | Cambia el rol de un miembro (Validación de jerarquía) |
| `DELETE` | `/{userId}` | Desvincula a un miembro del tenant (Solo OWNER) |

### ✉️ Invitaciones (`/invitations`)
| Método | Endpoint | Descripción |
|--------|----------|-------------|
| `GET` | `/` | Lista invitaciones pendientes del usuario actual |
| `POST` | `/` | Envía una invitación a un nuevo usuario |
| `POST` | `/accept` | Acepta una invitación activa |
| `DELETE` | `/{id}` | Cancela una invitación pendiente |

---

## 🛠️ Stack Tecnológico & Calidad

- **Core:** Spring Boot 3.4.3, Java 21, MapStruct, Lombok.
- **Seguridad:** Spring Security OAuth2 (Google/FB), JWT (JJWT 0.12.6), Redis (Blacklist & Rate Limiting IP+Email).
- **Persistencia:** PostgreSQL, Flyway, Soft Delete Forense (`deleted_at`).
- **Testing:** JUnit 5, Mockito, **Testcontainers** (PostgreSQL 15 + Redis 7).

### 📊 Resultados de Tests

| Tipo | Cantidad | Ejecución |
|------|----------|-----------|
| Unitarios (Mockito) | 52 tests | `mvn test` |
| Integración (Testcontainers) | 17 tests | `mvn verify` |
| Consistencia (API Paths) | 12 tests | `mvn test` |
| **Total** | **81 tests** | `mvn verify` |

**Cobertura de integración:**
- **AuthApiIntegrationTest** → register, login, logout, refresh token (happy paths + edge cases)
- **AuthApplicationTests** → contexto completo con PostgreSQL real + Redis + Flyway

---

## 🔒 Seguridad Implementada

- **JWT validado por dominio**: `JwtService.validateToken()` retorna `ValidatedToken` o lanza excepciones del dominio (`TokenExpiredException`, `TokenInvalidException`, `TokenRevokedException`). El filtro tiene un solo `catch (AuthApiException)`.
- **Rate limiting**: Bloqueo por combinación `IP:email` en login (5 intentos → 429).
- **Revocación de tokens**: Blacklist en Redis con TTL automático.
- **Password hashing**: BCrypt con validación de fortaleza (mínimo 1 letra + 1 número, 8+ caracteres).
- **Soft delete forense**: `deleted_at` en `users`, `tenants`, `user_tenants`.
- **Audit log**: Registro de REGISTER y LOGIN con IP y User-Agent.
- **Recuperación de contraseña**: Tokens en Redis (TTL 15 min) + timing attack prevention en `POST /forgot-password`.

---

## 🔗 Consistencia de Rutas API

Todas las rutas están centralizadas en `ApiPathConstants` y validadas automáticamente por **12 tests de consistencia** que usan reflection (`org.reflections`) para escanear controllers y detectar strings hardcodeados o redundancias en el `SecurityConfig` whitelist.

**Beneficio:** Si alguien cambia una ruta o agrega un endpoint sin constante, el test falla en CI/CD.

---

## 📅 Próximas Fases

| Fase | Descripción | Estado |
|------|-------------|--------|
| 🔴 **Fase 1** | Refresh Token Rotation (blacklist del token viejo en Redis) | ⏳ Pendiente |
| 🟡 **Fase 2** | ✅ Verificación de email + ✅ Recuperación de contraseña | ✅ Completado |
| 🟢 **Fase 3** | Cierre del flujo de invitaciones (registro vía invitación, auditoría) | ⏳ Pendiente |
| 🔵 **Fase 4** | Enterprise: Transfer Ownership, Dashboard Auditoría, CI/CD | ⏳ Pendiente |

---

<div align="center">

**PyMes Admin - Auth Microservice** | Estado: **Desacoplado, Escalable, Testeable, Endurecido & Password Recovery Ready** 🔒

[![Build & Test](https://github.com/dio-quincarDev/pymes-admin/actions/workflows/ci.yml/badge.svg)](https://github.com/dio-quincarDev/pymes-admin/actions)
[![Java 21](https://img.shields.io/badge/Java-21-blue.svg)](https://openjdk.org/projects/jdk/21/)
[![Testcontainers](https://img.shields.io/badge/Testcontainers-PostgreSQL%20%2B%20Redis-green.svg)](https://testcontainers.com/)

</div>
