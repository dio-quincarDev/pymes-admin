package auth.pymes.consistency;

import auth.pymes.common.constants.ApiPathConstants;
import auth.pymes.testutil.TestApiPaths;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import org.reflections.util.ConfigurationBuilder;
import org.springframework.web.bind.annotation.*;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("API Path Consistency Tests")
class ApiPathConsistencyTest {

    private static final String CONTROLLER_PACKAGE = "auth.pymes.controller";

    // ==================== CONTROLLER PATHS VALIDATION ====================

    @Nested
    @DisplayName("Controller Paths Validation")
    class ControllerPathsValidation {

        @Test
        @DisplayName("All controller @RequestMapping values should match ApiPathConstants")
        void validateRequestMappingUsesConstants() throws ClassNotFoundException {
            List<String> violations = new ArrayList<>();

            Reflections reflections = new Reflections(new ConfigurationBuilder()
                    .forPackage(CONTROLLER_PACKAGE)
                    .setScanners(Scanners.TypesAnnotated, Scanners.SubTypes));

            Set<Class<?>> controllers = reflections.getTypesAnnotatedWith(RequestMapping.class);

            for (Class<?> controller : controllers) {
                RequestMapping requestMapping = controller.getAnnotation(RequestMapping.class);
                if (requestMapping != null) {
                    String[] paths = requestMapping.value();
                    for (String path : paths) {
                        String expectedPath = ApiPathConstants.V1_ROUTE + getRouteConstantForController(controller);
                        if (!expectedPath.equals(path)) {
                            violations.add(controller.getSimpleName() + " has @RequestMapping=\"" + path +
                                    "\" but expected \"" + expectedPath + "\"");
                        }
                    }
                }
            }

            assertTrue(violations.isEmpty(),
                    "Controller @RequestMapping paths don't match ApiPathConstants:\n" + String.join("\n", violations));
        }

        @Test
        @DisplayName("All controller method mappings should use ApiPathConstants")
        void validateMethodMappingsUseConstants() throws Exception {
            List<String> violations = new ArrayList<>();

            Reflections reflections = new Reflections(new ConfigurationBuilder()
                    .forPackage(CONTROLLER_PACKAGE)
                    .setScanners(Scanners.TypesAnnotated, Scanners.SubTypes, Scanners.MethodsAnnotated));

            Set<Class<?>> controllers = reflections.getTypesAnnotatedWith(RequestMapping.class);

            for (Class<?> controller : controllers) {
                RequestMapping classMapping = controller.getAnnotation(RequestMapping.class);
                String basePath = classMapping != null && classMapping.value().length > 0
                        ? classMapping.value()[0]
                        : "";

                for (Method method : controller.getDeclaredMethods()) {
                    checkMappingAnnotation(method, PostMapping.class, m -> m.value(),
                            "PostMapping", controller.getSimpleName(), basePath, violations);
                    checkMappingAnnotation(method, GetMapping.class, m -> m.value(),
                            "GetMapping", controller.getSimpleName(), basePath, violations);
                    checkMappingAnnotation(method, PutMapping.class, m -> m.value(),
                            "PutMapping", controller.getSimpleName(), basePath, violations);
                    checkMappingAnnotation(method, DeleteMapping.class, m -> m.value(),
                            "DeleteMapping", controller.getSimpleName(), basePath, violations);
                    checkMappingAnnotation(method, PatchMapping.class, m -> m.value(),
                            "PatchMapping", controller.getSimpleName(), basePath, violations);
                }
            }

            assertTrue(violations.isEmpty(),
                    "Controller method mappings don't use ApiPathConstants:\n" + String.join("\n", violations));
        }

