package auth.pymes.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Servicio para cachear permisos de usuario por tenant en Redis.
 * Key pattern: auth:permissions:{user_id}:{tenant_id}
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PermissionCacheService {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String PERMISSIONS_PREFIX = "auth:permissions:";
    private static final long CACHE_TTL_MINUTES = 5;

    /**
     * Guarda los permisos de un usuario para un tenant en caché.
     * @param userId ID del usuario
     * @param tenantId ID del tenant
     * @param permissions Lista de permisos
     */
    public void cachePermissions(String userId, String tenantId, List<String> permissions) {
        String key = buildKey(userId, tenantId);
        redisTemplate.opsForValue().set(key, permissions, CACHE_TTL_MINUTES, TimeUnit.MINUTES);
        log.debug("Permisos cacheados para usuario {} tenant {}: {}", userId, tenantId, permissions);
    }

    /**
     * Obtiene los permisos cacheados de un usuario para un tenant.
     * @param userId ID del usuario
     * @param tenantId ID del tenant
     * @return Lista de permisos o null si no está en caché
     */
    @SuppressWarnings("unchecked")
    public List<String> getCachedPermissions(String userId, String tenantId) {
        String key = buildKey(userId, tenantId);
        Object value = redisTemplate.opsForValue().get(key);
        
        if (value instanceof List<?>) {
            return ((List<?>) value).stream()
                    .map(Object::toString)
                    .collect(Collectors.toList());
        }
        
        return null;
    }

    /**
     * Invalida los permisos cacheados de un usuario para un tenant.
     * @param userId ID del usuario
     * @param tenantId ID del tenant
     */
    public void invalidatePermissions(String userId, String tenantId) {
        String key = buildKey(userId, tenantId);
        redisTemplate.delete(key);
        log.debug("Permisos invalidados para usuario {} tenant {}", userId, tenantId);
    }

    /**
     * Invalida todos los permisos cacheados de un usuario (todos los tenants).
     * @param userId ID del usuario
     */
    public void invalidateAllUserPermissions(String userId) {
        String pattern = PERMISSIONS_PREFIX + userId + ":*";
        var keys = redisTemplate.keys(pattern);
        
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
            log.debug("Permisos invalidados para usuario {} ({} claves eliminadas)", userId, keys.size());
        }
    }

    private String buildKey(String userId, String tenantId) {
        return PERMISSIONS_PREFIX + userId + ":" + tenantId;
    }
}
