package dev.dioquincar.gateway_pymes.filter;

import dev.dioquincar.gateway_pymes.util.JwtUtils;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
@Slf4j
public class AuthenticationFilter extends AbstractGatewayFilterFactory<AuthenticationFilter.Config> {

    private final JwtUtils jwtUtils;
    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private final RouterValidator routerValidator;

    private static final String BLACKLIST_PREFIX = "auth:token_blacklist:";

    public AuthenticationFilter(JwtUtils jwtUtils, ReactiveRedisTemplate<String, String> redisTemplate, RouterValidator routerValidator) {
        super(Config.class);
        this.jwtUtils = jwtUtils;
        this.redisTemplate = redisTemplate;
        this.routerValidator = routerValidator;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();

            // 0. Si la ruta es pública (whitelist), no aplicar seguridad
            if (!routerValidator.isSecured.test(request)) {
                return chain.filter(exchange);
            }

            // 1. Obtener el token del header Authorization
            if (!request.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
                return onError(exchange, "No authorization header", HttpStatus.UNAUTHORIZED);
            }

            String authHeader = request.getHeaders().get(HttpHeaders.AUTHORIZATION).get(0);
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return onError(exchange, "Invalid authorization header", HttpStatus.UNAUTHORIZED);
            }

            String token = authHeader.substring(7);

            // 2. Validar firma y expiración del JWT (una sola vez)
            Claims claims;
            try {
                claims = jwtUtils.getClaims(token);
            } catch (Exception e) {
                return onError(exchange, "Invalid JWT token", HttpStatus.UNAUTHORIZED);
            }

            // 3. Verificar en Redis si está en la blacklist (Logout)
            return redisTemplate.hasKey(BLACKLIST_PREFIX + token)
                    .flatMap(isRevoked -> {
                        if (isRevoked) {
                            log.warn("Intento de acceso con token revocado: {}", token);
                            return onError(exchange, "Token is revoked", HttpStatus.UNAUTHORIZED);
                        }

                        // 4. Inyectar claims en los headers para los microservicios internos
                        ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                                .header("X-User-Id", claims.get("userId") != null ? String.valueOf(claims.get("userId")) : null)
                                .header("X-User-Email", claims.getSubject())
                                .header("X-Tenant-Id", claims.get("tenantId") != null ? String.valueOf(claims.get("tenantId")) : null)
                                .header("X-User-Role", claims.get("role") != null ? String.valueOf(claims.get("role")) : null)
                                .build();

                        return chain.filter(exchange.mutate().request(mutatedRequest).build());
                    });
        };
    }

    private Mono<Void> onError(ServerWebExchange exchange, String err, HttpStatus httpStatus) {
        exchange.getResponse().setStatusCode(httpStatus);
        return exchange.getResponse().setComplete();
    }

    public static class Config {
    }
}
