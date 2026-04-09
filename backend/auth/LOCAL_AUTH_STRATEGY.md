# Estrategia Maestra: Auth Local, Multi-tenant SaaS & IA Ready 🔐🚀

Este documento define la arquitectura para integrar la autenticación por usuario/contraseña y el sistema de permisos jerárquicos, alineado con la visión de la plataforma SaaS PyMes Admin.

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

## 5. Roadmap de Ejecución (Actualizado 2026-04-09)

### ✅ Completado recientemente
- [x] **Desacoplamiento total**: Separación en dominios Auth, User, Tenant, Member e Invitation.
- [x] **Estandarización de Mappers**: Uso profesional de MapStruct en todos los servicios.
- [x] **Refactor de DTOs**: Eliminación de redundancia entre `UserTenantResponse` y `MemberResponse`.
- [x] **Unit Tests JWT**: Cobertura del 100% en generación, validación y seguridad de firmas.
- [x] **Eliminar `JwtTokenProvider`**: Bean zombie legacy eliminado. ✅ COMPLETADO.
- [x] **Refactor `OAuth2AuthenticationSuccessHandler`**: `RuntimeException` → `ResourceNotFoundException`. ✅ COMPLETADO.
- [x] **Validación de Password**: Regex en `RegisterRequest` (mínimo 1 letra + 1 número). ✅ COMPLETADO.
- [x] **Límite de Plan FREE**: Usuario OWNER solo puede crear 1 tenant FREE. ✅ COMPLETADO.
- [x] **Rate Limiting IP + Email**: Bloqueo por combinación `IP:email` en login. ✅ COMPLETADO.

---

## 6. Pendientes (Orden de Importancia) 📋

### 🔴 Prioridad 1: Integridad & Deuda Crítica
1.  **Testcontainers (PostgreSQL real)**: Reemplazar H2 en tests para validar UUID nativo y JSONB.

### 🟡 Prioridad 2: Robustez de Negocio & Seguridad
1.  **Recuperación de Contraseña**:
    - **Estrategia**: Token por email con Redis (TTL 15 min). Sin tabla nueva en BD.
    - **Flujo**:
      1. `POST /auth/forgot-password` → genera token UUID, guarda en Redis `reset:{email}` (TTL 15 min).
      2. Usuario recibe link: `frontend.com/reset?token=xxx`.
      3. `POST /auth/reset-password` → valida token en Redis, actualiza password (BCrypt), elimina token.
    - **Endpoints**: `POST /api/v1/auth/forgot-password`, `POST /api/v1/auth/reset-password`.
    - **Seguridad**: Rate limiting en forgot-password, auditoría de cambio en `audit_log`.

### 🟢 Prioridad 3: Roadmap Funcional
1.  **Transfer Ownership**: Endpoint para ceder el rol de OWNER.
2.  **Refresh Token Rotation**: Invalidación del token anterior tras cada refresh.
3.  **Dashboard de Auditoría**: API paginada para consultar los `audit_log` (IA Ready).

---

### 📊 Calificación de Salud Arquitectónica: 9.5/10
> Deuda técnica crítica eliminada (`JwtTokenProvider`, `RuntimeException`). Seguridad reforzada con rate limiting IP+Email y validación de password. Resolver Testcontainers llevará al 10/10.
