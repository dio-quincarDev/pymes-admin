## Documentation: Tenant Service

### Technical Overview
* **Component:** `TenantServiceImpl`
* **Responsibilities:** Multi-tenant workspace management, contextual session switching, and resource limit enforcement.
* **Architecture:** Bridge pattern between Identity (`UserEntity`) and Workspace (`Tenant`) via the `UserTenant` join entity.

---

### Core Functionalities

#### 1. Workspace Discovery
* **Mechanism:** Paginated retrieval of all active tenants associated with a user.
* **Identity Context:** Resolves the user via the `OAuth2User` principal (Email extraction).
* **Data Mapping:** Transforms internal `UserTenant` entities into `UserTenantResponse` DTOs, including role details and invitation status (`acceptedAt`).

#### 2. Contextual Tenant Switching (`selectTenant`)
Implements the logic to "log in" to a specific workspace:
* **Authorization Check:** Verifies that the user has an active membership in the requested `tenantId`.
* **State Verification:** Ensures both the user-tenant relationship and the tenant itself are `isActive`.
* **Token Issuance:** Generates a new JWT pair (Access + Refresh) containing the new `tenantId`, `role`, and `plan` claims.
* **Persistence:** Rotates the Refresh Token in the database to reflect the new active session context.

#### 3. Tenant Provisioning & Limits
* **Validation:** Enforces a "Single Free Tenant" policy by counting existing `OWNER` roles for the user.
* **Constraint:** Prevents duplicate `slug` identifiers to ensure unique routing and indexing.
* **Defaults:** New tenants are provisioned with `PlanName.FREE`, a limit of 1 user, and immediate `OWNER` status for the creator.

---

### Logical Schema

| Action | Logic / Constraint | Result |
| :--- | :--- | :--- |
| **List Tenants** | Filter by `userId` AND `isActive == true`. | Paginated List. |
| **Select Tenant** | Validate relationship + Generate JWT. | Updated Auth Session. |
| **Create Tenant** | Check `freeTenantsCount < 1`. | New Workspace + Owner Role. |

---

### Technical Specifications

#### Multi-tenancy Guardrails
The service acts as the primary enforcement point for data isolation:
* **Role-Based:** Assigns the `OWNER` role during creation.
* **Status-Based:** Rejects access to disabled tenants even if the user membership is technically valid.

#### Transactional Integrity
`@Transactional` is applied to `selectTenant` and `createTenant` to ensure:
* Atomicity between tenant creation and role assignment.
* Consistent state update during token rotation and audit logging.

#### Performance & Scalability
* **Pagination:** Uses `Pageable` for workspace listing to handle users with a high number of memberships.
* **Indexing:** Relies on `existsBySlug` and `findByUserId` lookups, which assume underlying database indexes on these frequently queried fields.

---

### Error Handling Matrix
* `ResourceNotFoundException`: User or Tenant does not exist.
* `AuthorizationException`: User is not a member of the tenant or the tenant/user is inactive.
* `InvalidInputException`: Free plan limit (1 tenant) exceeded.
* `DuplicateResourceException`: Company slug already in use.