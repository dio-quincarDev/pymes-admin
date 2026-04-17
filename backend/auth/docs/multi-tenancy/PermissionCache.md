## Documentation: Permission Cache Service

### Technical Overview
* **Component:** `PermissionCacheService`
* **Infrastructure:** Redis (Remote Dictionary Server)
* **Responsibility:** High-performance caching of Authorization data to reduce database load during request interceptors.
* **Mechanism:** Key-Value pair storage with time-based expiration and pattern-based invalidation.

---

### Cache Strategy

The service implements a **Look-aside Cache** pattern to store user permissions, scoped by both identity and organizational context (Multi-tenancy).

#### 1. Key Structure
Keys are constructed using a hierarchical prefix to allow for targeted invalidation:
* **Format:** `auth:permissions:{userId}:{tenantId}`
* **Purpose:** Ensures that a user's permissions in one tenant do not leak into another, while allowing group operations via patterns.

#### 2. Lifecycle Management
* **TTL (Time To Live):** Hardcoded at **5 minutes**. This short duration ensures eventual consistency if the database is modified without triggering an invalidation event.
* **Storage Type:** Serialized `List<String>` stored via `RedisTemplate`.

---

### Invalidation Logic

The service provides two levels of cache purging to maintain data integrity:

1.  **Granular Invalidation:** `invalidatePermissions(userId, tenantId)` removes the specific key for a single workspace. This is used when a user's role is updated within one specific tenant.
2.  **Global Invalidation:** `invalidateAllUserPermissions(userId)` uses Redis key pattern matching (`auth:permissions:{userId}:*`). This is critical for security events, such as a global password change or account suspension, requiring a total wipe of the user's access rights across the entire platform.

---

### Technical Specifications

#### Operational Matrix

| Operation | Implementation | Redis Command | Complexity |
| :--- | :--- | :--- | :--- |
| **Write** | `opsForValue().set()` | `SETEX` | O(1) |
| **Read** | `opsForValue().get()` | `GET` | O(1) |
| **Delete (Single)** | `delete(key)` | `DEL` | O(1) |
| **Delete (Pattern)** | `keys(pattern)` + `delete(keys)` | `KEYS` + `DEL` | O(N) |

---

### Implementation Details

#### Type Safety & Normalization
The `getCachedPermissions` method includes a defensive normalization layer:
* It checks if the retrieved object is an instance of `List`.
* It explicitly maps elements to `String` via a Stream API to prevent `ClassCastException` during downstream consumption.

#### Logging & Debugging
The service utilizes `@Slf4j` for debug-level tracing. It records:
* Cache hits/misses (implicitly through flow).
* Successful cache population.
* Invalidation events, including the count of keys removed during pattern-based purges.

---

### Critical Configuration
* **Prefix:** `auth:permissions:`
* **Default TTL:** `300 seconds` (5 min).
* **Dependency:** Requires a configured `RedisTemplate<String, Object>` with appropriate serializers (typically `StringRedisSerializer` for keys and `JdkSerializationRedisSerializer` or `Jackson2JsonRedisSerializer` for values).