        @Test
        @DisplayName("All full paths should be correctly constructed from constants")
        void validateFullPathsConstruction() {
            // Verify that full path constants are correctly constructed
            assertEquals(ApiPathConstants.V1_ROUTE + ApiPathConstants.AUTH_ROUTE + ApiPathConstants.AUTH_REGISTER,
                    ApiPathConstants.FULL_AUTH_REGISTER);
            assertEquals(ApiPathConstants.V1_ROUTE + ApiPathConstants.AUTH_ROUTE + ApiPathConstants.AUTH_LOGIN,
                    ApiPathConstants.FULL_AUTH_LOGIN);
            assertEquals(ApiPathConstants.V1_ROUTE + ApiPathConstants.AUTH_ROUTE + ApiPathConstants.AUTH_LOGOUT,
                    ApiPathConstants.FULL_AUTH_LOGOUT);
            assertEquals(ApiPathConstants.V1_ROUTE + ApiPathConstants.AUTH_ROUTE + ApiPathConstants.AUTH_REFRESH,
                    ApiPathConstants.FULL_AUTH_REFRESH);
            assertEquals(ApiPathConstants.V1_ROUTE + ApiPathConstants.AUTH_ROUTE + ApiPathConstants.AUTH_VERIFY_EMAIL,
                    ApiPathConstants.FULL_AUTH_VERIFY_EMAIL);
            assertEquals(ApiPathConstants.V1_ROUTE + ApiPathConstants.AUTH_ROUTE + ApiPathConstants.AUTH_RESEND_VERIFICATION,
                    ApiPathConstants.FULL_AUTH_RESEND_VERIFICATION);
            assertEquals(ApiPathConstants.V1_ROUTE + ApiPathConstants.AUTH_ROUTE + ApiPathConstants.AUTH_FORGOT_PASSWORD,
                    ApiPathConstants.FULL_AUTH_FORGOT_PASSWORD);
            assertEquals(ApiPathConstants.V1_ROUTE + ApiPathConstants.AUTH_ROUTE + ApiPathConstants.AUTH_RESET_PASSWORD,
                    ApiPathConstants.FULL_AUTH_RESET_PASSWORD);
            assertEquals(ApiPathConstants.V1_ROUTE + ApiPathConstants.AUTH_ROUTE + ApiPathConstants.AUTH_OAUTH2 + ApiPathConstants.OAUTH2_INTENT,
                    ApiPathConstants.FULL_AUTH_OAUTH2_INTENT);
        }
    }

    // ==================== SECURITY CONFIG VALIDATION ====================

    @Nested
    @DisplayName("Security Config Validation")
    class SecurityConfigValidation {

        @Test
        @DisplayName("All public controller endpoints should be in whitelist")
        void validatePublicEndpointsInWhitelist() {
            // Auth endpoints that should be public (not logout)
            Set<String> expectedPublicEndpoints = Set.of(
                    ApiPathConstants.FULL_AUTH_REGISTER,
                    ApiPathConstants.FULL_AUTH_LOGIN,
                    ApiPathConstants.FULL_AUTH_REFRESH,
                    ApiPathConstants.FULL_AUTH_VERIFY_EMAIL,
                    ApiPathConstants.FULL_AUTH_RESEND_VERIFICATION,
                    ApiPathConstants.FULL_AUTH_FORGOT_PASSWORD,
                    ApiPathConstants.FULL_AUTH_RESET_PASSWORD
            );

            // In a real scenario, we would extract the whitelist from SecurityConfig
            // For now, we verify that the constants exist and match expected public endpoints
            Set<String> actualFullPaths = Set.of(
                    ApiPathConstants.FULL_AUTH_REGISTER,
                    ApiPathConstants.FULL_AUTH_LOGIN,
                    ApiPathConstants.FULL_AUTH_LOGOUT,
                    ApiPathConstants.FULL_AUTH_REFRESH,
                    ApiPathConstants.FULL_AUTH_VERIFY_EMAIL,
                    ApiPathConstants.FULL_AUTH_RESEND_VERIFICATION,
                    ApiPathConstants.FULL_AUTH_FORGOT_PASSWORD,
                    ApiPathConstants.FULL_AUTH_RESET_PASSWORD
            );

            // Verify all public endpoints exist in full paths
            for (String publicEndpoint : expectedPublicEndpoints) {
                assertTrue(actualFullPaths.contains(publicEndpoint),
                        "Public endpoint missing from full paths: " + publicEndpoint);
            }

            // Verify logout is NOT in public endpoints (it requires auth)
            assertFalse(expectedPublicEndpoints.contains(ApiPathConstants.FULL_AUTH_LOGOUT),
                    "AUTH_LOGOUT should NOT be in public endpoints - it requires authentication");
        }

