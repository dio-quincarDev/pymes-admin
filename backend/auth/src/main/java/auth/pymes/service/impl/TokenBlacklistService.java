package auth.pymes.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Servicio para gestionar la blacklist de tokens JWT en Redis.
 * Los tokens revocados (logout) se almacenan aquí hasta que expiran.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TokenBlacklistService {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String BLACKLIST_PREFIX = "auth:token_blacklist:";
    private static final String REVOKED_VALUE = "revoked";

    /**
     * Añade un token a la blacklist (logout).
     * @param token El token JWT a revocar
     * @param expirationSeconds Tiempo de expiración del token (para que Redis lo elimine automáticamente)
     */
    public void revokeToken(String token, long expirationSeconds) {
        String key = BLACKLIST_PREFIX + token;
        redisTemplate.opsForValue().set(key, REVOKED_VALUE, expirationSeconds, TimeUnit.SECONDS);
        log.debug("Token revocado añadido a blacklist: {}", key);
    }

    /**
     * Verifica si un token está en la blacklist.
     * @param token El token JWT a verificar
     * @return true si el token está revocado, false si no
     */
    public boolean isTokenRevoked(String token) {
        String key = BLACKLIST_PREFIX + token;
        Boolean exists = redisTemplate.hasKey(key);
        return exists != null && exists;
    }

    /**
     * Elimina un token de la blacklist (útil para re-login antes de expiración).
     * @param token El token JWT a remover
     */
    public void removeFromBlacklist(String token) {
        String key = BLACKLIST_PREFIX + token;
        redisTemplate.delete(key);
        log.debug("Token eliminado de blacklist: {}", key);
    }

    /**
     * Limpia tokens expirados de la blacklist (tarea de mantenimiento opcional).
     * Redis ya maneja la expiración automáticamente con TTL, pero este método
     * puede usarse para logging o métricas.
     */
    public void cleanupExpiredTokens() {
        // Redis maneja la expiración automáticamente con TTL
        log.debug("Limpieza de tokens expirados: Redis maneja TTL automáticamente");
    }
}
