## Documentation: JWT Security Engine

### Technical Overview
* **Core Library:** JJWT 0.12.6
* **Signature Algorithm:** HS256 (HMAC-SHA256)
* **Token Storage Strategy:** * **Access Tokens:** Stateless, with Redis-backed revocation (Blacklist).
    * **Refresh Tokens:** Stateful, stored in Database via SHA-256 hashing.

---

### Token Payload Structure (Claims)
The implementation injects specific identity and context claims to facilitate authorization at the Gateway level without database lookups:

* **sub**: User principal (Email).
* **userId**: Unique user identifier (UUID).
* **tenantId**: Tenant context for multi-tenant isolation (UUID).
* **role**: Assigned authorization authority.
* **plan**: Service tier (e.g., FREE, PREMIUM).
* **jti**: Unique JWT ID for tracking.

---

### Security Logic & Mechanisms

#### 1. Refresh Token Rotation (RTR)
The system implements a strict rotation policy. Every time a Refresh Token is used to generate a new Access Token, the old Refresh Token is marked as revoked.

#### 2. Reuse Detection (Anti-Replay)
A critical security feature within `validateAndRevokeRefreshToken`:
* If a Refresh Token is presented and its database status is already `revoked=true`, the system interprets this as a compromise.
* **Action:** Immediate deletion of the entire token family for that `userId`, forcing a full re-authentication across all devices.

#### 3. Token Blacklisting
Immediate revocation of Access Tokens is handled via `TokenBlacklistService`:
* Tokens are cached in Redis for the remainder of their TTL (`accessTokenExpiration`).
* Validation logic checks the blacklist before verifying standard JWT claims.

---

### Implementation Methods

#### Token Generation
* `generateAccessToken`: Includes full identity, tenant, and plan claims.
* `generateRefreshToken`: Minimal payload (userId only) to optimize storage and security.

#### Validation Pipeline
1.  **Integrity Check:** Signature verification and malformed structure detection.
2.  **Expiration Check:** Standard TTL validation.
3.  **Persistence Check:** Verification against the Redis Blacklist or Database revocation status.

---

### Configuration Requirements
```yaml
jwt:
  secret: ${JWT_SECRET}
  access-expiration: [Long]
  refresh-expiration: [Long]
```

### Data Integrity
All tokens stored in the database are hashed using **SHA-256**. This ensures that even in the event of a database breach, raw tokens cannot be retrieved or reused to impersonate users.