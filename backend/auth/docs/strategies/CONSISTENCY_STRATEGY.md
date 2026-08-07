# API Route Consistency Architecture

This document describes the unified routing design across the `auth` microservice controllers, security filters, and integration tests.

---

## 🎯 Architecture Design

To prevent mismatches between controller mappings, security filter whitelists, and integration test requests, all API routes are defined using centralized constants in `ApiPathConstants.java`.

### Route Mapping Matrix

| Controller | Base Path | Endpoints | Authentication |
|------------|-----------|-----------|----------------|
| **AuthApi** | `/api/v1/auth` | `POST /register`<br>`POST /login`<br>`POST /refresh`<br>`POST /verify-email`<br>`POST /resend-verification`<br>`POST /forgot-password`<br>`POST /reset-password`<br>`POST /oauth2/intent`<br>`GET /oauth2/intent/{intentId}`<br>`POST /exchange` | Public |
| **AuthApi** | `/api/v1/auth` | `POST /logout` | Required |
| **UserApi** | `/api/v1/users` | `GET /me` | Required |
| **TenantApi**| `/api/v1/tenants`| `GET /`<br>`POST /`<br>`POST /select` | Required |
| **MemberApi**| `/api/v1/tenants/{tenantId}/members` | `GET /`<br>`PUT /{userId}/role`<br>`DELETE /{userId}` | Required |
| **InvitationApi**| `/api/v1/invitations` | `GET /`<br>`POST /`<br>`POST /accept`<br>`DELETE /{invitationId}` | Required |

---

## 🔧 Unified Components

### 1. `ApiPathConstants.java`
Stores the version prefix, base route segments, and full path concatenation:
```java
public class ApiPathConstants {
    public static final String V1_ROUTE = "/api/v1";
    public static final String AUTH_ROUTE = "/auth";
    // Sub-paths
    public static final String AUTH_REGISTER = "/register";
    // Full paths
    public static final String FULL_AUTH_REGISTER = V1_ROUTE + AUTH_ROUTE + AUTH_REGISTER;
}
```

### 2. `SecurityConfig.java` Whitelist
The security filter chain reads the full path constants directly, preventing typos from breaking public access:
```java
private static final String[] WHITE_LIST = {
    // Swagger / OpenAPI
    "/v3/api-docs/**",
    "/swagger-ui/**",
    // Actuator
    "/actuator/**",
    // OAuth2 login endpoint
    "/oauth2/**",
    "/login/**",
    // Error page
    "/error",
    // Public auth endpoints
    ApiPathConstants.FULL_AUTH_REGISTER,
    ApiPathConstants.FULL_AUTH_LOGIN,
    ApiPathConstants.FULL_AUTH_REFRESH,
    ApiPathConstants.FULL_AUTH_VERIFY_EMAIL,
    ApiPathConstants.FULL_AUTH_RESEND_VERIFICATION,
    // Password recovery
    ApiPathConstants.FULL_AUTH_FORGOT_PASSWORD,
    ApiPathConstants.FULL_AUTH_RESET_PASSWORD,
    // OAuth2 intent and code exchange
    ApiPathConstants.FULL_AUTH_OAUTH2_INTENT,
    ApiPathConstants.V1_ROUTE + ApiPathConstants.AUTH_ROUTE + "/oauth2/intent/**",
    ApiPathConstants.V1_ROUTE + ApiPathConstants.AUTH_ROUTE + "/exchange"
};
```

---

## 🧪 Consistency Verification (Automated)

To ensure compile-time and runtime safety, the test suite includes `ApiPathConsistencyTest.java`. This test uses the **Reflections** library to scan all controller classes at runtime and verify:
1. Every mapping annotation matches a path declared in `ApiPathConstants`.
2. No hardcoded strings are used in mapping definitions.
3. Whitelisted paths match actual endpoints.
