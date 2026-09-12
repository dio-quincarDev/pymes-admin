package core_pymes.common.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.time.Duration;

/**
 * ponytail: 1 filtro, Redis SET NX 6h, replay respuesta — sin tabla nueva, sin lib nueva.
 * Reuse StringRedisTemplate ya inyectado (mismo que RecomputeDebounceService).
 * Cel: replay 6h, si necesitas 30d audit migrar a tabla PG idempotency_keys.
 */
@Component
@Order(0)
@ConditionalOnBean(StringRedisTemplate.class)
@RequiredArgsConstructor
@Slf4j
public class IdempotencyFilter extends OncePerRequestFilter {

    private final StringRedisTemplate stringRedisTemplate;
    private static final Duration TTL = Duration.ofHours(6);
    private static final String HEADER = "Idempotency-Key";
    private static final String PREFIX = "idempotency:";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        // ponytail: solo POST creaciones/pagos — PUT/DELETE ya idempotentes por PK, GET no cachea
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            chain.doFilter(request, response);
            return;
        }

        String key = request.getHeader(HEADER);
        if (key == null || key.isBlank()) {
            chain.doFilter(request, response);
            return;
        }

        // scope por tenant para no colisionar entre pymes
        String tenant = request.getHeader("X-Tenant-Id");
        if (tenant == null || tenant.isBlank()) {
            tenant = request.getParameter("tenantId");
        }
        // si no hay tenant aún, usa key global (onboarding)
        String redisKey = PREFIX + (tenant != null ? tenant + ":" : "") + key;

        String cached = stringRedisTemplate.opsForValue().get(redisKey);
        if (cached != null) {
            // formato: status|contentType|body
            String[] parts = cached.split("\\|", 3);
            int status = Integer.parseInt(parts[0]);
            String contentType = parts.length > 1 ? parts[1] : "application/json";
            String body = parts.length > 2 ? parts[2] : "";
            log.debug("Idempotency replay key={} tenant={}", key, tenant);
            response.setStatus(status);
            response.setContentType(contentType);
            response.getWriter().write(body);
            return;
        }

        ContentCachingResponseWrapper wrapped = new ContentCachingResponseWrapper(response);
        chain.doFilter(request, wrapped);

        int status = wrapped.getStatus();
        // solo cachea 2xx — errores no se replayean
        if (status >= 200 && status < 300) {
            String body = new String(wrapped.getContentAsByteArray(), wrapped.getCharacterEncoding());
            String contentType = wrapped.getContentType() != null ? wrapped.getContentType() : "application/json";
            String toStore = status + "|" + contentType + "|" + body;
            // SET NX 6h — si 2 requests concurrentes, el primero gana
            Boolean set = stringRedisTemplate.opsForValue().setIfAbsent(redisKey, toStore, TTL);
            if (Boolean.TRUE.equals(set)) {
                log.debug("Idempotency stored key={} tenant={} ttl=6h", key, tenant);
            }
        }
        wrapped.copyBodyToResponse();
    }
}
