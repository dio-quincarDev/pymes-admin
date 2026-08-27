# Local Authentication & SaaS Multi-Tenancy Architecture

This document outlines the core architecture for user identity, role-based authorization, and tenant isolation.

---

## 1. Multi-Tenant Data Model

### `UserEntity` (Global Identity)
- **BCrypt Credentials**: The `password` field holds BCrypt hashes.
- **Provider Support**: Tracks provider types (`LOCAL`, `GOOGLE`) and `providerId` to support hybrid authentication profiles.
- **Audit Logs**: Deletion tracking via logical soft delete.

### `UserTenant` (Workspace Mapping)
- **Role Hierarchy**: Power weighting: `OWNER (4) > ADMIN (3) > CONTABLE (2) > VIEWER (1)`.
- **Soft Delete**: Uses Hibernate `@SQLDelete` and `@Where` annotations to manage user-tenant disassociations.

---

## 2. Onboarding & Workspace Provisioning

The onboarding flow performs an atomic database registration:
1. Validates uniqueness of user email and company slug.
2. Persists the `UserEntity` profile.
3. Provisions a new `Tenant` workspace defaulted to the **FREE Plan** (limitations: `maxUsers = 1`).
4. Creates a `UserTenant` association mapping the user as the workspace **`OWNER`**.
5. Inserts an initial audit log record mapping the transaction.

---

## 3. JWT & Context Claims

The issued JWT acts as a stateless access pass across microservices.
- **Claims**: `userId`, `email`, `tenantId`, `role`, `plan`, and `jti` (unique UUID).
- **Active Workspace Selection**: The user selects a tenant. If verified, the auth service generates a new JWT mapping the `tenantId`, `role`, and `plan` parameters.

---

## 4. Role Hierarchy & B2B Authorization

Authorization constraints are validated at two checkpoints:
- **Perimeter (Controller)**: Enforced via Method Security (`@PreAuthorize("hasRole(...)")`).
- **Domain Logic (Service)**: Enforces business hierarchical boundaries:
  - An `ADMIN` cannot modify or delete an `OWNER`.
  - An `OWNER` cannot leave or delete their profile without transferring ownership first.

---

## 5. Token Management (RTR)

To protect stateless API sessions, the service implements **Refresh Token Rotation (RTR)**:
1. Client submits a request to refresh the token using their `refreshToken`.
2. The service executes an atomic database check:
   - Validates signature and TTL.
   - Searches Postgres for the token hash.
   - **Reuse Detection**: If the database shows the token was already used (`revoked = true`), it triggers an alarm and invalidates the entire token family of that user (full re-auth).
3. If valid, the old token is marked `revoked` and a new token pair (Access + Refresh) is returned.

---

## 6. Redis Blacklist & Fail-Open Strategy

### Token Blacklist

When a user logs out, the access token is added to a Redis blacklist with a TTL matching the token expiration (`accessTokenExpiration` in milliseconds). The `TokenBlacklistService` stores keys under `auth:token_blacklist:{token}` and uses `TimeUnit.MILLISECONDS` for Redis TTL.

### Fail-Open on Redis Failure

If Redis is unavailable (connection timeout, memory pressure, network partition):
- **Blacklist check** (`isTokenRevoked`): returns `false` — the token is treated as valid. This prevents a Redis outage from locking out all authenticated users.
- **Logout** (`revokeToken`): logs a warning and continues. Refresh tokens are still deleted from PostgreSQL (separate try-catch). The access token may remain usable until natural JWT expiration.
- **Token revocation** (`revokeToken` on logout): best-effort. If Redis is down, the token is not blacklisted but the user's refresh tokens are still deleted.

**Trade-off**: A revoked access token could theoretically be used for up to `accessTokenExpiration` (1 hour) if Redis is down during logout. This is acceptable because:
1. The attacker needs the token before it's revoked
2. Refresh tokens are deleted from DB regardless
3. The alternative (fail-closed) locks out ALL users when Redis is down
