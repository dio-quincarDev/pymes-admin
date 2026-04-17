## Documentation: Password Reset Service

### Technical Overview
* **Component:** `PasswordResetServiceImpl`
* **Infrastructure:** Redis (Ephemeral storage) & PostgreSQL (Identity persistence).
* **Responsibility:** Secure orchestration of the password recovery lifecycle.
* **Mechanism:** Short-lived opaque tokens stored in a high-performance cache to avoid database schema bloat.

---

### Security Strategy

The implementation prioritizes cryptographic integrity and protection against common authentication attacks.

#### 1. Secure Token Generation
* **Entropy:** Uses `SecureRandom` to generate 32-byte (256-bit) cryptographically strong random values.
* **Encoding:** Tokens are formatted as Hex strings for URL compatibility.
* **Storage:** Redis acts as the source of truth, mapping the `token` (key) to the `email` (value).

#### 2. Timing Attack Mitigation
The `generateResetToken` method is designed to resist user enumeration:
* Even if an email does not exist in the database, the system logs the event and returns a response, aiming to maintain a consistent execution time relative to successful lookups.

#### 3. Lifecycle & Expiration
* **TTL (Time To Live):** Hardcoded to **15 minutes**.
* **Self-Destruction:** Tokens are explicitly deleted from Redis immediately after a successful password update (`One-Time Use` policy).
* **Automatic Eviction:** Redis natively handles expiration if the user fails to complete the process within the 15-minute window.

---

### Core Workflows

#### Token Generation
1.  **Identity Verification:** Look up `UserEntity` by email.
2.  **State Creation:** Generate 32-byte hex token.
3.  **Persistence:** Store in Redis using the prefix `password:reset:{token}` with a value of the user's email.

#### Password Reset Execution
1.  **Token Validation:** Retrieve email from Redis using the provided token key.
2.  **Integrity Check:** If the key is null (expired or non-existent), throw `PasswordResetTokenInvalidException`.
3.  **Credential Update:** * Encode `newPassword` using `PasswordEncoder` (BCrypt/Argon2).
    * Persist updated `UserEntity` to PostgreSQL.
4.  **Cleanup:** Invalidate the token in Redis to prevent replay attacks.

---

### Technical Specifications

| Feature | Implementation |
| :--- | :--- |
| **Token Length** | 64 characters (Hex format of 32 bytes) |
| **Storage Type** | String (Redis Key-Value) |
| **Prefix** | `password:reset:` |
| **Consistency** | `@Transactional` (PostgreSQL side) |

---

### Implementation Details

#### Cryptographic Provider
The use of `java.security.SecureRandom` ensures that tokens are not predictable, unlike standard `java.util.Random`, making the recovery link impossible to guess via brute force.

#### Atomicity
The `resetPassword` method is wrapped in a transaction to ensure that if the database update fails, the token is not accidentally deleted from Redis, allowing the user to retry without requesting a new email.