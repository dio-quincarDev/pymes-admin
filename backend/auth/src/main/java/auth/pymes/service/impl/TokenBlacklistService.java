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
 * Cada tenant tiene su propio espacio de nombres aislado.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TokenBlacklistService {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String BLACKLIST_PREFIX = "auth:token_blacklist:";

    /**
     * Añade un token a la blacklist (logout).
     * @param token El token JWT a revocar
     * @param expirationMs Tiempo de expiración del token en milisegundos
     * @param tenantId Identificador del tenant (para aislamiento multi-tenant)
     */
    public void revokeToken(String token, long expirationMs, String tenantId) {
        String key = "tenant:" + tenantId + ":auth:token_blacklist:" + token;
        redisTemplate.opsForValue().set(key, "revoked", expirationMs, TimeUnit.MILLISECONDS);
        log.debug("Token revocado añadido a blacklist: {}", key);
    }

    /**
     * Verifica si un token está en la blacklist.
     * @param token El token JWT a verificar
     * @param tenantId Identificador del tenant
     * @return true si el token está revocado, false si no
     */
    public boolean isTokenRevoked(String token, String tenantId) {
        String key = "tenant:" + tenantId + ":auth:token_blacklist:" + token;
        Boolean exists = redisTemplate.hasKey(key);
        return exists != null && exists;
    }

    /**
     * Elimina un token de la blacklist (útil para re-login antes de expiración).
     * @param token El token JWT a remover
     * @param tenantId Identificador del tenant
     */
    public void removeFromBlacklist(String token, String tenantId) {
        String key = "tenant:" + tenantId + ":auth:token_blacklist:" + token;
        redisTemplate.delete(key);
        log.debug("Token eliminado de blacklist: {}", key);
    }

    /**
     * Limpia tokens expirados de la blacklist.
     * Redis ya maneja la expiración automáticamente con TTL.
     * @param tenantId Identificador del tenant
     */
    public void cleanupExpiredTokens(String tenantId) {
        // Redis maneja la expiración automáticamente con TTL
        log.debug("Limpieza de tokens expirados para tenant {}: Redis maneja TTL automáticamente", tenantId);
    }
}