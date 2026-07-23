package dev.dioquincar.gateway_pymes.filter;

import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

import java.util.List;
import java.util.function.Predicate;

@Component
public class RouterValidator {

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public static final List<String> openEndPoints = List.of(
            "/api/v1/auth/login",
            "/api/v1/auth/register",
            "/api/v1/auth/refresh",
            "/api/v1/auth/verify-email",
            "/api/v1/auth/resend-verification",
            "/api/v1/auth/forgot-password",
            "/api/v1/auth/reset-password",
            "/api/v1/auth/exchange",
            "/api/v1/auth/oauth2/**",
            "/api/v1/invitations/accept",
            "/api/v1/invitations/*/info",
            "/api/v1/invitations/*/register",
            "/login/**",
            "/oauth2/**",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/actuator/**",
            "/error"
    );

    public Predicate<ServerHttpRequest> isSecured = request ->
            openEndPoints.stream()
                    .noneMatch(uri -> pathMatcher.match(uri, request.getURI().getPath()));

}
