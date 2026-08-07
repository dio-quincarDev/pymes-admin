# Estrategia: Sistema de Invitación por Email

> Documenta la estrategia extraída del branch `refactor/invitation-attempt`, con el conocimiento de los daily reports de los días 22-24 de julio 2026.

---

## Resumen Ejecutivo

Flujo de invitación por email que permite a un OWNER o ADMIN invitar usuarios nuevos a un tenant. El usuario invitado puede registrarse y aceptar la invitación en una sola transacción atómica. El endpoint de registro por invitación es **separado** del registro normal — nunca se contamina `RegisterRequest`.

---

## Flujo del usuario

```
OWNER/ADMIN                              INVITADO (sin cuenta)
     │                                          │
     ▼                                          ▼
TeamsPage → "Invite Member"          Click en link del email
     │                                          │
     ▼                                          ▼
POST /invitations/send              GET /invitations/{token}/info
(envía email con link)              (preview: email, nombre tenant)
                                            │
                                            ▼
                                   Form: nombre, email, contraseña
                                            │
                                            ▼
                                   POST /invitations/{token}/register
                                   ── crea User
                                   ── crea UserTenant con el role del invitación
                                   ── marca invitation.acceptedAt = now
                                   ── genera JWT + refresh token
                                            │
                                            ▼
                                   authStore.setSession() → Dashboard
```

**Si el usuario ya tiene cuenta** (autenticado), solo necesita dar click en "Aceptar" → `POST /invitations/accept` (endpoint existente, no se toca).

---

## Arquitectura del backend

### 3 endpoints nuevos (todos públicos)

| Método | Ruta | Auth | Descripción |
|--------|------|------|-------------|
| `GET` | `/invitations/{token}/info` | No | Retorna email + nombre del tenant (preview antes de registrarse) |
| `POST` | `/invitations/{token}/register` | No | Crea usuario + acepta invitación + retorna JWT |
| `POST` | `/invitations/accept` | Sí (JWT) | Acepta invitación para usuario autenticado (ya existe) |

### Separación de flujos de registro

```
/auth/register          → Registro normal (sin invitación)
/invitations/{token}/register → Registro + aceptación de invitación (transacción atómica)
```

**Por qué separado:** El registro normal no conoce tenants, roles ni invitaciones. Mezclarlos crea un `RegisterRequest` con campos condicionales, validación inconsistente y dos caminos de error. Con endpoints separados, cada flujo tiene su propio DTO, su propia validación, y sus propios errores.

### Transacción atómica en `registerAndAccept()`

```java
// InvitationServiceImpl.java — pseudocódigo
@Transactional
public AuthResponse registerAndAccept(String token, InvitationRegisterRequest request) {
    // 1. Validar token no expirado
    Invitation invitation = invitationRepository.findByToken(token)
        .orElseThrow(() -> InvitationException.of(CodigoError.INVITATION_NOT_FOUND, token));

    if (invitation.getExpiresAt().isBefore(Instant.now()))
        throw InvitationException.of(CodigoError.INVITATION_EXPIRED, token);

    // 2. Crear usuario
    UserEntity user = UserEntity.builder()
        .name(request.name())
        .email(request.email().toLowerCase())
        .password(passwordEncoder.encode(request.password()))
        .emailVerified(true)  // ya tiene invitación válida
        .enabled(true)
        .build();
    userRepository.save(user);

    // 3. Crear vínculo tenant-usuario con el rol del invitación
    UserTenant userTenant = UserTenant.builder()
        .user(user)
        .tenant(invitation.getTenant())
        .role(invitation.getRole())
        .acceptedAt(Instant.now())
        .build();
    userTenantRepository.save(userTenant);

    // 4. Marcar invitación como aceptada
    invitation.setAcceptedAt(Instant.now());
    invitationRepository.save(invitation);

    // 5. Generar JWT + refresh token y retornar AuthResponse
    return buildAuthResponse(user, invitation.getTenant());
}
```

Si falla en cualquier paso, la transacción revierte todo — no queda usuario parcial sin vínculo.

---

## Archivos a crear/modificar

### Backend Auth (10 archivos)

```
backend/auth/src/main/java/auth/pymes/common/models/entities/Tenant.java
  → maxUsers: 1 → 2

backend/auth/src/main/resources/db/migration/V3__plan_cooldown.sql        (NUEVO)
  → ALTER TABLE auth.tenants ADD COLUMN IF NOT EXISTS last_role_change_at TIMESTAMP WITH TIME ZONE;

backend/auth/src/main/java/auth/pymes/common/models/dto/request/InvitationRegisterRequest.java  (NUEVO)
  → record { name, email, password }

backend/auth/src/main/java/auth/pymes/common/models/dto/response/InvitationInfoResponse.java    (NUEVO)
  → record { email, tenantName }

backend/auth/src/main/java/auth/pymes/utils/exception/CodigoError.java
  → + ROLE_CHANGE_COOLDOWN("ROLE005", "Role changes on the FREE plan are limited to once every {0} days", CONFLICT)

backend/auth/src/main/java/auth/pymes/controller/InvitationApi.java
  → + GET /{token}/info
  → + POST /{token}/register

backend/auth/src/main/java/auth/pymes/controller/impl/InvitationApiController.java
  → + getInvitationInfo() → llama invitationService.getInvitationInfo(token)
  → + register() → llama invitationService.registerAndAccept(token, request)

backend/auth/src/main/java/auth/pymes/service/InvitationService.java
  → + InvitationInfoResponse getInvitationInfo(String token)
  → + AuthResponse registerAndAccept(String token, InvitationRegisterRequest request)

backend/auth/src/main/java/auth/pymes/service/impl/InvitationServiceImpl.java
  → + getInvitationInfo(): busca invitation por token, retorna email + tenant name
  → + registerAndAccept(): transacción atómica (ver arriba)

backend/auth/src/main/java/auth/pymes/service/impl/MemberServiceImpl.java
  → + role change cooldown check: si plan FREE, verificar last_role_change_at + 30 días

backend/auth/src/main/java/auth/pymes/common/config/SecurityConfig.java
  → + "*/info" y "*/register" a WHITE_LIST

backend/auth/src/main/resources/application-dev.yaml
  → + plan.free.role-change-cooldown-days: 30
```

