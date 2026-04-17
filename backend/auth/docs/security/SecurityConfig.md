## Documentation: Security Configuration

### Technical Overview
* **Component:** `SecurityConfig`
* **Framework:** Spring Security 6.x
* **Pattern:** Stateless Security with JWT & OAuth2 Federation.
* **Responsibility:** Centralizing the security filter chain, authorization rules, and identity providers.

---

### Security Architecture

The implementation configures a non-blocking, stateless security perimeter tailored for microservices.

#### 1. Security Filter Chain
* **CSRF:** Disabled (Stateless architecture using JWT).
* **Session Policy:** `STATELESS` (No JSESSIONID).
* **CORS:** Integrated via a custom `corsConfigurationSource` to handle cross-origin resource sharing for frontend applications.

#### 2. Authentication Providers
* **Local Auth:** Utilizes `BCryptPasswordEncoder` and a custom `UserDetailsService` that resolves identities from the `userRepository`.
* **OAuth2 Login:** Configured with a `customOAuth2UserService` for JIT provisioning and a specialized `successHandler` to bridge external authentication with internal JWT issuance.

---

### Access Control Matrix

#### Public Routes (White List)
Access is permitted without authentication for system-critical and onboarding endpoints:
* **Documentation:** Swagger/OpenAPI UI and docs.
* **Monitoring:** Spring Boot Actuator endpoints.
* **Auth Lifecycle:** Registration, Login, Token Refresh, Email Verification, and Password Recovery.
* **OAuth2:** Native login endpoints.

#### Protected Routes
* **Policy:** `anyRequest().authenticated()`
* **Mechanism:** All other requests must provide a valid Bearer token processed by the `jwtAuthenticationFilter` before reaching the `UsernamePasswordAuthenticationFilter`.

---

### Exception & Error Handling

The configuration overrides default Spring Security behaviors to return standardized JSON responses via the `ObjectMapper`.

| Incident | HTTP Status | Internal Error Code |
| :--- | :--- | :--- |
| **Authentication Failure** | 401 Unauthorized | `UNAUTHORIZED_ACCESS` |
| **Authorization Failure** | 403 Forbidden | `INSUFFICIENT_PERMISSIONS` |

#### Standardized JSON Error Payload
```json
{
  "codigo": "ERROR_CODE",
  "mensaje": "Human readable message",
  "path": "/api/resource",
  "timestamp": "ISO-8601 String",
  "detalles": {}
}
```

---

### Technical Specifications

#### Bean Definitions
* **AuthenticationManager:** Exported from `AuthenticationConfiguration` to be used in the `AuthServiceImpl`.
* **PasswordEncoder:** Standardizes on `BCrypt` for hashing local credentials.
* **UserDetailsService:** Bridges Spring Security with the PostgreSQL identity store.

#### Filter Ordering
The `jwtAuthenticationFilter` is explicitly placed **before** the `UsernamePasswordAuthenticationFilter`. This ensures that the SecurityContext is populated from the JWT claims before the standard authentication mechanisms attempt to process the request.

---

### Security Metadata
* **EnableMethodSecurity:** Enabled to allow granular authorization using `@PreAuthorize` or `@Secured` annotations at the service layer.
* **Actuator Protection:** While in the White List, access should be restricted via infrastructure (Cloud Firewall/WAF) or further refined with roles in production.