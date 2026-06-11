package auth.pymes.common.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

/**
 * Rate limiting con Redis usando script Lua atómico.
 * Garantiza que INCR y EXPIRE sean una sola operación atómica.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RateLimitService {

    private final StringRedisTemplate redisTemplate;

    private static final String RATE_LIMIT_PREFIX = "rate_limit:";
    private static final int MAX_ATTEMPTS = 5;
    private static final Duration WINDOW = Duration.ofMinutes(15);

    private static final DefaultRedisScript<Long> INCR_EXPIRE_SCRIPT = new DefaultRedisScript<>();

    static {
        INCR_EXPIRE_SCRIPT.setScriptText(
            "local count = redis.call('INCR', KEYS[1])\n" +
            "if count == 1 then\n" +
            "    redis.call('EXPIRE', KEYS[1], ARGV[1])\n" +
            "end\n" +
            "return count"
        );
        INCR_EXPIRE_SCRIPT.setResultType(Long.class);
    }

    /**
     * @return true si está permitido, false si excedió el límite
     */
    public boolean isAllowed(String key) {
        String redisKey = RATE_LIMIT_PREFIX + key;
        Long count = redisTemplate.execute(
            INCR_EXPIRE_SCRIPT,
            List.of(redisKey),
            String.valueOf(WINDOW.toSeconds())
        );

        if (count == null) {
            log.warn("Rate limit: respuesta nula de Redis para key={}", key);
            return true;
        }

        boolean allowed = count <= MAX_ATTEMPTS;

        if (!allowed) {
            log.warn("Rate limit excedido para key={}, count={}", key, count);
        }

        return allowed;
    }

    public long getRemainingAttempts(String key) {
        String redisKey = RATE_LIMIT_PREFIX + key;
        String value = redisTemplate.opsForValue().get(redisKey);
        long attempts = value != null ? Long.parseLong(value) : 0;
        return Math.max(0, MAX_ATTEMPTS - attempts);
    }
}
