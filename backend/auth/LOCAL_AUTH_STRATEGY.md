# Estrategia Maestra: Auth Local, Multi-tenant SaaS & IA Ready 🔐🚀

Este documento define la arquitectura para integrar la autenticación por usuario/contraseña y el sistema de permisos jerárquicos, alineado con la visión de la plataforma SaaS PyMes Admin.

## 1. Evolución del Modelo de Datos (IA & SaaS Ready)

### UserEntity (Identidad Universal)
- **Campo `password`**: Almacenar hash BCrypt.
- **Flexibilidad de Proveedor**: `providerId` opcional para permitir registros locales y OAuth2 híbrido.
- **Auditoría Forense**: Implementar `deleted_at` (ZonedDateTime) en lugar de un simple boolean `is_active`, permitiendo que el **IA Toolkit** realice análisis históricos de usuarios desvinculados.

### UserTenant (Soft Delete & Jerarquía)
- **Anotaciones Hibernate**: `@SQLDelete` y `@Where` para desvinculaciones lógicas.
- **Peso Jerárquico**: Definir un orden de poder: `OWNER (4) > ADMIN (3) > CONTABLE (2) > VIEWER (1)`.

---

## 2. Onboarding de Tenant (Aprovisionamiento SaaS)

El registro local no es solo un `INSERT` de usuario, es el **Aprovisionamiento del SaaS**:
- **Registro Atómico (`POST /auth/register`)**:
  1. Validar unicidad de email y fortaleza de contraseña.
  2. Crear `UserEntity` (Dueño).
  3. Crear `Tenant` (Empresa) con **Plan FREE** por defecto (`maxUsers = 1`).
  4. Vincular mediante `UserTenant` con rol **`OWNER`**.
- **Límites de Plan**: El `AuthService` validará que las invitaciones no superen el `maxUsers` del Tenant (Plan FREE = 1 usuario).

---

## 3. JWT Enriquecido (Pasaporte Multi-service)

El JWT emitido por el Auth Service servirá como pasaporte para los futuros **Core Service** y **IA Service (FastAPI)**:
- **Claims Requeridos**:
  - `sub`: User ID (UUID)
  - `email`: Email del usuario
  - `tenant_id`: ID del tenant activo
  - `role`: Rol en el tenant actual (`OWNER`, `ADMIN`, etc.)
  - `plan`: Tipo de suscripción (`FREE`, `PRO`, `ENTERPRISE`) para habilitar módulos de IA en el toolkit forense.

---

## 4. Seguridad de Jerarquía B2B

Se implementará seguridad en dos capas para proteger la integridad del Tenant:

### A. Capa de Acceso (Controller - `@PreAuthorize`)
Filtrado rápido basado en el Rol del JWT:
- `hasAnyAuthority('OWNER', 'ADMIN')`: Gestión de invitaciones y usuarios.
- `hasAuthority('OWNER')`: Cambios de Plan, Facturación y borrado del Tenant.

### B. Capa de Lógica (Service - Jerarquía Crítica)
Validación explícita para prevenir abusos de poder:
- **Restricción Inviolable**: Un `ADMIN` tiene prohibido borrar, modificar o degradar a un `OWNER` o a otro `ADMIN`.
- **Protección del Dueño**: El `OWNER` no puede borrarse a sí mismo sin transferir la propiedad del Tenant (SaaS Integrity).

---

## 5. Roadmap de Ejecución Refinado

> **Estado**: ✅ Completado (2026-04-05)
> Ver sección 6 para deuda técnica y feedback.

---

## 6. Deuda Técnica Crítica (Próximo Sprint)

| Prioridad | Problema | Riesgo | Acción Requerida |
|-----------|----------|--------|-----------------|
| 🔴 | `JwtServiceImpl` sin tests | Si genera tokens mal, todo el sistema se cae | Tests unitarios: generación, extracción, expiración, revocación |
| 🔴 | H2 en tests ≠ PostgreSQL | H2 no valida UUID, JSONB, dialecto | Reemplazar con Testcontainers (PostgreSQL real en CI) |
| 🟡 | `UserTenantResponse` reutilizado con semántica distinta | `tenantId` se usa como `userId`, confusión en API | Crear DTO dedicado `UserDetailResponse` |
| 🟡 | Rate Limiting solo por IP | Atacante con proxy rota IP fácilmente | Combinar IP + email |
| 🟡 | Sin validación de complejidad de password | Solo `@Size(min=8)`, sin regex | Agregar validación de mayúsculas, números, especiales |

### 📝 Pendiente (Segundo Sprint)

