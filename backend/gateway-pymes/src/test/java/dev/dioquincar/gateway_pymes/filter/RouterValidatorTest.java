package dev.dioquincar.gateway_pymes.filter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.server.reactive.ServerHttpRequest;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RouterValidatorTest {

    private final RouterValidator validator = new RouterValidator();

    @ParameterizedTest
    @ValueSource(strings = {
            "/api/v1/auth/login",
            "/api/v1/auth/register",
            "/api/v1/auth/refresh",
            "/api/v1/auth/verify-email",
            "/api/v1/auth/resend-verification",
            "/api/v1/auth/forgot-password",
            "/api/v1/auth/reset-password",
            "/api/v1/auth/exchange",
            "/api/v1/auth/oauth2/callback",
            "/login/oauth2/code/google",
            "/oauth2/authorization/google",
            "/v3/api-docs/auth",
            "/swagger-ui/index.html",
            "/actuator/health",
            "/error"
    })
    void openEndpointsAreNotSecured(String path) {
        assertFalse(isSecured(path), path + " should be open");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/api/v1/companies",
            "/api/v1/products/123",
            "/api/v1/users/me",
            "/api/v1/auth/logout",
            "/api/v1/auth/change-password",
            "/some/other/path"
    })
    void securedEndpointsAreSecured(String path) {
        assertTrue(isSecured(path), path + " should be secured");
    }

    @Test
    void nestedOpenPathIsNotSecured() {
        assertFalse(isSecured("/api/v1/auth/verify-email?token=abc"));
    }

    private boolean isSecured(String path) {
        ServerHttpRequest request = mock(ServerHttpRequest.class);
        when(request.getURI()).thenReturn(URI.create(path));
        return validator.isSecured.test(request);
    }
}
