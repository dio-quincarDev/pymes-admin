package auth.pymes.common.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Objects;

/**
 * Rate limiting simple con Redis usando sliding window counter.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RateLimitService {

    private final StringRedisTemplate redisTemplate;

    private static final String RATE_LIMIT_PREFIX = "rate_limit:";
    private static final int MAX_ATTEMPTS = 5;
    private static final Duration WINDOW = Duration.ofMinutes(15);

    /**
     * @return true si está permitido, false si excedió el límite
     */
    public boolean isAllowed(String key) {
        String redisKey = RATE_LIMIT_PREFIX + key;
        Long count = redisTemplate.opsForValue().increment(redisKey);

        if (count != null && count == 1) {
            redisTemplate.expire(redisKey, WINDOW);
        }

        boolean allowed = count != null && count <= MAX_ATTEMPTS;

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