| Item | Descripción |
|------|-------------|
| **Transfer ownership** | Endpoint para transferir rol OWNER antes de desvincularse |
| **Password reset** | Forgot password con token por email |
| **Session management** | Listar/revoke sesiones activas por usuario |
| **Refresh token rotation** | Invalidar refresh token tras cada uso |
| **Dashboard de auditoría** | Endpoint paginado para consultar `audit_log` |
| **Eliminar `JwtTokenProvider`** | Bean legacy sin uso, eliminar para evitar confusión |

---

## 7. Feedback de Arquitectura (Review 2026-04-05)

### ✅ Lo que funciona

- **Separación controller → service → repository**. Interface + Impl. Correcto y testeable.
- **OAuth2 + Local conviven sin mezclarse**. Flujos separados, mismo JWT.
- **Validación de jerarquía en service layer**, no solo en `@PreAuthorize`. Eso es seguridad real.
- **Soft delete pragmático**: `deleted_at` + `is_active` sin romper código existente.
- **Rate Limiting simple**: sin librerías pesadas, efectivo, fácil de mantener.
- **Auditoría IA Ready**: IP + User-Agent capturados en cada login/register.

### ❌ Lo que necesita atención

- **`JwtServiceImpl` es el componente más crítico sin cobertura**. No negociable.
- **H2 en tests miente**: no valida UUID, JSONB, ni dialecto PostgreSQL. CI puede pasar y prod romper.
- **`OAuth2AuthenticationSuccessHandler` lanza `RuntimeException` crudo**. Debería usar excepción tipada del proyecto.
- **`UserTenantResponse` es un hack**: reutiliza campos con semántica distinta (`tenantId` → `userId`). Crear DTO dedicado.
- **`JwtTokenProvider` (legacy) sigue como Spring bean zombie**. Eliminar.

## 8. Contexto de Mercado 2026

### Market Landscape

| Herramienta | Tipo | Precio | Cuándo usarlo |
|-------------|------|--------|---------------|
| **Auth0 (Okta)** | SaaS enterprise | $15K-$50K+/año | Presupuesto alto, no quieres pensar en auth |
| **Keycloak** | Open source, self-hosted | Gratis (caro de mantener) | Equipo dedicado, control total |
| **Clerk** | Dev-first SaaS | $25-$500/mes | Startups que quieren auth en 5 min |
| **Supabase Auth** | Ecosystem auth | Gratis (limitado) | Si ya usas Supabase |
| **Firebase Auth** | Google ecosystem | Gratis (hasta cierto punto) | Mobile-first o ya en GCP |

### Tu Nicho

No compites con Auth0. Compite con *"contraté un freelancer que me hizo algo que funciona más o menos"*.

### ROI Estimado

| Comparación | Auth0 (3 años) | Este Microservicio | Ahorro |
|-------------|---------------|-------------------|--------|
| **Licencias** | $45K - $150K | $0 | $45K-$150K |
| **Desarrollo** | $0 | $8K - $28K (una vez) | — |
| **Mantenimiento** | $0 | $2K-$5K/año | — |
| **Total 3 años** | **$45K - $150K** | **$14K - $43K** | **$30K - $107K** |

### Diferenciadores Reales (vs Auth0/Keycloak)

| Feature | Este MS | Auth0 | Keycloak |
|---------|---------|-------|----------|
| **Multi-tenant B2B real** | ✅ Nativo | ❌ Requiere custom code | ❌ Requiere extensión |
| **Jerarquía de roles (OWNER→VIEWER)** | ✅ Con validación | ❌ Roles planos | ⚠️ Configurable pero complejo |
| **Límites de plan (maxUsers)** | ✅ Nativo | ❌ Custom | ❌ Custom |
| **Auditoría IA Ready (IP + UA)** | ✅ Forense con timestamp | ⚠️ Logs básicos | ⚠️ Logs crudos |
| **Código auditable** | ✅ Tu repo | ❌ SaaS cerrado | ✅ Open source |
| **Sin vendor lock-in** | ✅ PostgreSQL + Redis | ❌ Lock-in total | ✅ Self-hosted |
| **Time to production** | 1-2 sprints | 1 día | 2-4 semanas de setup |

### Stack Justificado

| Decisión | Por qué |
|----------|---------|
| **Spring Boot 3 + Java 21** | Ecosistema enterprise, seguridad battle-tested, contratos de tipo fuertes. Ideal para auth donde un bug = breach |
| **PostgreSQL** | UUID nativo, JSONB para auditoría, row-level security (futuro). El estándar para datos críticos |
| **Redis** | Sub-millisecond para blacklist + rate limiting. No necesitas una DB para esto |
| **JWT stateless** | El auth service no necesita llamar a la DB para validar cada request. Escala horizontal sin esfuerzo |

---

### 📊 Calificación General: 7.5/10

> Tiene las piezas correctas en el lugar correcto. Los 2 gaps críticos (tests JWT + Testcontainers) son deuda técnica que **va a explotar en producción** si no se abordan. El resto es calidad de vida que se arregla en 1-2 sprints.