### Backend Gateway (1 archivo)

```
backend/gateway-pymes/src/main/java/dev/dioquincar/gateway_pymes/filter/RouterValidator.java
  → + /api/v1/invitations/accept
  → + /api/v1/invitations/*/info
  → + /api/v1/invitations/*/register
```

### Frontend (8 archivos)

```
frontend/pymes/src/modules/auth/types/index.ts
  → + plan en User (string)
  → + activeTenant.plan (string)
  → + InvitationRegisterRequest { name, email, password }
  → + InvitationInfo { email, tenantName }

frontend/pymes/src/modules/auth/store/index.ts
  → + selectTenant(tenantId): cambia activeTenant y refresca user
  → pasamanos de plan en setSession

frontend/pymes/src/modules/auth/services/invitation.service.ts
  → + getInvitationInfo(token): GET /invitations/{token}/info
  → + registerAndAccept(token, request): POST /invitations/{token}/register

frontend/pymes/src/modules/auth/services/member.service.ts    (NUEVO)
  → getMembers(): GET /members
  → updateRole(userId, role): PATCH /members/{userId}/role
  → removeMember(userId): DELETE /members/{userId}

frontend/pymes/src/modules/auth/pages/AcceptInvitationPage.vue
  → refactor: carga info vía getInvitationInfo()
  → si no autenticado: form de registro + llama registerAndAccept()
  → si autenticado: botón "Aceptar" (flujo existente)
  → errores manejados con código (INV001, INV002, etc.)

frontend/pymes/src/modules/core/pages/TeamsPage.vue              (NUEVO)
  → tabla de miembros (name, email, role, joinedAt)
  → botón "Invite Member" → dialog email + role
  → change role dialog (OWNER/ADMIN)
  → remove member (OWNER only)

frontend/pymes/src/modules/core/router/routes.ts
  → + ruta teams: { path: 'teams', name: 'teams', component: TeamsPage, meta: { ownerOnly: true, roles: ['OWNER', 'ADMIN'] } }

frontend/pymes/src/layouts/MainLayout.vue
  → + nav item "Teams" (visible solo OWNER/ADMIN)
  → filtro de nav items por rol del usuario activo
```

### Email templates (4 archivos)

```
backend/auth/src/main/resources/templates/email/invitation.html
  → rediseño Swiss style con branding PymeQ

backend/auth/src/main/resources/templates/email/verification.html
  → rediseño Swiss style

backend/auth/src/main/resources/templates/email/password-reset.html
  → rediseño Swiss style

backend/auth/src/main/resources/templates/email/fragments/layout.html
  → layout base para todos los templates (nuevo fragment)
```

---

## Lo que NO se toca

| Archivo | Por qué no |
|---------|-----------|
| `AuthServiceImpl.java` | Flujo de registro normal intacto |
| `RegisterRequest.java` | Sin campos de invitación |
| `AuthApiController.java` | No se agregan endpoints aquí |
| `OAuth2AuthenticationSuccessHandler.java` | El cambio de "Mi Empresa" → nombre Google es opcional (mejora cosmética) |
| Tests existentes (130) | No se rompen — no se cambian contratos existentes |

---

## Lección del intento anterior

El primer intento (`refactor/invitation-attempt`) contaminó `RegisterRequest` con campos condicionales (`invitationToken`, `invitationRole`), creando un solo endpoint de registro que manejaba dos flujos completamente diferentes. El resultado: validación inconsistente, errores difíciles de trazar, y un DTO que no representaba un solo concepto.

**La corrección:** endpoints separados, DTOs separados, flujos separados. Cada endpoint hace una cosa y la hace bien.

---

## Verificación

| Paso | Comando |
|------|---------|
| Auth tests | `cd backend/auth && ./mvnw test -B` (debe mantener 130 tests) |
| Auth integration | `cd backend/auth && ./mvnw verify -B -Dspring.profiles.active=integration` |
| Gateway build | `cd backend/gateway-pymes && ./mvnw test -B` |
| Frontend build | `cd frontend/pymes && npm run build` (lint + vue-tsc) |
| Docker check | `docker compose config` |

---

## Prioridad de implementación

1. **MVP (sin cooldown):** Tenant maxUsers → 2, register+accept endpoints, TeamsPage, nav por roles
2. **Cooldown (post-MVP):** V3 migration, role change cooldown, `plan.free.role-change-cooldown-days`
3. **Email redesign (opcional):** Swiss style templates, layout fragment

---

*Creado: 2026-07-24 | Análisis de `feature/core..refactor/invitation-attempt`*
