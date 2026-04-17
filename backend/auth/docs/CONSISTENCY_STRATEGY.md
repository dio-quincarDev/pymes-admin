# Strategy: API Path Consistency

## Problem Statement

Currently there are inconsistencies between:
1. **Controllers** (interface definitions with `@RequestMapping` and `@*Mapping`)
2. **ApiPathConstants** (centralized constants)
3. **SecurityConfig** (whitelist for public endpoints)
4. **Integration Tests** (hardcoded paths in tests)

## Current State Analysis

### Controllers (Interfaces)
| Controller | Base Path | Endpoints | Requires Auth? |
|---|---|---|---|
| `AuthApi` | `/api/v1/auth` | `POST /register`<br>`POST /login`<br>`POST /logout`<br>`POST /refresh`<br>`POST /verify-email`<br>`POST /resend-verification` | ❌ Public (except logout) |
| `UserApi` | `/api/v1/users` | `GET /me` | ✅ Yes |
| `TenantApi` | `/api/v1/tenants` | `GET /`<br>`POST /select`<br>`POST /` | ✅ Yes |
| `MemberApi` | `/api/v1/tenants/{tenantId}/members` | `GET /`<br>`PUT /{userId}/role`<br>`DELETE /{userId}` | ✅ Yes |
| `InvitationApi` | `/api/v1/invitations` | `GET /`<br>`POST /`<br>`POST /accept`<br>`DELETE /{invitationId}` | ✅ Yes |

### SecurityConfig Whitelist
```java
✅ /api/v1/auth/register
✅ /api/v1/auth/login
✅ /api/v1/auth/refresh
✅ /api/v1/auth/verify-email
✅ /api/v1/auth/resend-verification
❌ /api/v1/auth/logout (MISSING - should be authenticated)
```

### Integration Tests
- Tests use **hardcoded paths** like `/api/v1/auth/login` instead of constants
- This creates risk of tests passing but production code using wrong paths

## Proposed Strategy

### Phase 1: Centralize All Path Constants

**Update `ApiPathConstants.java`** to include:
```java
public class ApiPathConstants {
    // Version prefix
    public static final String V1_ROUTE = "/api/v1";
    
    // Base routes
    public static final String AUTH_ROUTE = "/auth";
    public static final String USERS_ROUTE = "/users";
    public static final String TENANTS_ROUTE = "/tenants";
    public static final String MEMBERS_ROUTE = "/members";
    public static final String INVITATIONS_ROUTE = "/invitations";
    
    // Auth sub-paths
    public static final String AUTH_REGISTER = "/register";
    public static final String AUTH_LOGIN = "/login";
    public static final String AUTH_LOGOUT = "/logout";
    public static final String AUTH_REFRESH = "/refresh";
    public static final String AUTH_VERIFY_EMAIL = "/verify-email";
    public static final String AUTH_RESEND_VERIFICATION = "/resend-verification";
    
    // User sub-paths
    public static final String USERS_ME = "/me";
    
    // Tenant sub-paths
    public static final String TENANTS_SELECT = "/select";
    
    // Invitation sub-paths
    public static final String INVITATIONS_ACCEPT = "/accept";
    
    // Full paths for security config
    public static final String FULL_AUTH_REGISTER = V1_ROUTE + AUTH_ROUTE + AUTH_REGISTER;
    public static final String FULL_AUTH_LOGIN = V1_ROUTE + AUTH_ROUTE + AUTH_LOGIN;
    public static final String FULL_AUTH_LOGOUT = V1_ROUTE + AUTH_ROUTE + AUTH_LOGOUT;
    public static final String FULL_AUTH_REFRESH = V1_ROUTE + AUTH_ROUTE + AUTH_REFRESH;
    public static final String FULL_AUTH_VERIFY_EMAIL = V1_ROUTE + AUTH_ROUTE + AUTH_VERIFY_EMAIL;
    public static final String FULL_AUTH_RESEND_VERIFICATION = V1_ROUTE + AUTH_ROUTE + AUTH_RESEND_VERIFICATION;
}
```

### Phase 2: Update Controllers to Use Constants

**AuthApi.java:**
```java
@RequestMapping(ApiPathConstants.V1_ROUTE + ApiPathConstants.AUTH_ROUTE)

@PostMapping(ApiPathConstants.AUTH_REGISTER)
@PostMapping(ApiPathConstants.AUTH_LOGIN)
@PostMapping(ApiPathConstants.AUTH_LOGOUT)
@PostMapping(ApiPathConstants.AUTH_REFRESH)
@PostMapping(ApiPathConstants.AUTH_VERIFY_EMAIL)
@PostMapping(ApiPathConstants.AUTH_RESEND_VERIFICATION)
```

**UserApi.java:**
```java
@RequestMapping(ApiPathConstants.V1_ROUTE + ApiPathConstants.USERS_ROUTE)

@GetMapping(ApiPathConstants.USERS_ME)
```

**TenantApi.java:**
```java
@RequestMapping(ApiPathConstants.V1_ROUTE + ApiPathConstants.TENANTS_ROUTE)

@PostMapping(ApiPathConstants.TENANTS_SELECT)
```

**InvitationApi.java:**
```java
@RequestMapping(ApiPathConstants.V1_ROUTE + ApiPathConstants.INVITATIONS_ROUTE)

@PostMapping(ApiPathConstants.INVITATIONS_ACCEPT)
```

