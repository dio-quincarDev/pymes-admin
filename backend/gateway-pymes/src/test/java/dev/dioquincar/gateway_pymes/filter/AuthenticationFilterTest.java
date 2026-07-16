package dev.dioquincar.gateway_pymes.filter;

import dev.dioquincar.gateway_pymes.util.JwtUtils;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthenticationFilterTest {

    @Mock JwtUtils jwtUtils;
    @Mock ReactiveRedisTemplate<String, String> redisTemplate;
    @Mock ServerWebExchange exchange;
    @Mock ServerHttpRequest request;
    @Mock ServerHttpResponse response;
    @Mock GatewayFilterChain chain;
    @Mock Claims claims;
    @Mock DataBufferFactory bufferFactory;
    @Mock DataBuffer dataBuffer;

    private final HttpHeaders httpHeaders = new HttpHeaders();
    private final RouterValidator routerValidator = new RouterValidator();
    private AuthenticationFilter filter;
    private GatewayFilter gatewayFilter;

    @BeforeEach
    void setUp() {
        filter = new AuthenticationFilter(jwtUtils, redisTemplate, routerValidator);
        gatewayFilter = filter.apply(new AuthenticationFilter.Config());
        lenient().when(exchange.getRequest()).thenReturn(request);
        lenient().when(exchange.getResponse()).thenReturn(response);
        lenient().when(chain.filter(any())).thenReturn(Mono.empty());
        lenient().when(response.getHeaders()).thenReturn(httpHeaders);
        lenient().when(response.bufferFactory()).thenReturn(bufferFactory);
        lenient().when(bufferFactory.wrap(any(byte[].class))).thenReturn(dataBuffer);
        lenient().when(response.writeWith(any())).thenReturn(Mono.empty());
    }

    @Test
    void whitelistedPathSkipsAuth() {
        routerValidator.isSecured = r -> false;

        gatewayFilter.filter(exchange, chain).block();

        verify(chain).filter(exchange);
        verifyNoInteractions(jwtUtils, redisTemplate);
    }

    @Test
    void missingAuthHeaderReturns401() {
        secured();
        when(request.getHeaders()).thenReturn(new HttpHeaders());

        gatewayFilter.filter(exchange, chain).block();

        verify(response).setStatusCode(HttpStatus.UNAUTHORIZED);
        verify(response).writeWith(any());
    }

    @Test
    void invalidBearerTokenReturns401() {
        secured();
        when(request.getHeaders()).thenReturn(headers("Bearer "));
        when(jwtUtils.getClaims("")).thenThrow(new RuntimeException("empty token"));

        gatewayFilter.filter(exchange, chain).block();

        verify(response).setStatusCode(HttpStatus.UNAUTHORIZED);
        verify(response).writeWith(any());
    }

    @Test
    void expiredTokenReturns401() {
        secured();
        when(request.getHeaders()).thenReturn(headers("Bearer invalid.jwt.token"));
        when(jwtUtils.getClaims("invalid.jwt.token")).thenThrow(new RuntimeException("expired"));

        gatewayFilter.filter(exchange, chain).block();

        verify(response).setStatusCode(HttpStatus.UNAUTHORIZED);
        verify(response).writeWith(any());
    }

    @Test
    void revokedTokenReturns401() {
        secured();
        when(request.getHeaders()).thenReturn(headers("Bearer t"));
        when(jwtUtils.getClaims("t")).thenReturn(claims);
        when(claims.get("role")).thenReturn("ADMIN");
        when(redisTemplate.hasKey("auth:token_blacklist:t")).thenReturn(Mono.just(true));

        gatewayFilter.filter(exchange, chain).block();

        verify(response).setStatusCode(HttpStatus.UNAUTHORIZED);
        verify(response).writeWith(any());
    }

    @Test
    void refreshTokenRejectedBeforeRedisCheck() {
        secured();
        when(request.getHeaders()).thenReturn(headers("Bearer t"));
        when(jwtUtils.getClaims("t")).thenReturn(claims);
        // claims.get("role") returns null by default — refresh token

        gatewayFilter.filter(exchange, chain).block();

        verify(response).setStatusCode(HttpStatus.UNAUTHORIZED);
        verify(response).writeWith(any());
        verifyNoInteractions(redisTemplate);
    }

    @Test
    void validTokenWithPartialClaimsOnlyInjectsPresentHeaders() {
        secured();
        when(request.getHeaders()).thenReturn(headers("Bearer t"));
        when(jwtUtils.getClaims("t")).thenReturn(claims);
        when(redisTemplate.hasKey("auth:token_blacklist:t")).thenReturn(Mono.just(false));
        when(claims.get("role")).thenReturn("OWNER");
        when(claims.get("role", String.class)).thenReturn("OWNER");
        when(claims.get("userId", String.class)).thenReturn("u1");
        // subject, tenantId, plan explicitly return null
        when(claims.getSubject()).thenReturn(null);
        when(claims.get("tenantId", String.class)).thenReturn(null);
        when(claims.get("plan", String.class)).thenReturn(null);

        var requestBuilder = mock(ServerHttpRequest.Builder.class, RETURNS_SELF);
        when(request.mutate()).thenReturn(requestBuilder);
        var mutatedRequest = mock(ServerHttpRequest.class);
        when(requestBuilder.build()).thenReturn(mutatedRequest);

        var exchangeBuilder = mock(ServerWebExchange.Builder.class, RETURNS_SELF);
        when(exchange.mutate()).thenReturn(exchangeBuilder);
        var mutatedExchange = mock(ServerWebExchange.class);
        when(exchangeBuilder.build()).thenReturn(mutatedExchange);

        gatewayFilter.filter(exchange, chain).block();

        verify(chain).filter(mutatedExchange);
        verify(requestBuilder).header("X-User-Id", "u1");
        verify(requestBuilder).header("X-User-Role", "OWNER");
        verify(requestBuilder, never()).header(eq("X-User-Email"), any());
        verify(requestBuilder, never()).header(eq("X-Tenant-Id"), any());
        verify(requestBuilder, never()).header(eq("X-User-Plan"), any());
    }

    @Test
    void validTokenInjectsAllClaimHeaders() {
        secured();
        when(request.getHeaders()).thenReturn(headers("Bearer t"));
        when(jwtUtils.getClaims("t")).thenReturn(claims);
        when(redisTemplate.hasKey("auth:token_blacklist:t")).thenReturn(Mono.just(false));
        when(claims.get("role")).thenReturn("ADMIN");
        when(claims.get("role", String.class)).thenReturn("ADMIN");
        when(claims.get("userId", String.class)).thenReturn("u1");
        when(claims.getSubject()).thenReturn("admin@test.com");
        when(claims.get("tenantId", String.class)).thenReturn("t1");
        when(claims.get("plan", String.class)).thenReturn("PRO");

        var requestBuilder = mock(ServerHttpRequest.Builder.class, RETURNS_SELF);
        when(request.mutate()).thenReturn(requestBuilder);
        var mutatedRequest = mock(ServerHttpRequest.class);
        when(requestBuilder.build()).thenReturn(mutatedRequest);

        var exchangeBuilder = mock(ServerWebExchange.Builder.class, RETURNS_SELF);
        when(exchange.mutate()).thenReturn(exchangeBuilder);
        var mutatedExchange = mock(ServerWebExchange.class);
        when(exchangeBuilder.build()).thenReturn(mutatedExchange);

        gatewayFilter.filter(exchange, chain).block();

        verify(chain).filter(mutatedExchange);
        verify(requestBuilder).header("X-User-Id", "u1");
        verify(requestBuilder).header("X-User-Email", "admin@test.com");
        verify(requestBuilder).header("X-Tenant-Id", "t1");
        verify(requestBuilder).header("X-User-Role", "ADMIN");
        verify(requestBuilder).header("X-User-Plan", "PRO");
    }

    @Test
    void errorResponseIsJson() {
        secured();
        when(request.getHeaders()).thenReturn(new HttpHeaders());

        gatewayFilter.filter(exchange, chain).block();

        assertEquals(MediaType.APPLICATION_JSON, httpHeaders.getContentType());
    }

    private void secured() {
        routerValidator.isSecured = r -> true;
    }

    private static HttpHeaders headers(String authValue) {
        HttpHeaders h = new HttpHeaders();
        h.set(HttpHeaders.AUTHORIZATION, authValue);
        return h;
    }
}