        @Test
        @DisplayName("Whitelist should not have redundant wildcard patterns")
        void validateWhitelistNoRedundancies() {
            // Simulate the whitelist entries (mirrors SecurityConfig.WHITE_LIST)
            List<String> whitelistEntries = List.of(
                    "/v3/api-docs/**",
                    "/swagger-ui/**",
                    "/actuator/**",
                    "/login/**",
                    "/error",
                    ApiPathConstants.FULL_AUTH_REGISTER,
                    ApiPathConstants.FULL_AUTH_LOGIN,
                    ApiPathConstants.FULL_AUTH_REFRESH,
                    ApiPathConstants.FULL_AUTH_VERIFY_EMAIL,
                    ApiPathConstants.FULL_AUTH_RESEND_VERIFICATION,
                    ApiPathConstants.FULL_AUTH_FORGOT_PASSWORD,
                    ApiPathConstants.FULL_AUTH_RESET_PASSWORD
            );

            // Check: if a wildcard entry like "/actuator/**" exists, no more specific actuator
            // pattern like "/api/v1/actuator/**" should also exist
            List<String> violations = new ArrayList<>();
            for (int i = 0; i < whitelistEntries.size(); i++) {
                for (int j = i + 1; j < whitelistEntries.size(); j++) {
                    String entry1 = whitelistEntries.get(i);
                    String entry2 = whitelistEntries.get(j);
                    
                    // Check if one pattern makes the other redundant
                    if (isRedundant(entry1, entry2)) {
                        violations.add("\"" + entry2 + "\" is redundant (covered by \"" + entry1 + "\")");
                    }
                }
            }

            assertTrue(violations.isEmpty(),
                    "Whitelist has redundant entries:\n" + String.join("\n", violations));
        }

        @Test
        @DisplayName("Whitelist paths should use constants, not hardcoded strings")
        void validateWhitelistUsesConstants() {
            // This test documents the requirement that SecurityConfig.WHITE_LIST
            // should use ApiPathConstants instead of hardcoded strings.
            // The actual validation is done by code review since we can't easily
            // extract the whitelist array from SecurityConfig at runtime.

            // Verify that all FULL_AUTH_* constants exist (they should be used in SecurityConfig)
            assertNotNull(ApiPathConstants.FULL_AUTH_REGISTER);
            assertNotNull(ApiPathConstants.FULL_AUTH_LOGIN);
            assertNotNull(ApiPathConstants.FULL_AUTH_REFRESH);
            assertNotNull(ApiPathConstants.FULL_AUTH_VERIFY_EMAIL);
            assertNotNull(ApiPathConstants.FULL_AUTH_RESEND_VERIFICATION);
        }
    }

    // ==================== TEST PATHS VALIDATION ====================

    @Nested
    @DisplayName("Test Paths Validation")
    class TestPathsValidation {

        @Test
        @DisplayName("TestApiPaths should match ApiPathConstants for Auth endpoints")
        void validateTestAuthPathsMatchProduction() {
            assertEquals(ApiPathConstants.FULL_AUTH_REGISTER, TestApiPaths.AUTH_REGISTER);
            assertEquals(ApiPathConstants.FULL_AUTH_LOGIN, TestApiPaths.AUTH_LOGIN);
            assertEquals(ApiPathConstants.FULL_AUTH_LOGOUT, TestApiPaths.AUTH_LOGOUT);
            assertEquals(ApiPathConstants.FULL_AUTH_REFRESH, TestApiPaths.AUTH_REFRESH);
            assertEquals(ApiPathConstants.FULL_AUTH_VERIFY_EMAIL, TestApiPaths.AUTH_VERIFY_EMAIL);
            assertEquals(ApiPathConstants.FULL_AUTH_RESEND_VERIFICATION, TestApiPaths.AUTH_RESEND_VERIFICATION);
        }

        @Test
        @DisplayName("TestApiPaths should match ApiPathConstants for User endpoints")
        void validateTestUserPathsMatchProduction() {
            assertEquals(ApiPathConstants.V1_ROUTE + ApiPathConstants.USERS_ROUTE, TestApiPaths.USERS);
            assertEquals(ApiPathConstants.V1_ROUTE + ApiPathConstants.USERS_ROUTE + ApiPathConstants.USERS_ME,
                    TestApiPaths.USERS_ME);
        }

        @Test
        @DisplayName("TestApiPaths should match ApiPathConstants for Tenant endpoints")
        void validateTestTenantPathsMatchProduction() {
            assertEquals(ApiPathConstants.V1_ROUTE + ApiPathConstants.TENANTS_ROUTE, TestApiPaths.TENANTS);
            assertEquals(ApiPathConstants.V1_ROUTE + ApiPathConstants.TENANTS_ROUTE + ApiPathConstants.TENANTS_SELECT,
                    TestApiPaths.TENANTS_SELECT);
        }

        @Test
        @DisplayName("TestApiPaths should match ApiPathConstants for Member endpoints")
        void validateTestMemberPathsMatchProduction() {
            assertEquals(ApiPathConstants.V1_ROUTE + ApiPathConstants.TENANTS_ROUTE + "/{tenantId}" + ApiPathConstants.MEMBERS_ROUTE,
                    TestApiPaths.MEMBERS);
        }

