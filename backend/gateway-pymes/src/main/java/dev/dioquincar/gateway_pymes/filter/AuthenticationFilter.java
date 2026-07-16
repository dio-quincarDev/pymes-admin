package dev.dioquincar.gateway_pymes.filter;

import dev.dioquincar.gateway_pymes.util.JwtUtils;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

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

            if (!routerValidator.isSecured.test(request)) {
                return chain.filter(exchange);
            }

            if (!request.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
                return onError(exchange, "No authorization header", HttpStatus.UNAUTHORIZED);
            }

            String authHeader = request.getHeaders().get(HttpHeaders.AUTHORIZATION).get(0);
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return onError(exchange, "Invalid authorization header", HttpStatus.UNAUTHORIZED);
            }

            String token = authHeader.substring(7);

            Claims claims;
            try {
                claims = jwtUtils.getClaims(token);
            } catch (Exception e) {
                return onError(exchange, "Invalid JWT token", HttpStatus.UNAUTHORIZED);
            }

            // Reject refresh tokens — they lack role/tenantId/plan claims
            if (claims.get("role") == null) {
                return onError(exchange, "Access tokens only", HttpStatus.UNAUTHORIZED);
            }

            return redisTemplate.hasKey(BLACKLIST_PREFIX + token)
                    .flatMap(isRevoked -> {
                        if (isRevoked) {
                            log.warn("Intento de acceso con token revocado");
                            return onError(exchange, "Token is revoked", HttpStatus.UNAUTHORIZED);
                        }

                        ServerHttpRequest.Builder builder = exchange.getRequest().mutate();
                        addIfNotNull(builder, "X-User-Id", claims.get("userId", String.class));
                        addIfNotNull(builder, "X-User-Email", claims.getSubject());
                        addIfNotNull(builder, "X-Tenant-Id", claims.get("tenantId", String.class));
                        addIfNotNull(builder, "X-User-Role", claims.get("role", String.class));
                        addIfNotNull(builder, "X-User-Plan", claims.get("plan", String.class));

                        return chain.filter(exchange.mutate().request(builder.build()).build());
                    });
        };
    }

    private static void addIfNotNull(ServerHttpRequest.Builder builder, String name, String value) {
        if (value != null) {
            builder.header(name, value);
        }
    }

    private Mono<Void> onError(ServerWebExchange exchange, String err, HttpStatus httpStatus) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(httpStatus);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String body = "{\"error\":\"" + err + "\",\"status\":" + httpStatus.value() + "}";
        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }

    public static class Config {
    }
}
