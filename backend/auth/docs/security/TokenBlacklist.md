## Documentation: Token Blacklist Service

### Technical Overview
* **Component:** `TokenBlacklistService`
* **Infrastructure:** Redis (In-memory Data Store)
* **Responsibility:** Real-time invalidation of Access Tokens (Logout) prior to their natural expiration.
* **Mechanism:** Temporary key-value storage with automatic TTL-based eviction.

---

### Revocation Strategy

The service provides a mechanism to handle the "Stateless Invalidation" problem of JWTs. When a user logs out, the server loses control over the client-side token; this service restores that control by tracking revoked signatures.

#### 1. Blacklist Lifecycle
* **Storage Pattern:** `SET` operation with a prefix and the raw token as part of the key.
* **Value:** A static `revoked` flag.
* **Expiration (TTL):** Every entry is stored with a time-to-live equal to the remaining life of the token. This ensures Redis memory is reclaimed automatically exactly when the token would have expired naturally.

#### 2. Pattern & Key Structure
* **Prefix:** `auth:token_blacklist:`
* **Full Key:** `auth:token_blacklist:{JWT_STRING}`

---

### Core Operations

#### Token Revocation (`revokeToken`)
This is the primary method called during the Logout flow.
* It requires the `expirationSeconds` parameter, typically derived from the token's remaining TTL.
* Uses `opsForValue().set()` with `TimeUnit.SECONDS` to guarantee self-cleanup.

#### Validation Interception (`isTokenRevoked`)
This check is integrated into the JWT Validation Filter.
* **Operation:** `hasKey(key)`.
* **Impact:** Provides O(1) lookup time, ensuring minimal latency penalty during the request filter chain.

---

### Technical Specifications

| Feature | Detail |
| :--- | :--- |
| **Data Structure** | String (Redis Key-Value) |
| **Complexity** | O(1) for all main operations |
| **Auto-Cleanup** | Managed by Redis Native TTL |
| **Persistence** | Volatile (In-memory only by default) |

---

### Implementation Safeguards

#### Automatic Maintenance
The `cleanupExpiredTokens` method serves as a placeholder for observability. Since Redis handles the removal of expired keys natively, the system avoids manual overhead or scheduled "purge" queries that could impact performance.

#### Reliability
The `isTokenRevoked` method includes null-safety checks for the `Boolean` return type of the Redis driver, ensuring that connectivity issues or missing keys do not trigger false positives in the authentication logic.

#### Use Case: Re-Login
The `removeFromBlacklist` method allows for edge-case scenarios where a token might need to be "un-revoked" (e.g., specific administrative overrides or immediate re-authentication flows), providing operational flexibility.