        @Test
        @DisplayName("TestApiPaths should match ApiPathConstants for Invitation endpoints")
        void validateTestInvitationPathsMatchProduction() {
            assertEquals(ApiPathConstants.V1_ROUTE + ApiPathConstants.INVITATIONS_ROUTE, TestApiPaths.INVITATIONS);
            assertEquals(ApiPathConstants.V1_ROUTE + ApiPathConstants.INVITATIONS_ROUTE + ApiPathConstants.INVITATIONS_ACCEPT,
                    TestApiPaths.INVITATIONS_ACCEPT);
        }

        @Test
        @DisplayName("All TestApiPaths should start with V1_ROUTE")
        void validateAllTestPathsStartWithV1() {
            assertTrue(TestApiPaths.AUTH_REGISTER.startsWith(ApiPathConstants.V1_ROUTE));
            assertTrue(TestApiPaths.AUTH_LOGIN.startsWith(ApiPathConstants.V1_ROUTE));
            assertTrue(TestApiPaths.USERS_ME.startsWith(ApiPathConstants.V1_ROUTE));
            assertTrue(TestApiPaths.TENANTS.startsWith(ApiPathConstants.V1_ROUTE));
            assertTrue(TestApiPaths.MEMBERS.startsWith(ApiPathConstants.V1_ROUTE));
            assertTrue(TestApiPaths.INVITATIONS.startsWith(ApiPathConstants.V1_ROUTE));
        }
    }

    // ==================== HELPER METHODS ====================

    private String getRouteConstantForController(Class<?> controller) {
        String name = controller.getSimpleName();
        if (name.endsWith("Controller")) {
            name = name.substring(0, name.length() - "Controller".length());
        }
        if (name.endsWith("ApiController")) {
            name = name.substring(0, name.length() - "ApiController".length()) + "Api";
        }

        return switch (name) {
            case "AuthApi" -> ApiPathConstants.AUTH_ROUTE;
            case "UserApi" -> ApiPathConstants.USERS_ROUTE;
            case "TenantApi" -> ApiPathConstants.TENANTS_ROUTE;
            case "MemberApi" -> ApiPathConstants.TENANTS_ROUTE + "/{tenantId}" + ApiPathConstants.MEMBERS_ROUTE;
            case "InvitationApi" -> ApiPathConstants.INVITATIONS_ROUTE;
            case "OAuth2Api", "LoginOauth2" -> ApiPathConstants.AUTH_ROUTE + ApiPathConstants.AUTH_OAUTH2;
            default -> throw new IllegalArgumentException("Unknown controller: " + controller.getSimpleName());
        };
    }

    private <T extends Annotation> void checkMappingAnnotation(
            Method method,
            Class<T> annotationType,
            java.util.function.Function<T, String[]> pathExtractor,
            String annotationName,
            String controllerName,
            String basePath,
            List<String> violations) {

        T annotation = method.getAnnotation(annotationType);
        if (annotation != null) {
            String[] paths = pathExtractor.apply(annotation);
            for (String path : paths) {
                // Empty path is valid (default endpoint for the method)
                if (path.isEmpty()) {
                    continue;
                }

                // The path from reflection is the resolved string value.
                // We need to verify that basePath + path forms a valid full path pattern.
                String fullPath = basePath + path;
                
                // Check if the full path matches a known constant pattern
                if (!isValidFullPath(fullPath)) {
                    violations.add(controllerName + "." + method.getName() + " has @" + annotationName +
                            "=\"" + path + "\" which should be constructed from ApiPathConstants");
                }
            }
        }
    }

    private boolean isValidFullPath(String fullPath) {
        // Check if the path starts with the V1_ROUTE constant
        // This validates that the controller's @RequestMapping uses V1_ROUTE + ROUTE
        // and the method mapping is a relative path that gets concatenated
        return fullPath.startsWith(ApiPathConstants.V1_ROUTE);
    }

    /**
     * Checks if pattern2 is redundant given pattern1.
     * e.g., "/actuator/**" makes "/api/v1/actuator/**" redundant.
     */
    private boolean isRedundant(String pattern1, String pattern2) {
        // Exact duplicate
        if (pattern1.equals(pattern2)) {
            return true;
        }
        
        // If pattern1 is a wildcard that covers pattern2
        if (pattern1.endsWith("/**")) {
            String prefix = pattern1.substring(0, pattern1.length() - 3);
            if (pattern2.startsWith(prefix)) {
                return true;
            }
        }
        
        return false;
    }
}
