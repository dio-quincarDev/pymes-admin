# PyMes Admin - Auth Microservice

Centro de Identidad Multi-tenant para la plataforma PyMes Admin.

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.3-6DB33F?logo=springboot)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-21-ED8B00?logo=openjdk&logoColor=ffffff)](https://www.oracle.com/java/technologies/downloads/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-336791?logo=postgresql)](https://www.postgresql.org/)
[![Redis](https://img.shields.io/badge/Redis-DC382D?logo=redis&logoColor=ffffff)](https://redis.io/)
[![OAuth2](https://img.shields.io/badge/OAuth2-2.0-000000?logo=oauth)](https://oauth.net/2/)
[![JWT](https://img.shields.io/badge/JWT-0.12.6-000000?logo=json)](https://jwt.io/)

---

## Descripcion

Este microservicio es el nucleo de identidad de la plataforma PyMes Admin. Gestiona el acceso y orquesta la estructura multi-tenant de forma atomica y segura, disenado para escalar en entornos SaaS B2B.

### Arquitectura Orientada a Dominios
Separacion fisica y logica de responsabilidades:
1. Auth Domain: Ciclo de vida de sesion (Login, Registro, RTR, Logout Global).
2. User Domain: Gestion de identidad y perfiles globales.
3. Member Domain: Gestion de membresias y roles dentro de tenants (OWNER > ADMIN > CONTABLE > VIEWER).
4. Invitation Domain: Flujo completo de invitaciones para nuevos colaboradores.

---

## Caracteristicas de Seguridad

### JWT y Tokens
- Refresh Token Rotation (RTR): Rotacion por cada uso, el Refresh Token viejo se invalida al solicitar uno nuevo.
- Deteccion de Reuso: Si se intenta usar un Refresh Token ya revocado, se revoca automaticamente todos los tokens del usuario.
- Identidad Unica de Tokens: Inclusion del claim `jti` (JWT ID) en cada token generado.

### OAuth2
- Google OAuth2 implementado via Gateway.
- Facebook: Pendiente de configuracion.

### Medidas de Proteccion
- Rate Limiting: Bloqueo por combinacion `IP:Email` en login (5 intentos -> 429).
- Password Hashing: BCrypt con validacion de fortaleza.
- Audit Log: Trazabilidad de REGISTER y LOGIN con IP y User-Agent.
- Timing Attack Prevention: Respuestas de tiempo constante en recuperacion de contrasena.
- Soft Delete: Uso de `deleted_at` en entidades.
- Token-Email Validation: Validacion cruzada token-email en verificacion.

---

## Stack Tecnologico y Calidad

- **Core:** Spring Boot 3.4.3, Java 21, MapStruct 1.6.3.
- **Security:** Spring Security OAuth2 (Google/FB), JJWT 0.12.6, Redis (Blacklist).
- **Persistencia:** PostgreSQL 15, Flyway (V5: UNIQUE constraints on tokens).
- **Testing:** JUnit 5, Mockito, **Testcontainers** (Postgres + Redis).

### Suite de Pruebas Automatizadas

| Tipo de Test | Cantidad | Descripción |
|--------------|----------|-------------|
| **Unitarios** | 124 tests | Cobertura total de servicios y lógica de rotación. |
| **Integración** | 31 tests | Flujos E2E con PostgreSQL y Redis reales. |
| **TOTAL** | **155 tests** | **0 fallos - 100% Integridad.** |

---

## Endpoints Principales

Ruta base: `/api/v1`

| Recurso | Endpoint | Descripcion |
|---------|----------|-------------|
| Auth | POST /register | Registro atomico: User + Tenant FREE + Rol OWNER (pending token). |
| Auth | POST /login | Login con email/password + Rate Limiting. |
| Auth | POST /logout | Logout global - invalida todos los refresh tokens del usuario. |
| Auth | POST /refresh | Rotacion de tokens con validacion de reuso. |
| Auth | POST /verify-email | Verificacion de identidad con validacion token-email cruzada. |
| Auth | POST /forgot-password | Recuperacion de contrasena via Redis. |
| Auth | POST /reset-password | Reset de contrasena con token valido. |
| Tenants | GET /tenants | Lista tenants del usuario (paginado). |
| Tenants | POST /tenants | Crear nuevo tenant (solo FREE). |
| Tenants | POST /tenants/select | Cambio de contexto de empresa con regeneracion de tokens. |
| Members | GET /tenants/{id}/members | Lista miembros de un tenant. |
| Members | PUT /tenants/{id}/members/{userId}/role | Cambio de rol con validacion de jerarquia. |
| Members | DELETE /tenants/{id}/members/{userId} | Desvincular miembro - solo OWNER. |
| Invitations | GET /invitations | Lista invitaciones pendientes del usuario. |
| Invitations | POST /invitations | Crear invitacion. |
| Invitations | POST /invitations/accept | Aceptar invitacion. |
| Invitations | DELETE /invitations/{id} | Cancelar invitacion. |

---

## Configuracion y DevOps

### Perfiles de Maven
- `dev`: Desarrollo local con logs en DEBUG. Carga secretos via .env.
- `stg`: Staging con configuracion estricta de variables de entorno.
- `prod`: Produccion optimizada con logs en WARN.

### CI/CD
- GitHub Actions: Pipeline automatizado que ejecuta `mvn verify` en cada PR.
- Docker-Ready: El microservicio esta listo para ser orquestado junto a PostgreSQL y Redis.

---

## Roadmap

| Fase | Descripcion | Estado |
|------|-------------|--------|
| Fase 1 | RTR + Reuse Detection + jti uniqueness | COMPLETADO |
| Fase 2 | Verificacion de email + Registro Pending Token + Token-Email Mismatch Fix | COMPLETADO |
| Fase 3 | OAuth2 Google + OAuth2 Intent Cookie | COMPLETADO |
| Fase 4 | Logout Global + Thymeleaf Email Templates | COMPLETADO |
| Fase 5 | Member Management (roles, invitaciones) | COMPLETADO |
| Fase 6 | Password Reset + Forgot Password | COMPLETADO |
| Fase 7 | Facebook OAuth2 | PENDIENTE |
| Fase 8 | MFA (TOTP), PKCE, Enterprise SSO | BACKLOG |

### Features Implementadas Recientemente

- Registro Pending Token: Usuario no se crea en DB hasta verificar email.
- Logout Global: Cierra todas las sesiones activas del usuario.
- Token-Email Mismatch Fix: Validacion cruzada token-email en verificacion.
- Member Management: Gestion de roles y membresias por tenant.
- Thymeleaf Email System: Plantillas profesionales responsivas.

### Problema Conocido

Facebook OAuth2: Pendiente de configuracion en Facebook Developer Console.

---

## Estado

Production-Ready

[![Build](https://github.com/dio-quincarDev/pymes-admin/actions/workflows/ci.yml/badge.svg)](https://github.com/dio-quincarDev/pymes-admin/actions)
[![Testcontainers](https://img.shields.io/badge/Testcontainers-PostgreSQL%20%2B%20Redis-2C8EBB?logo=testcontainers)](https://www.testcontainers.org/)
[![Flyway](https://img.shields.io/badge/Flyway-CC0200?logo=flyway)](https://flywaydb.org/)
[![JUnit5](https://img.shields.io/badge/JUnit5-25A162?logo=junit5&logoColor=ffffff)](https://junit.org/junit5/)