### Phase 3: Update SecurityConfig to Use Full Path Constants

**Replace hardcoded paths:**
```java
private static final String[] WHITE_LIST = {
    "/v3/api-docs/**",
    "/swagger-ui/**",
    "/swagger-ui.html",
    "/actuator/**",
    "/login/**",
    "/error",
    ApiPathConstants.V1_ROUTE + "/actuator/**",
    ApiPathConstants.FULL_AUTH_REGISTER,
    ApiPathConstants.FULL_AUTH_LOGIN,
    ApiPathConstants.FULL_AUTH_REFRESH,
    ApiPathConstants.FULL_AUTH_VERIFY_EMAIL,
    ApiPathConstants.FULL_AUTH_RESEND_VERIFICATION
    // Note: logout REQUIRES authentication (not in whitelist)
};
```

### Phase 4: Create Test Path Constants Helper

**Create `TestApiPaths.java` in test scope:**
```java
package auth.pymes.testutil;

import auth.pymes.common.constants.ApiPathConstants;

/**
 * Test utility class that mirrors production API paths.
 * Ensures tests use the same paths as production code.
 */
public final class TestApiPaths {
    private TestApiPaths() {}
    
    // Auth endpoints (mirror production constants)
    public static final String AUTH = ApiPathConstants.V1_ROUTE + ApiPathConstants.AUTH_ROUTE;
    public static final String AUTH_REGISTER = AUTH + ApiPathConstants.AUTH_REGISTER;
    public static final String AUTH_LOGIN = AUTH + ApiPathConstants.AUTH_LOGIN;
    public static final String AUTH_LOGOUT = AUTH + ApiPathConstants.AUTH_LOGOUT;
    public static final String AUTH_REFRESH = AUTH + ApiPathConstants.AUTH_REFRESH;
    public static final String AUTH_VERIFY_EMAIL = AUTH + ApiPathConstants.AUTH_VERIFY_EMAIL;
    public static final String AUTH_RESEND_VERIFICATION = AUTH + ApiPathConstants.AUTH_RESEND_VERIFICATION;
    
    // User endpoints
    public static final String USERS = ApiPathConstants.V1_ROUTE + ApiPathConstants.USERS_ROUTE;
    public static final String USERS_ME = USERS + ApiPathConstants.USERS_ME;
    
    // Tenant endpoints
    public static final String TENANTS = ApiPathConstants.V1_ROUTE + ApiPathConstants.TENANTS_ROUTE;
    public static final String TENANTS_SELECT = TENANTS + ApiPathConstants.TENANTS_SELECT;
    
    // Member endpoints
    public static final String MEMBERS = ApiPathConstants.V1_ROUTE + ApiPathConstants.TENANTS_ROUTE + "/{tenantId}" + ApiPathConstants.MEMBERS_ROUTE;
    
    // Invitation endpoints
    public static final String INVITATIONS = ApiPathConstants.V1_ROUTE + ApiPathConstants.INVITATIONS_ROUTE;
    public static final String INVITATIONS_ACCEPT = INVITATIONS + ApiPathConstants.INVITATIONS_ACCEPT;
}
```

**Update integration tests to use constants:**
```java
// Before:
mockMvc.perform(post("/api/v1/auth/register")

// After:
mockMvc.perform(post(TestApiPaths.AUTH_REGISTER)
```

### Phase 5: Add Automated Consistency Check

**Create a unit test that validates consistency:**
```java
@DisplayName("API Path Consistency Tests")
class ApiPathConsistencyTest {

    @Test
    @DisplayName("All controller paths should use constants")
    void validateControllerPathsUseConstants() {
        // Use reflection to scan all controller interfaces
        // Verify all @RequestMapping and @*Mapping annotations use constants
        // Fail if any hardcoded strings found
    }

    @Test
    @DisplayName("All whitelist paths should match controller paths")
    void validateWhitelistMatchesControllers() {
        // Extract all public endpoints from controllers
        // Verify they exist in SecurityConfig whitelist
    }

    @Test
    @DisplayName("Test paths should match production paths")
    void validateTestPathsMatchProduction() {
        // Compare TestApiPaths constants with ApiPathConstants
        // Ensure they're identical
    }
}
```

## Implementation Order

1. ✅ **Step 1**: Update `ApiPathConstants.java` (add sub-paths and full paths)
2. ✅ **Step 2**: Update all controller interfaces to use constants
3. ✅ **Step 3**: Update `SecurityConfig.java` whitelist to use full path constants
4. ✅ **Step 4**: Create `TestApiPaths.java` test utility
5. ✅ **Step 5**: Update integration tests to use test constants
6. ✅ **Step 6**: Add consistency validation test
7. ✅ **Step 7**: Run all tests to ensure nothing breaks

## Benefits

- **Single Source of Truth**: All API paths defined once in `ApiPathConstants`
- **Type Safety**: Compile-time checking prevents typos
- **Easy Refactoring**: Change a path in one place
- **Test Reliability**: Tests use same constants as production
- **Documentation**: Constants serve as API documentation
- **Automated Validation**: Consistency tests catch future deviations

## Risk Mitigation

- Run full test suite after each step
- No functional changes, only refactoring
- Backwards compatible (same string values)
- Can rollback easily (git commit per step)
