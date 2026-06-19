# Technical Class Reference — Auth Service

This document consolidates the technical specifications and non-obvious design details for the core classes of the `auth` microservice, eliminating file clutter and redundant method signatures.

---

## 🔐 Security & Identity

### `SecurityConfig`
- **Framework**: Spring Security 6.x (Stateless).
- **Session Policy**: `STATELESS` (Stateless token-based API).
- **Filter Chain Ordering**: `JwtAuthenticationFilter` is executed **before** `UsernamePasswordAuthenticationFilter`.
- **Public Whitelist**: Swagger/OpenAPI docs, Actuator endpoints, `/auth/register`, `/auth/login`, `/auth/refresh`, `/auth/verify-email`, `/auth/resend-verification`, `/auth/forgot-password`, `/auth/reset-password`.
- **Protected Routes**: All other paths require authentication.
- **Error Payloads**: Overrides Spring Security behaviors to return a standardized JSON error format:
  ```json
  {
    "codigo": "ERROR_CODE",
    "mensaje": "Human readable description",
    "path": "/api/v1/resource",
    "timestamp": "ISO-8601 String",
    "detalles": {}
  }
  ```

### `JwtServiceImpl`
- **Algorithm**: HS256 (HMAC with SHA-256).
- **Access Token Claims**: `sub` (email), `userId`, `tenantId`, `role`, `plan`, `jti` (unique UUID to prevent reuse).
- **Refresh Token Storage**: Stored in Postgres table `refresh_tokens` hashed with **SHA-256**. Payload only contains `userId` to minimize data exposure.
- **Token Rotation (RTR)**: Each refresh request invalidates the old token and issues a new pair.
- **Reuse Detection**: If a revoked refresh token is presented, the service immediately invalidates all tokens of that token family (global re-auth) to protect against hijacking.

### `TokenBlacklistService`
- **Mechanism**: Blacklists revoked JWT access tokens during logout.
- **Infrastructure**: Redis.
- **Key Schema**: `auth:token_blacklist:{token}`
- **TTL**: Dynamically set to the remaining lifetime of the token. Redis garbage-collects expired tokens automatically.

---

## 🏢 Multi-Tenancy & Authorization

### `AuthServiceImpl`
- **Registration**: Atomic transaction. Creates `UserEntity`, `Tenant` (FREE plan, max 1 tenant/user limit), and `UserTenant` with role `OWNER`. Triggers email verification.
- **Login**: Verifies `isActive` and `isEmailVerified` flags, authenticates via Spring's `AuthenticationManager`, resolves context, and records an `AuditLog` entry (extracting client IP via `X-Forwarded-For` -> `X-Real-IP` -> `RemoteAddr`).
- **Logout**: Revokes the access token and purges the user's refresh tokens for global session revocation.

### `PermissionCacheService`
- **Pattern**: Look-aside cache.
- **Key Schema**: `auth:permissions:{userId}:{tenantId}`
- **Value**: Serialized `List<String>` of permissions/roles.
- **TTL**: Hardcoded to **5 minutes** (eventual consistency).
- **Eviction**:
  - **Granular**: `invalidatePermissions(userId, tenantId)` on role updates.
  - **Global**: `invalidateAllUserPermissions(userId)` scans using Redis keys pattern (`auth:permissions:{userId}:*`) to evict all workspaces on security events.

---

## 🛡️ Brute-Force & Integrity Protection

### `RateLimitService`
- **Scope**: Fixed-window rate limiting per IP + email key.
- **Key Schema**: `login:{ip}:{email}`
- **Behavior**: Atomically increments attempts in Redis. Enforces TTL at initial creation to prevent permanent lockout conditions.

### `EmailVerificationServiceImpl`
- **Onboarding Flow**: Registration data is stored in Redis under `temp-register:{token}` with a **15-minute TTL** instead of committing to the DB. A confirmation mail is sent.
- **Verification**: User clicks link, sending `{token, email}` to `/verify-email`. The backend performs a cross-validation:
  ```java
  if (!storedEmail.equalsIgnoreCase(email)) {
      throw new EmailVerificationTokenInvalidException("Token-email mismatch");
  }
  ```
  Only on match is the user and workspace persisted to the DB.

### `PasswordResetServiceImpl`
- **Flow**: Generates recovery token, stores email in Redis under `password:reset:{token}` with a **15-minute TTL**.
- **Timing Attack Mitigation**: When `/forgot-password` is called for a non-existent email, the service simulates a standard cryptographic hashing and email-sending delay (200ms sleep) before returning, preventing database email enumeration via timing profiles.
