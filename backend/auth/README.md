# PyMes Admin - Auth Microservice 🔐

> **Spring Boot 3.4.3 + Java 21** - Centro de Identidad Multi-tenant con Seguridad de Grado Industrial (RTR, jti, Reuse Detection)

---

## 📋 Descripción

Este microservicio es el **núcleo de identidad** de la plataforma PyMes Admin. No solo gestiona el acceso, sino que orquesta la estructura multi-tenant de forma atómica y segura, diseñado para escalar en entornos SaaS B2B.

### 🏗️ Arquitectura Orientada a Dominios (SRP)
Separación física y lógica de responsabilidades para un mantenimiento sin fricciones:
1.  **Auth Domain**: Ciclo de vida de sesión (Login, Registro, **RTR**, Logout).
2.  **User Domain**: Gestión de identidad y perfiles globales.
3.  **Tenant Domain**: Aprovisionamiento de empresas (Planes, Límites, Selección).
4.  **Member Domain**: Jerarquía de permisos y roles (`OWNER > ADMIN > CONTABLE > VIEWER`).
5.  **Invitation Domain**: Flujo completo de onboarding para nuevos colaboradores.

---

## 🔒 Seguridad Avanzada (Implementada)

### 🔄 Refresh Token Rotation (RTR)
Implementación de un motor de seguridad atómico en `JwtService`:
- **Rotación por cada uso**: Al solicitar un nuevo Access Token, el Refresh Token viejo se invalida y se emite uno nuevo.
- **Detección de Reuso**: Si se intenta usar un Refresh Token ya revocado, el sistema detecta un posible compromiso y **revoca automáticamente todos los tokens del usuario**, forzando un re-login global.

### 💎 Identidad Única de Tokens (`jti`)
- Inclusión del claim **`jti` (JWT ID)** en cada token generado.
- Garantiza unicidad criptográfica absoluta, eliminando colisiones de hash en la base de datos incluso bajo condiciones de alta concurrencia.

### 🛡️ Perímetro Endurecido
- **Rate Limiting**: Bloqueo inteligente por combinación `IP:Email` en login (5 intentos → 429).
- **Password Hashing**: BCrypt con validación de fortaleza (mínimo 1 letra + 1 número).
- **Audit Log**: Trazabilidad completa de REGISTER y LOGIN con IP y User-Agent.
- **Timing Attack Prevention**: Recuperación de contraseña con respuestas de tiempo constante.
- **Soft Delete Forense**: Uso de `deleted_at` en todas las entidades críticas.

---

## 🛠️ Stack Tecnológico & Calidad

- **Core:** Spring Boot 3.4.3, Java 21, MapStruct 1.6.3.
- **Security:** Spring Security OAuth2 (Google/FB), JJWT 0.12.6, Redis (Blacklist).
- **Persistencia:** PostgreSQL 15, Flyway (V5: UNIQUE constraints on tokens).
- **Testing:** JUnit 5, Mockito, **Testcontainers** (Postgres + Redis).

### 📊 Suite de Pruebas Automatizadas

| Tipo de Test | Cantidad | Descripción |
|--------------|----------|-------------|
| **Unitarios** | 67 tests | Cobertura total de servicios y lógica de rotación. |
| **Integración**| 17 tests | Flujos E2E con PostgreSQL y Redis reales. |
| **Consistencia**| 12 tests | Validación de rutas API vía Reflection. |
| **TOTAL** | **96 tests** | **0 fallos - 100% Integridad.** |

---

## 🌐 Endpoints Principales (V1)

Ruta base: `/api/v1`

| Recurso | Endpoint | Operación Crítica |
|---------|----------|-------------------|
| **Auth** | `/register` | Registro atómico: User + Tenant FREE + Rol OWNER. |
| **Auth** | `/refresh` | Rotación de tokens con validación de reuso. |
| **Auth** | `/verify-email` | Verificación de identidad vía Redis (TTL 15m). |
| **Tenants**| `/select` | Cambio de contexto de empresa con regeneración de tokens. |
| **Members**| `/{userId}/role` | Cambio de rol con validación de jerarquía de poder. |

---

## ⚙️ Configuración & DevOps

### 📋 Perfiles de Maven
- **`dev`**: Desarrollo local con logs en `DEBUG`. Carga secretos vía `.env`.
- **`stg`**: Staging con configuración estricta de variables de entorno.
- **`prod`**: Producción optimizada con logs en `WARN`.

### 🚀 CI/CD
- **GitHub Actions**: Pipeline automatizado que ejecuta `mvn verify` en cada PR.
- **Docker-Ready**: El microservicio está listo para ser orquestado junto a PostgreSQL y Redis.

---

## 📅 Roadmap Actualizado (Abril 2026)

| Fase | Descripción | Estado |
|------|-------------|--------|
| 🔴 **Fase 1** | **RTR + Reuse Detection + jti uniqueness** | ✅ COMPLETADO |
| 🟡 **Fase 2** | ✅ Verificación de email + ✅ Recuperación de contraseña | ✅ COMPLETADO |
| 🟡 **Fase 3** | **CORS Real Bean + Alertas Activas de Seguridad** | ⏳ Siguiente |
| 🟢 **Fase 4** | PKCE para SPAs + Device Fingerprinting | 📅 En Plan |
| 🔵 **Fase 5** | Enterprise: MFA (TOTP), SSO / SAML, Ownership Transfer | 📅 Backlog |

---

<div align="center">

**PyMes Admin - Auth Microservice** | Estado: **Production-Ready & SaaS-Hardened** 🔒

[![Build & Test](https://github.com/dio-quincarDev/pymes-admin/actions/workflows/ci.yml/badge.svg)](https://github.com/dio-quincarDev/pymes-admin/actions)
[![Java 21](https://img.shields.io/badge/Java-21-blue.svg)](https://openjdk.org/projects/jdk/21/)
[![Testcontainers](https://img.shields.io/badge/Testcontainers-PostgreSQL%20%2B%20Redis-green.svg)](https://testcontainers.com/)

</div>
