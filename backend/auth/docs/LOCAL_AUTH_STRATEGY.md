# Estrategia Maestra: Auth Local, Multi-tenant SaaS & IA Ready 🔐🚀

Este documento define la arquitectura para integrar la autenticación por usuario/contraseña y el sistema de permisos jerárquicos, alineado con la visión de la plataforma SaaS PyMes Admin.

---

## 1. Evolución del Modelo de Datos (IA & SaaS Ready)

### UserEntity (Identidad Universal)
- **Campo `password`**: Almacenar hash BCrypt.
- **Flexibilidad de Proveedor**: `providerId` opcional para permitir registros locales y OAuth2 híbrido.
- **Auditoría Forense**: Implementar `deleted_at` (ZonedDateTime) en lugar de un simple boolean `is_active`.

### UserTenant (Soft Delete & Jerarquía)
- **Anotaciones Hibernate**: `@SQLDelete` y `@Where` para desvinculaciones lógicas.
- **Peso Jerárquico**: Definir un orden de poder: `OWNER (4) > ADMIN (3) > CONTABLE (2) > VIEWER (1)`.

---

## 2. Onboarding de Tenant (Aprovisionamiento SaaS)

El registro local no es solo un `INSERT` de usuario, es el **Aprovisionamiento del SaaS**:
- **Registro Atómico (`POST /api/v1/auth/register`)**:
  1. Validar unicidad de email y fortaleza de contraseña.
  2. Crear `UserEntity` (Dueño).
  3. Crear `Tenant` (Empresa) con **Plan FREE** por defecto (`maxUsers = 1`).
  4. Vincular mediante `UserTenant` con rol **`OWNER`**.
- **Límites de Plan**:
  - El `AuthService` validará que las invitaciones no superen el `maxUsers` del Tenant.
  - El `TenantService` validará que un usuario en Plan FREE no pueda crear más de 1 empresa como OWNER.

---

## 3. JWT Enriquecido (Pasaporte Multi-service)

El JWT emitido por el Auth Service servirá como pasaporte para los futuros servicios:
- **Claims Requeridos**: `userId`, `email`, `tenantId`, `role`, `plan`.

---

## 4. Seguridad de Jerarquía B2B

Se implementará seguridad en dos capas:
### A. Capa de Acceso (Controller - `@PreAuthorize`)
### B. Capa de Lógica (Service - Jerarquía Crítica)
- Un `ADMIN` no puede tocar a un `OWNER`.
- El `OWNER` no puede borrarse sin transferir la propiedad.

---

## 5. Testing con Testcontainers

### ✅ Infraestructura de tests de integración

**Estructura:**
```
src/test/java/
├── unit/                    # Unit tests (Mockito)
└── integration/             # Integration tests (Testcontainers)
    ├── AbstractIntegrationTest.java  # PostgreSQL + Redis
    ├── AuthApplicationTests.java     # Context load
    └── api/
        └── AuthApiIntegrationTest.java  # Endpoints
```

**依赖:**
- `spring-boot-testcontainers`
- `testcontainers:junit-jupiter`
- `testcontainers:postgresql`
- `testcontainers:redis`

**Configuración:**
- `.testcontainers.properties` → `reuse.enable=true`
- `AbstractIntegrationTest` → `postgres:15-alpine` + `redis:7-alpine`

---

## 6. Roadmap de Próximos Pasos 📋

### 🔴 Fase 1: Seguridad & Perímetro
- [ ] Alertas de Seguridad (Webhooks/Slack) en detección de reuso
- [ ] PKCE para SPAs
- [ ] Device Fingerprinting

### 🟡 Fase 2: Identidad & Social
- [ ] MFA (TOTP - Google Authenticator)
- [ ] SSO / SAML

### 🟢 Fase 3: Cierre del Flujo de Invitaciones
- [ ] Invitación completa con Email
- [ ] `acceptInvitation` con registro pre-asignado
- [ ] Auditoría en `audit_log`

### 🔵 Fase 4: Enterprise (post-MVP)
- [ ] Transfer Ownership
- [ ] Dashboard de Auditoría

---

## 📐 Estrategia JWT/OAuth2 Híbrida

### 🎯 El Problema
Usuarios registrados localmente (JWT) no podían crear empresas porque los servicios dependían de `OAuth2User`.

### 📐 Solución
1. Controladores reciben `Authentication` genérico
2. `TenantServiceImpl` inspecciona el principal:
   - Si `OAuth2User` → extrae `email`
   - Si `UserEntity` → extrae `getEmail()`
   - Fallback a `getName()`

---

## 📐 Rotación de Refresh Tokens (RTR)

### 🎯 El Problema
Refresh Tokens estáticos hasta expiración. Un atacante podía generar Access Tokens indefinidamente.

### 📐 Arquitectura RTR
1. Cliente envía `oldRefreshToken`
2. Validación atómica:
   - Verifica firma y expiración
   - Busca hash en PostgreSQL
   - **Detección de Reuso**: Si `revoked = true` → alarma
3. **Estrategia de Mitigación**: Revoca todos los tokens del usuario
4. Emisión: Token viejo `revoked`, nueva pareja (Access + Refresh)

### 💎 Unicidad (`jti`)
Claim `jti` con `UUID.randomUUID()` para evitar colisiones de tokens.

---

*Documentado: 2026-04-17*