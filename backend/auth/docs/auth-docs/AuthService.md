## Documentation: Authentication Service

### Technical Overview
* **Component:** `AuthServiceImpl`
* **Responsibilities:** User lifecycle management, Multi-tenancy provisioning, and Security auditing.
* **Architecture:** Atomic transactional operations with integrated Rate Limiting and Audit logging.

---

### Core Workflows

#### 1. Registration & Provisioning
Atomic process that ensures data consistency between identity and workspace:
* **Validation:** Checks for existing email and company slug uniqueness.
* **User Creation:** Persists `UserEntity` with encoded credentials and `AuthProvider.LOCAL`.
* **Tenant Provisioning:** Automates workspace creation with `PlanName.FREE` and default resource limits.
* **Relationship:** Establishes `UserTenant` link with `OWNER` role.
* **Identity Verification:** Triggers `EmailVerificationService` before permitting full access.

#### 2. Authentication & Security
Multi-layered login validation pipeline:
* **Rate Limiting:** Prevents brute-force attacks via `RateLimitService` (IP + Email key).
* **State Validation:** Verifies `isActive` and `isEmailVerified` status before authentication.
* **Identity Context:** Resolves the active `tenantId`, `role`, and `plan` to embed into the initial JWT.
* **Audit Trail:** Captures `X-Forwarded-For` / `X-Real-IP` and `User-Agent` for every session start.

#### 3. Token Life Cycle & Rotation
* **Logout:** Proactive revocation via `jwtService.revokeToken`.
* **Refresh Strategy:** Implements **Rotation**. The previous Refresh Token is invalidated upon exchange for a new pair.
* **Tenant Fallback:** If a stored `tenantId` in the token is no longer valid, the service automatically re-syncs with the first available active `UserTenant`.

---

### Data Models & Mapping

| Entity | Mapping Responsibility |
| :--- | :--- |
| **UserEntity** | Identity, status, and provider data. |
| **Tenant** | Workspace metadata, plan tier, and capacity. |
| **UserTenant** | Authorization bridge (Role-Based Access Control). |
| **AuditLog** | Immutable record of authentication events. |

---

### Technical Security Specifications

#### IP Extraction Logic
To ensure accurate auditing behind proxies (OCI Load Balancers/NGINX):
1.  Check `X-Forwarded-For` (first entry).
2.  Fallback to `X-Real-IP`.
3.  Final fallback to `getRemoteAddr()`.

#### Transaction Management
The `@Transactional` annotation is strictly applied to `register`, `login`, and `refreshToken` to prevent partial data persistence (e.g., creating a User without their Tenant).

#### Audit Schema
Logs are stored in `audit_log_repository` with the following context:
* `action`: REGISTER, LOGIN.
* `resource`: Fixed as "AUTH".
* `metadata`: IP Address and User-Agent.

---

### Exception Handling Matrix
* `DuplicateResourceException`: Conflict in Email or Slug.
* `InvalidInputException`: Rate limit exceeded.
* `AuthenticationException`: Invalid credentials.
* `AuthorizationException`: Account inactive or email pending verification.